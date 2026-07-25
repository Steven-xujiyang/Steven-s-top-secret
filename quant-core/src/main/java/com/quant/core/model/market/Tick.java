package com.quant.core.model.market;

import java.time.LocalDateTime;

/**
 * Tick-level market data.
 */
public record Tick(
    String symbol,
    LocalDateTime timestamp,
    double price,
    long volume,
    double amount,
    double bid1,
    double ask1,
    long bidVolume1,
    long askVolume1
) {
}
