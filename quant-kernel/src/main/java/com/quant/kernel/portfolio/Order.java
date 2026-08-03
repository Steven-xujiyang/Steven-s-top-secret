package com.quant.kernel.portfolio;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import java.util.Objects;

/**
 * An immutable order placed by the strategy, to be executed at the next bar open.
 *
 * <p>This PR only supports positive integer quantities and {@link ExecutionPolicy#NEXT_BAR_OPEN}.
 */
public record Order(
        OrderId id,
        InstrumentId instrument,
        Side side,
        long quantity,
        MarketTime createdAt,
        ExecutionPolicy executionPolicy) {

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(instrument, "instrument must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive, got " + quantity);
        }
        if (executionPolicy != ExecutionPolicy.NEXT_BAR_OPEN) {
            throw new IllegalArgumentException(
                    "Only NEXT_BAR_OPEN is supported, got " + executionPolicy);
        }
    }
}
