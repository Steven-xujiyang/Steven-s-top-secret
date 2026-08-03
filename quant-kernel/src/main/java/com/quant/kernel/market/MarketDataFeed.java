package com.quant.kernel.market;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * In-memory market data feed.
 *
 * <p>Provides daily bars keyed by instrument and trading date.  All data is
 * loaded up-front; this PR does not include streaming or lazy-loading adapters.
 */
public class MarketDataFeed {

    private final NavigableMap<LocalDate, Map<InstrumentId, DailyBar>> dataByDate;

    public MarketDataFeed(List<DailyBar> allBars) {
        Objects.requireNonNull(allBars, "allBars must not be null");
        Map<LocalDate, Map<InstrumentId, DailyBar>> grouped = new TreeMap<>();
        for (DailyBar bar : allBars) {
            grouped.computeIfAbsent(bar.tradingDate(), k -> new TreeMap<>())
                    .put(bar.instrument(), bar);
        }
        this.dataByDate = Collections.unmodifiableNavigableMap(
                new TreeMap<>(grouped));
    }

    /**
     * Returns all trading dates in this feed, sorted.
     */
    public NavigableSet<LocalDate> tradingDates() {
        return dataByDate.navigableKeySet();
    }

    /**
     * Returns the bars for a specific date, keyed by instrument (stable order).
     */
    public Map<InstrumentId, DailyBar> barsFor(LocalDate date) {
        Map<InstrumentId, DailyBar> bars = dataByDate.get(date);
        if (bars == null) {
            return Collections.emptyMap();
        }
        return bars;
    }

    /**
     * Returns the bar for a specific instrument and date, or {@code null}.
     */
    public DailyBar bar(InstrumentId instrument, LocalDate date) {
        Map<InstrumentId, DailyBar> bars = dataByDate.get(date);
        return bars != null ? bars.get(instrument) : null;
    }

    /**
     * Returns the earliest date with data that is strictly after {@code date},
     * or {@code null} if none.
     */
    public LocalDate nextTradingDate(LocalDate date) {
        return dataByDate.higherKey(date);
    }

    /**
     * Returns the latest date with data that is before or equal to {@code date},
     * or {@code null} if none.
     */
    public LocalDate tradingDateOnOrBefore(LocalDate date) {
        return dataByDate.floorKey(date);
    }

    /**
     * Returns the bar for the earliest available date strictly after {@code date},
     * useful for retrieving "next day's" open for next-bar-open execution.
     */
    public Map<InstrumentId, DailyBar> nextDayBars(LocalDate date) {
        LocalDate next = nextTradingDate(date);
        if (next == null) return Collections.emptyMap();
        return barsFor(next);
    }
}
