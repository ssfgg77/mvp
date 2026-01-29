package com.example.trading.schwab.dto;

import java.util.List;

public record OrderStatusDto(
    String status,
    List<Object> fillsPayload
) {}
