package com.example.trading.schwab.dto;

import java.util.Map;

public record QuotesDto(
    Map<String, Object> quotesBySymbol
) {}
