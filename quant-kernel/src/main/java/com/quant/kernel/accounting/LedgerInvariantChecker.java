package com.quant.kernel.accounting;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Checks the fundamental accounting invariant after every ledger mutation.
 *
 * <p>Invariant: {@code cash + sum(positionQty × mark) == equity}.
 * Failure throws a domain exception containing runId, eventTime, and context.
 */
public final class LedgerInvariantChecker {

    private LedgerInvariantChecker() {
        // static utility
    }

    /**
     * Verifies that {@code cash + marketValue == equity}.
     *
     * @throws LedgerInvariantException if the invariant is violated
     */
    public static void check(MarketTime time,
                             BigDecimal cash,
                             Map<InstrumentId, Position> positions,
                             Map<InstrumentId, BigDecimal> prices,
                             BigDecimal equity,
                             BigDecimal cumulativeCommission,
                             String runId) {

        BigDecimal computedMarketValue = BigDecimal.ZERO;
        for (Map.Entry<InstrumentId, Position> entry : positions.entrySet()) {
            BigDecimal price = prices.get(entry.getKey());
            if (price == null) {
                throw new LedgerInvariantException(
                        "No price for instrument " + entry.getKey()
                                + " at " + time + " [run=" + runId + "]");
            }
            computedMarketValue = computedMarketValue.add(
                    entry.getValue().marketValue(price));
        }

        BigDecimal computedEquity = cash.add(computedMarketValue);

        if (computedEquity.compareTo(equity) != 0) {
            throw new LedgerInvariantException(
                    "Equity invariant violated at " + time
                            + ": cash=" + cash
                            + " + marketValue=" + computedMarketValue
                            + " = " + computedEquity
                            + " but equity=" + equity
                            + " [run=" + runId + "]");
        }
    }
}
