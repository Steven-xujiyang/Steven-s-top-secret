package com.quant.kernel.accounting;

import com.quant.kernel.execution.Fill;
import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Side;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Mutable accounting book that tracks cash, positions, and cumulative commission.
 *
 * <p>Every mutation (applying a fill) preserves the identity:
 * {@code equity = cash + marketValue}.
 *
 * <p>This class is NOT thread-safe; it is owned by a single backtest run.
 */
public class Ledger {

    private BigDecimal cash;
    private final Map<InstrumentId, Long> positions; // TreeMap for stable ordering
    private BigDecimal cumulativeCommission;
    private final int moneyScale;
    private final RoundingMode roundingMode;
    private final String runId;

    public Ledger(BigDecimal initialCash, int moneyScale, RoundingMode roundingMode,
                  String runId) {
        Objects.requireNonNull(initialCash, "initialCash must not be null");
        if (initialCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialCash must be >= 0, got " + initialCash);
        }
        if (moneyScale < 0) {
            throw new IllegalArgumentException("moneyScale must be >= 0, got " + moneyScale);
        }
        Objects.requireNonNull(roundingMode, "roundingMode must not be null");
        Objects.requireNonNull(runId, "runId must not be null");

        this.cash = initialCash.setScale(moneyScale, roundingMode);
        this.positions = new TreeMap<>();
        this.cumulativeCommission = BigDecimal.ZERO.setScale(moneyScale, roundingMode);
        this.moneyScale = moneyScale;
        this.roundingMode = roundingMode;
        this.runId = runId;
    }

    /**
     * Applies a fill, updating cash, positions, and cumulative commission.
     *
     * <p>BUY:  {@code cash -= quantity×price + commission}, position += quantity
     * <br>SELL: {@code cash += quantity×price - commission}, position -= quantity
     */
    public void applyFill(Fill fill) {
        long currentQty = positions.getOrDefault(fill.instrument(), 0L);

        if (fill.side() == Side.BUY) {
            BigDecimal cost = fill.price().multiply(BigDecimal.valueOf(fill.quantity()));
            cash = cash.subtract(cost).subtract(fill.commission())
                    .setScale(moneyScale, roundingMode);
            positions.put(fill.instrument(), currentQty + fill.quantity());
        } else {
            // SELL
            BigDecimal proceeds = fill.price().multiply(BigDecimal.valueOf(fill.quantity()));
            cash = cash.add(proceeds).subtract(fill.commission())
                    .setScale(moneyScale, roundingMode);
            long newQty = currentQty - fill.quantity();
            if (newQty < 0) {
                throw new IllegalStateException(
                        "Cannot sell more than held: " + fill.instrument()
                                + " held=" + currentQty + " sell=" + fill.quantity()
                                + " [run=" + runId + "]");
            }
            if (newQty == 0) {
                positions.remove(fill.instrument());
            } else {
                positions.put(fill.instrument(), newQty);
            }
        }

        cumulativeCommission = cumulativeCommission.add(fill.commission())
                .setScale(moneyScale, roundingMode);
    }

    /**
     * Computes market value and equity at the given prices, then verifies
     * the invariant. Returns a snapshot if valid.
     */
    public LedgerSnapshot snapshot(MarketTime time, Map<InstrumentId, BigDecimal> prices) {
        BigDecimal marketValue = BigDecimal.ZERO;
        Map<InstrumentId, Position> posMap = new TreeMap<>();

        for (Map.Entry<InstrumentId, Long> entry : positions.entrySet()) {
            BigDecimal price = prices.get(entry.getKey());
            if (price == null) {
                throw new IllegalArgumentException(
                        "No price for instrument " + entry.getKey()
                                + " at " + time + " [run=" + runId + "]");
            }
            Position pos = new Position(entry.getKey(), entry.getValue());
            posMap.put(entry.getKey(), pos);
            marketValue = marketValue.add(pos.marketValue(price))
                    .setScale(moneyScale, roundingMode);
        }

        BigDecimal equity = cash.add(marketValue)
                .setScale(moneyScale, roundingMode);

        LedgerInvariantChecker.check(time, cash, posMap, prices,
                equity, cumulativeCommission, runId);

        return new LedgerSnapshot(time, cash, posMap, marketValue, equity,
                cumulativeCommission);
    }

    // — read-only accessors —

    public BigDecimal cash() {
        return cash;
    }

    public Map<InstrumentId, Long> positions() {
        return Collections.unmodifiableMap(new TreeMap<>(positions));
    }

    public long position(InstrumentId instrument) {
        return positions.getOrDefault(instrument, 0L);
    }

    public BigDecimal cumulativeCommission() {
        return cumulativeCommission;
    }

    public int moneyScale() {
        return moneyScale;
    }

    public RoundingMode roundingMode() {
        return roundingMode;
    }

    public String runId() {
        return runId;
    }
}
