package com.example.trading.schwab.dto;

public record SchwabAccountDto(
    String schwabAccountRef,
    String accountType,
    String nickname
) {}
