package com.quant.core.model.signal;

import java.time.Instant;

/**
 * A single factor value for a symbol at a point in time.
 */
public record FactorValue(
    String factorName,
    String symbol,
    Instant timestamp,
    double rawValue,
    Double zScore,
    Double percentile
) {
    /**
     * Create a factor value with z-score and percentile computed.
     */
    public static FactorValue of(String factorName, String symbol, Instant timestamp,
                                  double rawValue, Double zScore, Double percentile) {
        return new FactorValue(factorName, symbol, timestamp, rawValue, zScore, percentile);
    }

    /**
     * Create a raw factor value without normalization.
     */
    public static FactorValue raw(String factorName, String symbol, Instant timestamp, double rawValue) {
        return new FactorValue(factorName, symbol, timestamp, rawValue, null, null);
    }
}
