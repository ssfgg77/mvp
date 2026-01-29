package com.example.trading.schwab.dto;

public record PlaceOrderResultDto(
    String schwabOrderId,
    String status
) {}
