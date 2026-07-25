package com.quant.core.model.market;

import com.quant.common.enums.BarFrequency;

import java.time.LocalDateTime;

/**
 * OHLCV bar for a single symbol at a given timestamp.
 */
public record Bar(
    String symbol,
    LocalDateTime timestamp,
    BarFrequency frequency,
    double open,
    double high,
    double low,
    double close,
    long volume,
    double amount
) {
    /**
     * Typical price: (high + low + close) / 3.
     */
    public double typicalPrice() {
        return (high + low + close) / 3.0;
    }

    /**
     * Weighted close price: (high + low + 2 * close) / 4.
     */
    public double weightedClose() {
        return (high + low + 2 * close) / 4.0;
    }

    /**
     * Range: high - low.
     */
    public double range() {
        return high - low;
    }

    /**
     * Create a new bar with adjusted prices (for splits/dividends).
     */
    public Bar adjusted(double factor) {
        return new Bar(symbol, timestamp, frequency,
            open * factor, high * factor, low * factor, close * factor,
            volume, amount * factor);
    }
}
