package com.quant.kernel.strategy;

import com.quant.kernel.market.InstrumentId;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only context passed to a {@link Strategy} at decision time.
 *
 * <p>Contains the current portfolio state: cash, positions (quantity per instrument),
 * and the equity as valued at the latest market prices.
 */
public class StrategyContext {

    private final BigDecimal cash;
    private final Map<InstrumentId, Long> positions;
    private final BigDecimal equity;

    public StrategyContext(BigDecimal cash,
                           Map<InstrumentId, Long> positions,
                           BigDecimal equity) {
        this.cash = Objects.requireNonNull(cash, "cash must not be null");
        this.positions = Collections.unmodifiableMap(
                Map.copyOf(Objects.requireNonNull(positions, "positions must not be null")));
        this.equity = Objects.requireNonNull(equity, "equity must not be null");
    }

    public BigDecimal cash() {
        return cash;
    }

    public Map<InstrumentId, Long> positions() {
        return positions;
    }

    /**
     * Returns the quantity held for an instrument, or 0 if none.
     */
    public long position(InstrumentId instrument) {
        return positions.getOrDefault(instrument, 0L);
    }

    public BigDecimal equity() {
        return equity;
    }
}
