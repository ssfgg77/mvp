package com.example.trading.service;

import com.example.trading.api.dto.PlaceOrderRequest;
import com.example.trading.api.dto.PlaceOrderResponse;
import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.IdempotencyKey;
import com.example.trading.persistence.entity.SchwabAccount;
import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.repo.IdempotencyKeyRepository;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.PlaceOrderRequestDto;
import com.example.trading.schwab.dto.PlaceOrderResultDto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private static final int MAX_ORDER_QUANTITY = 10000;

  private final CurrentUserService currentUser;
  private final UserSettingsService settings;
  private final SchwabAccountRepository accounts;
  private final TradeOrderRepository orders;
  private final IdempotencyKeyRepository idempotency;
  private final SchwabTokenService tokens;
  private final SchwabClient schwab;
  private final AuditService audit;

  public OrderService(
      CurrentUserService currentUser,
      UserSettingsService settings,
      SchwabAccountRepository accounts,
      TradeOrderRepository orders,
      IdempotencyKeyRepository idempotency,
      SchwabTokenService tokens,
      SchwabClient schwab,
      AuditService audit
  ) {
    this.currentUser = currentUser;
    this.settings = settings;
    this.accounts = accounts;
    this.orders = orders;
    this.idempotency = idempotency;
    this.tokens = tokens;
    this.schwab = schwab;
    this.audit = audit;
  }

  @Transactional
  public PlaceOrderResponse placeOrder(String idempotencyKey, PlaceOrderRequest req, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);

    NormalizedOrder normalized = validate(req);
    String fingerprint = fingerprint(req);
    String keyHash = sha256Hex(idempotencyKey);

    var existing = idempotency.findByUserAndKeyHash(user, keyHash);
    if (existing.isPresent()) {
      IdempotencyKey idem = existing.get();
      if (!idem.getRequestFingerprint().equals(fingerprint)) {
        throw new IllegalStateException("Idempotency-Key reuse with different request payload");
      }
      TradeOrder o = orders.findById(idem.getOrderId()).orElseThrow();
      PlaceOrderResponse resp = new PlaceOrderResponse();
      resp.orderId = o.getId();
      resp.status = o.getStatus().name();
      resp.schwabOrderId = o.getSchwabOrderId();
      return resp;
    }

    SchwabAccount account = resolveAccount(user, req.accountId);

    TradeOrder o = new TradeOrder();
    o.setUser(user);
    o.setSchwabAccount(account);
    o.setClientOrderId("cli-" + UUID.randomUUID());
    o.setSymbol(normalized.symbol());
    o.setQuantity(normalized.quantity());
    o.setSide(TradeOrder.Side.valueOf(normalized.side()));
    o.setOrderType(TradeOrder.OrderType.valueOf(normalized.orderType()));
    o.setLimitPrice(normalized.limitPrice());
    o.setStatus(TradeOrder.Status.NEW);
    orders.save(o);

    // persist idempotency mapping
    IdempotencyKey idem = new IdempotencyKey();
    idem.setUser(user);
    idem.setKeyHash(keyHash);
    idem.setRequestFingerprint(fingerprint);
    idem.setOrderId(o.getId());
    idempotency.save(idem);

    // Call Schwab
    String token = tokens.requireAccessToken(auth);
    PlaceOrderResultDto result = schwab.placeOrder(
        token,
        account.getSchwabAccountRef(),
        new PlaceOrderRequestDto(
            normalized.symbol(),
            normalized.side(),
            normalized.quantity(),
            normalized.orderType(),
            normalized.limitPrice()
        )
    );

    o.setSchwabOrderId(result.schwabOrderId());
    o.setStatus(parseStatus(result.status()));
    orders.save(o);

    audit.record(user, "ORDER_PLACE", "TradeOrder", o.getId().toString(),
        "Placed order; schwabOrderId=" + o.getSchwabOrderId());

    PlaceOrderResponse resp = new PlaceOrderResponse();
    resp.orderId = o.getId();
    resp.status = o.getStatus().name();
    resp.schwabOrderId = o.getSchwabOrderId();
    return resp;
  }

  @Transactional
  public void cancelOrder(UUID orderId, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    TradeOrder o = orders.findByIdAndUser(orderId, user).orElseThrow();

    if (o.getStatus() == TradeOrder.Status.FILLED
        || o.getStatus() == TradeOrder.Status.CANCELED
        || o.getStatus() == TradeOrder.Status.REJECTED) {
      return; // already terminal
    }

    String token = tokens.requireAccessToken(auth);
    schwab.cancelOrder(token, o.getSchwabAccount().getSchwabAccountRef(), o.getSchwabOrderId());
    o.setStatus(TradeOrder.Status.CANCEL_REQUESTED);
    orders.save(o);

    audit.record(user, "ORDER_CANCEL", "TradeOrder", o.getId().toString(),
        "Cancel requested; schwabOrderId=" + o.getSchwabOrderId());
  }

  private SchwabAccount resolveAccount(AppUser user, Long accountIdOverride) {
    if (accountIdOverride != null) {
      return accounts.findByIdAndUser(accountIdOverride, user).orElseThrow();
    }
    var s = settings.getOrCreate(user);
    var def = s.getDefaultSchwabAccount();
    if (def == null) throw new IllegalStateException("No default Schwab account selected");
    return def;
  }

  private static String fingerprint(PlaceOrderRequest req) {
    if (req == null) return "";
    // stable fingerprint: symbol|side|qty|type|limit|accountOverride
    String symbol = normalizeSymbol(req.symbol);
    String side = normalizeEnum(req.side);
    String orderType = normalizeEnum(req.orderType);
    String qty = req.quantity == null ? "" : req.quantity.toString();
    return (symbol + "|" + side + "|" + qty + "|" + orderType + "|" +
        (req.limitPrice == null ? "" : req.limitPrice.toPlainString()) + "|" + (req.accountId == null ? "" : req.accountId));
  }

  private static TradeOrder.Status parseStatus(String status) {
    if (status == null) return TradeOrder.Status.SUBMITTED;
    try {
      return TradeOrder.Status.valueOf(status);
    } catch (Exception e) {
      return TradeOrder.Status.SUBMITTED;
    }
  }

  private static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(dig);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static NormalizedOrder validate(PlaceOrderRequest req) {
    if (req == null) throw new IllegalStateException("Order request required");
    String symbol = normalizeSymbol(req.symbol);
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalStateException("Symbol is required");
    }
    String side = normalizeEnum(req.side);
    if (!"BUY".equals(side) && !"SELL".equals(side)) {
      throw new IllegalStateException("Side must be BUY or SELL");
    }
    if (req.quantity == null || req.quantity <= 0) {
      throw new IllegalStateException("Quantity must be > 0");
    }
    if (req.quantity > MAX_ORDER_QUANTITY) {
      throw new IllegalStateException("Quantity exceeds max per order");
    }
    String orderType = normalizeEnum(req.orderType);
    if (!"MARKET".equals(orderType) && !"LIMIT".equals(orderType)) {
      throw new IllegalStateException("Order type must be MARKET or LIMIT");
    }
    if ("LIMIT".equals(orderType)) {
      if (req.limitPrice == null || req.limitPrice.signum() <= 0) {
        throw new IllegalStateException("Limit price required for LIMIT orders");
      }
    }
    return new NormalizedOrder(symbol, side, orderType, req.quantity, req.limitPrice);
  }

  private static String normalizeSymbol(String symbol) {
    if (symbol == null) return null;
    return symbol.trim().toUpperCase();
  }

  private static String normalizeEnum(String value) {
    if (value == null) return null;
    return value.trim().toUpperCase();
  }

  private record NormalizedOrder(
      String symbol,
      String side,
      String orderType,
      int quantity,
      java.math.BigDecimal limitPrice
  ) {}
}
