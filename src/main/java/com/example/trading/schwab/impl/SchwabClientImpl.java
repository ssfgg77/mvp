package com.example.trading.schwab.impl;

import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SchwabClientImpl implements SchwabClient {

  private final RestClient rest;
  private final String apiBaseUrl;
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<Map<String, Object>> MAP =
      new ParameterizedTypeReference<>() {};

  public SchwabClientImpl(RestClient rest, @Value("${app.schwab.api-base-url}") String apiBaseUrl) {
    this.rest = rest;
    this.apiBaseUrl = apiBaseUrl;
  }

  @Override
  public List<SchwabAccountDto> getAccounts(String accessToken) {
    var accounts = retrieveWithErrors(
        rest.get()
            .uri(url("/trader/v1/accounts/accountNumbers"))
            .headers(h -> h.setBearerAuth(accessToken))
    ).body(LIST_OF_MAP);

    if (accounts == null || accounts.isEmpty()) {
      return List.of();
    }

    return accounts.stream()
        .map(item -> {
          String ref = firstString(item, "hashValue", "accountHash", "accountNumber");
          String accountType = firstString(item, "accountType", "type");
          String nickname = firstString(item, "nickname", "accountNickName", "accountName", "displayName");
          if (nickname == null) {
            nickname = firstString(item, "accountNumber", "hashValue");
          }
          if (accountType == null) {
            accountType = "UNKNOWN";
          }
          return new SchwabAccountDto(ref, accountType, nickname);
        })
        .toList();
  }

  @Override
  public AccountSnapshotDto getDefaultAccountSnapshot(String accessToken, String schwabAccountRef) {
    URI uri = UriComponentsBuilder.fromUriString(url("/trader/v1/accounts/" + schwabAccountRef))
        .queryParam("fields", "positions")
        .build()
        .toUri();

    Map<String, Object> payload = retrieveWithErrors(
        rest.get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(accessToken))
    ).body(MAP);

    Map<String, Object> account = extractSecuritiesAccount(payload);
    Map<String, Object> balances = extractBalances(payload, account);

    BigDecimal cashBalance = firstBigDecimal(balances,
        "cashBalance",
        "cash",
        "cashAvailableForTrading",
        "moneyMarketFund"
    );
    BigDecimal equityValue = firstBigDecimal(balances,
        "equity",
        "equityValue",
        "liquidationValue",
        "totalValue"
    );

    Object positions = account != null ? account.get("positions") : null;
    if (positions == null && payload != null) {
      positions = payload.get("positions");
    }

    return new AccountSnapshotDto(cashBalance, equityValue, positions);
  }

  @Override
  public QuotesDto getQuotes(String accessToken, List<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return new QuotesDto(Collections.emptyMap());
    }

    URI uri = UriComponentsBuilder.fromUriString(url("/marketdata/v1/quotes"))
        .queryParam("symbols", String.join(",", symbols))
        .build()
        .toUri();

    Map<String, Object> body = retrieveWithErrors(
        rest.get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(accessToken))
    ).body(MAP);

    return new QuotesDto(body == null ? Collections.emptyMap() : body);
  }

  @Override
  public PlaceOrderResultDto placeOrder(String accessToken, String schwabAccountRef, PlaceOrderRequestDto request) {
    Map<String, Object> orderPayload = new java.util.LinkedHashMap<>();
    orderPayload.put("orderType", request.orderType());
    orderPayload.put("session", "NORMAL");
    orderPayload.put("duration", "DAY");
    orderPayload.put("orderStrategyType", "SINGLE");
    orderPayload.put("complexOrderStrategyType", "NONE");
    orderPayload.put("orderLegCollection", List.of(
        Map.of(
            "instruction", request.side(),
            "quantity", request.quantity(),
            "instrument", Map.of(
                "symbol", request.symbol(),
                "assetType", "EQUITY"
            )
        )
    ));

    if ("LIMIT".equalsIgnoreCase(request.orderType()) && request.limitPrice() != null) {
      orderPayload.put("price", request.limitPrice());
    }

    ResponseEntity<Map<String, Object>> response = retrieveWithErrors(
        rest.post()
            .uri(url("/trader/v1/accounts/" + schwabAccountRef + "/orders"))
            .headers(h -> h.setBearerAuth(accessToken))
            .body(orderPayload)
    ).toEntity(MAP);

    String orderId = extractOrderId(response.getHeaders().getLocation());
    Map<String, Object> body = response.getBody();
    String status = firstString(body, "status", "orderStatus");
    if (status == null) {
      status = "SUBMITTED";
    }
    if (orderId == null) {
      orderId = firstString(body, "orderId", "id");
    }
    return new PlaceOrderResultDto(orderId, status);
  }

  @Override
  public CancelOrderResultDto cancelOrder(String accessToken, String schwabAccountRef, String schwabOrderId) {
    ResponseEntity<Void> response = retrieveWithErrors(
        rest.delete()
            .uri(url("/trader/v1/accounts/" + schwabAccountRef + "/orders/" + schwabOrderId))
            .headers(h -> h.setBearerAuth(accessToken))
    ).toBodilessEntity();

    String status = response.getStatusCode().is2xxSuccessful() ? "CANCEL_REQUESTED" : "UNKNOWN";
    return new CancelOrderResultDto(status);
  }

  @Override
  public OrderStatusDto getOrderStatus(String accessToken, String schwabAccountRef, String schwabOrderId) {
    Map<String, Object> body = retrieveWithErrors(
        rest.get()
            .uri(url("/trader/v1/accounts/" + schwabAccountRef + "/orders/" + schwabOrderId))
            .headers(h -> h.setBearerAuth(accessToken))
    ).body(MAP);

    String status = firstString(body, "status", "orderStatus");
    List<Object> fills = extractFills(body);
    return new OrderStatusDto(status, fills);
  }

  private RestClient.ResponseSpec retrieveWithErrors(RestClient.RequestHeadersSpec<?> spec) {
    return spec.retrieve()
        .onStatus(this::isAuthError, (req, res) -> {
          throw new IllegalStateException("Schwab auth invalid; reconnect");
        })
        .onStatus(this::isRateLimit, (req, res) -> {
          throw new IllegalStateException("Rate limited");
        })
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new IllegalStateException("Schwab request failed: " + res.getStatusCode().value());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new RuntimeException("Schwab server error: " + res.getStatusCode().value());
        });
  }

  private boolean isAuthError(HttpStatusCode status) {
    int code = status.value();
    return code == 401 || code == 403;
  }

  private boolean isRateLimit(HttpStatusCode status) {
    return status.value() == 429;
  }

  private String url(String path) {
    if (apiBaseUrl.endsWith("/")) {
      return apiBaseUrl + stripLeadingSlash(path);
    }
    return apiBaseUrl + (path.startsWith("/") ? path : "/" + path);
  }

  private String stripLeadingSlash(String path) {
    if (path == null) return "";
    return path.startsWith("/") ? path.substring(1) : path;
  }

  private Map<String, Object> extractSecuritiesAccount(Map<String, Object> payload) {
    if (payload == null) return null;
    Object sec = payload.get("securitiesAccount");
    if (sec instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return payload;
  }

  private Map<String, Object> extractBalances(Map<String, Object> payload, Map<String, Object> account) {
    Map<String, Object> balances = asMap(payload == null ? null : payload.get("aggregatedBalance"));
    if (balances == null && account != null) {
      balances = asMap(account.get("aggregatedBalance"));
    }
    return balances;
  }

  private List<Object> extractFills(Map<String, Object> payload) {
    if (payload == null) return null;
    Object activity = payload.get("orderActivityCollection");
    if (activity instanceof List<?> list) {
      return (List<Object>) list;
    }
    Object fills = payload.get("fills");
    if (fills instanceof List<?> list) {
      return (List<Object>) list;
    }
    return null;
  }

  private String firstString(Map<String, Object> map, String... keys) {
    if (map == null || keys == null) return null;
    for (String key : keys) {
      Object value = map.get(key);
      if (value != null) {
        String str = value.toString();
        if (!str.isBlank()) return str;
      }
    }
    return null;
  }

  private BigDecimal firstBigDecimal(Map<String, Object> map, String... keys) {
    if (map == null || keys == null) return null;
    for (String key : keys) {
      BigDecimal value = asBigDecimal(map.get(key));
      if (value != null) return value;
    }
    return null;
  }

  private BigDecimal asBigDecimal(Object value) {
    if (value == null) return null;
    if (value instanceof BigDecimal bd) return bd;
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    if (value instanceof String s && !s.isBlank()) {
      try {
        return new BigDecimal(s);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return null;
  }

  private String extractOrderId(URI location) {
    if (location == null) return null;
    String path = location.getPath();
    if (path == null || path.isBlank()) return null;
    int idx = path.lastIndexOf('/');
    if (idx >= 0 && idx < path.length() - 1) {
      return path.substring(idx + 1);
    }
    return null;
  }
}
