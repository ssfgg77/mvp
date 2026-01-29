package com.example.trading.poller;

import com.example.trading.persistence.entity.ExecutionFill;
import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.repo.ExecutionFillRepository;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.OrderStatusDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Poll open orders every 5s and reconcile status/fills into DB.
 * Single-instance assumption (approved).
 */
@Component
public class OrderReconciliationJob {
  private static final Logger log = LoggerFactory.getLogger(OrderReconciliationJob.class);

  private final TradeOrderRepository orders;
  private final ExecutionFillRepository fills;
  private final SchwabClient schwab;
  private final OAuth2AuthorizedClientService authorizedClients;

  private final long intervalMs;
  private final Map<Long, Instant> rateLimitBackoffUntil = new ConcurrentHashMap<>();
  public OrderReconciliationJob(
      TradeOrderRepository orders,
      ExecutionFillRepository fills,
      SchwabClient schwab,
      OAuth2AuthorizedClientService authorizedClients,
      @Value("${app.polling.orders-ms:5000}") long intervalMs
  ) {
    this.orders = orders;
    this.fills = fills;
    this.schwab = schwab;
    this.authorizedClients = authorizedClients;
    this.intervalMs = intervalMs;
  }

  @Scheduled(fixedDelayString = "${app.polling.orders-ms:5000}")
  @Transactional
  public void run() {
    List<TradeOrder> reconcilable = orders.findReconcilable(List.of(
        TradeOrder.Status.SUBMITTED,
        TradeOrder.Status.OPEN,
        TradeOrder.Status.PARTIALLY_FILLED,
        TradeOrder.Status.CANCEL_REQUESTED
    ));

    if (reconcilable.isEmpty()) return;

    var byUser = reconcilable.stream()
        .collect(Collectors.groupingBy(TradeOrder::getUser));

    for (var entry : byUser.entrySet()) {
      var user = entry.getKey();
      var userOrders = entry.getValue();

      Instant backoffUntil = rateLimitBackoffUntil.get(user.getId());
      if (backoffUntil != null && backoffUntil.isAfter(Instant.now())) {
        continue;
      }

      var client = authorizedClients.loadAuthorizedClient("schwab", user.getUsername());
      if (client == null || client.getAccessToken() == null) {
        continue;
      }
      String token = client.getAccessToken().getTokenValue();

      for (TradeOrder order : userOrders) {
        if (order.getSchwabOrderId() == null) {
          continue;
        }
        try {
          OrderStatusDto status = schwab.getOrderStatus(
              token,
              order.getSchwabAccount().getSchwabAccountRef(),
              order.getSchwabOrderId()
          );
          reconcileOrder(order, status);
        } catch (IllegalStateException e) {
          if ("Rate limited".equalsIgnoreCase(e.getMessage())) {
            rateLimitBackoffUntil.put(user.getId(), Instant.now().plusSeconds(30));
            log.warn("Rate limited for user={}, skipping remaining orders this cycle", user.getUsername());
            break;
          }
          log.info("Order reconciliation failed user={} order={} reason={}",
              user.getUsername(), order.getId(), e.getMessage());
        } catch (Exception e) {
          log.info("Order reconciliation failed user={} order={}",
              user.getUsername(), order.getId(), e);
        }
      }
    }

    log.debug("Reconciling {} orders at {}", reconcilable.size(), Instant.now());
  }

  private void reconcileOrder(TradeOrder order, OrderStatusDto statusDto) {
    if (statusDto == null) return;

    TradeOrder.Status mapped = mapStatus(statusDto.status(), order.getStatus());
    if (mapped != null) {
      order.setStatus(mapped);
      order.setUpdatedAt(Instant.now());
      orders.save(order);
    }

    upsertFills(order, statusDto.fillsPayload());
  }

  private void upsertFills(TradeOrder order, List<Object> fillsPayload) {
    if (fillsPayload == null || fillsPayload.isEmpty()) return;
    for (Object obj : fillsPayload) {
      if (!(obj instanceof Map<?, ?> map)) continue;
      String externalExecId = firstString(map, "executionId", "execId", "externalExecId", "id");
      Instant execTime = firstInstant(map, "execTime", "executionTime", "filledTime", "time");
      Integer quantity = firstInt(map, "quantity", "filledQuantity", "qty");
      BigDecimal price = firstBigDecimal(map, "price", "executionPrice", "fillPrice");

      if (execTime == null || quantity == null || price == null) {
        continue;
      }

      if (externalExecId == null) {
        externalExecId = sha256(execTime + "|" + quantity + "|" + price);
      }

      if (fills.existsByOrderAndExternalExecId(order, externalExecId)) {
        continue;
      }

      ExecutionFill fill = new ExecutionFill();
      fill.setOrder(order);
      fill.setExternalExecId(externalExecId);
      fill.setExecTime(execTime);
      fill.setQuantity(quantity);
      fill.setPrice(price);
      fills.save(fill);
    }
  }

  private TradeOrder.Status mapStatus(String status, TradeOrder.Status fallback) {
    if (status == null) return fallback;
    String normalized = status.trim().toUpperCase();
    return switch (normalized) {
      case "FILLED" -> TradeOrder.Status.FILLED;
      case "CANCELED", "CANCELLED" -> TradeOrder.Status.CANCELED;
      case "REJECTED" -> TradeOrder.Status.REJECTED;
      case "PARTIALLY_FILLED" -> TradeOrder.Status.PARTIALLY_FILLED;
      case "OPEN" -> TradeOrder.Status.OPEN;
      case "SUBMITTED" -> TradeOrder.Status.SUBMITTED;
      case "CANCEL_REQUESTED" -> TradeOrder.Status.CANCEL_REQUESTED;
      default -> fallback;
    };
  }

  private String firstString(Map<?, ?> map, String... keys) {
    for (String key : keys) {
      Object value = map.get(key);
      if (value != null) {
        String str = value.toString();
        if (!str.isBlank()) return str;
      }
    }
    return null;
  }

  private Integer firstInt(Map<?, ?> map, String... keys) {
    for (String key : keys) {
      Object value = map.get(key);
      Integer parsed = asInt(value);
      if (parsed != null) return parsed;
    }
    return null;
  }

  private BigDecimal firstBigDecimal(Map<?, ?> map, String... keys) {
    for (String key : keys) {
      Object value = map.get(key);
      BigDecimal parsed = asBigDecimal(value);
      if (parsed != null) return parsed;
    }
    return null;
  }

  private Instant firstInstant(Map<?, ?> map, String... keys) {
    for (String key : keys) {
      Object value = map.get(key);
      Instant parsed = asInstant(value);
      if (parsed != null) return parsed;
    }
    return null;
  }

  private Integer asInt(Object value) {
    if (value instanceof Number n) return n.intValue();
    if (value instanceof String s && !s.isBlank()) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private BigDecimal asBigDecimal(Object value) {
    if (value instanceof BigDecimal bd) return bd;
    if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
    if (value instanceof String s && !s.isBlank()) {
      try {
        return new BigDecimal(s);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private Instant asInstant(Object value) {
    if (value instanceof Instant instant) return instant;
    if (value instanceof Number n) return Instant.ofEpochMilli(n.longValue());
    if (value instanceof String s && !s.isBlank()) {
      try {
        return Instant.parse(s);
      } catch (Exception ignored) {
        try {
          return Instant.ofEpochMilli(Long.parseLong(s));
        } catch (NumberFormatException ignored2) {
          return null;
        }
      }
    }
    return null;
  }

  private String sha256(String input) {
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(bytes);
    } catch (Exception e) {
      return input;
    }
  }
}
