package com.example.trading.schwab.dto;

import java.math.BigDecimal;

public record PlaceOrderRequestDto(
    String symbol,
    String side,
    int quantity,
    String orderType,
    BigDecimal limitPrice
) {}
