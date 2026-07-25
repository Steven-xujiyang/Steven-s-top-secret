package com.quant.core.model.portfolio;

import com.quant.core.model.order.Position;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable snapshot of portfolio state at a point in time.
 */
public record PortfolioSnapshot(
    Instant timestamp,
    double cash,
    Map<String, Position> positions,
    double positionValue,
    double nav,
    double unrealizedPnl,
    double realizedPnl,
    double totalCommission,
    double initialCapital
) {
    /**
     * Total return since inception.
     */
    public double totalReturn() {
        return (nav - initialCapital) / initialCapital;
    }

    /**
     * Return positions as an unmodifiable map.
     */
    @Override
    public Map<String, Position> positions() {
        return Collections.unmodifiableMap(positions);
    }

    /**
     * Number of active (non-zero) positions.
     */
    public int activePositionCount() {
        return (int) positions.values().stream()
            .filter(p -> !p.isFlat())
            .count();
    }
}
