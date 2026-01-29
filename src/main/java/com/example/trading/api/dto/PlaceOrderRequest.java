package com.example.trading.api.dto;

import java.math.BigDecimal;

public class PlaceOrderRequest {
  public String symbol;
  public String side;       // BUY/SELL
  public Integer quantity;
  public String orderType;  // MARKET/LIMIT
  public BigDecimal limitPrice; // required if LIMIT
  public Long accountId;    // optional; default account used when null
}
