package com.quant.kernel.strategy;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A target portfolio expressed as non-negative weights that sum to at most 1.0.
 *
 * <p>Each weight is in [0, 1]; the total is ≤ 1.0 (the remainder is cash).
 * Weight comparison uses {@link BigDecimal#compareTo}, not scale-sensitive equality.
 *
 * <p>The {@code decidedAt} timestamp records when the target was produced;
 * execution may happen at a later market phase.
 */
public record TargetPortfolio(
        MarketTime decidedAt,
        Map<InstrumentId, BigDecimal> weights) {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public TargetPortfolio(
            MarketTime decidedAt,
            Map<InstrumentId, BigDecimal> weights) {

        Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        Objects.requireNonNull(weights, "weights must not be null");

        // Defensive copy into stable-ordered map, validate weights
        Map<InstrumentId, BigDecimal> copy = new TreeMap<>();
        BigDecimal sum = ZERO;
        for (Map.Entry<InstrumentId, BigDecimal> entry : weights.entrySet()) {
            InstrumentId id = Objects.requireNonNull(entry.getKey(), "weight key must not be null");
            BigDecimal w = Objects.requireNonNull(entry.getValue(), "weight must not be null");

            if (w.compareTo(ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Weight for " + id + " is negative: " + w);
            }
            if (w.compareTo(ONE) > 0) {
                throw new IllegalArgumentException(
                        "Weight for " + id + " exceeds 1.0: " + w);
            }
            sum = sum.add(w);
            if (w.compareTo(ZERO) > 0) {
                copy.put(id, w);
            }
        }

        if (sum.compareTo(ONE) > 0) {
            throw new IllegalArgumentException(
                    "Total weight exceeds 1.0: " + sum);
        }

        this.decidedAt = decidedAt;
        this.weights = Collections.unmodifiableMap(copy);
    }

    /**
     * The weight assigned to an instrument, or zero if not present.
     */
    public BigDecimal weight(InstrumentId instrument) {
        return weights.getOrDefault(instrument, ZERO);
    }

    /**
     * Total allocated weight.
     */
    public BigDecimal totalWeight() {
        BigDecimal sum = ZERO;
        for (BigDecimal w : weights.values()) {
            sum = sum.add(w);
        }
        return sum;
    }
}
