package com.example.trading.schwab.dto;

import java.math.BigDecimal;

/**
 * One account's balances and positions snapshot.
 * Positions payload is kept as an Object placeholder until Schwab DTOs are finalized.
 */
public record AccountSnapshotDto(
    BigDecimal cashBalance,
    BigDecimal equityValue,
    Object positionsPayload
) {}
