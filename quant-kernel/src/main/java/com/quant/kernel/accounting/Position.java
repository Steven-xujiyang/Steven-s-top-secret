package com.quant.kernel.accounting;

import com.quant.kernel.market.InstrumentId;

import java.util.Objects;

/**
 * A position in a single instrument: quantity held.
 *
 * <p>This PR is long-only, so quantity is always ≥ 0.
 * Position is a value object used inside {@link LedgerSnapshot}.
 */
public record Position(InstrumentId instrument, long quantity) {

    public Position {
        Objects.requireNonNull(instrument, "instrument must not be null");
        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Position quantity must be >= 0 for long-only, got " + quantity);
        }
    }

    /**
     * Market value at the given price.
     */
    public java.math.BigDecimal marketValue(java.math.BigDecimal price) {
        return price.multiply(java.math.BigDecimal.valueOf(quantity));
    }

    public boolean isFlat() {
        return quantity == 0;
    }
}
