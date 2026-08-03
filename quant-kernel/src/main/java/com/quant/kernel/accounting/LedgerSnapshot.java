package com.quant.kernel.accounting;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * An immutable point-in-time snapshot of the ledger.
 *
 * <p>Invariant: {@code equity = cash + marketValue} (enforced by
 * {@link LedgerInvariantChecker} before every snapshot is exposed).
 */
public record LedgerSnapshot(
        MarketTime time,
        BigDecimal cash,
        Map<InstrumentId, Position> positions,
        BigDecimal marketValue,
        BigDecimal equity,
        BigDecimal cumulativeCommission) {

    public LedgerSnapshot {
        Objects.requireNonNull(time, "time must not be null");
        Objects.requireNonNull(cash, "cash must not be null");
        Objects.requireNonNull(positions, "positions must not be null");
        Objects.requireNonNull(marketValue, "marketValue must not be null");
        Objects.requireNonNull(equity, "equity must not be null");
        Objects.requireNonNull(cumulativeCommission, "cumulativeCommission must not be null");

        // Defensive copy into stable-ordered map
        positions = Collections.unmodifiableMap(new TreeMap<>(positions));
    }
}
