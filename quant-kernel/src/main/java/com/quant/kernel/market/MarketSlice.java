package com.quant.kernel.market;

import com.quant.kernel.time.MarketPhase;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A read-only snapshot of market data visible at a given {@link MarketTime}.
 *
 * <p>At {@link MarketPhase#OPEN}, only the current day's open price is visible.
 * At {@link MarketPhase#CLOSE}, the full bar OHLC is visible for the current day,
 * but the next day's data is not yet available (enforced by {@code availableAt}).
 *
 * <p>The slice also exposes the previous day's close for reference
 * (e.g. for position valuation when no position change occurred).
 */
public class MarketSlice {

    private final MarketTime time;
    private final Map<InstrumentId, DailyBar> bars;

    private MarketSlice(MarketTime time, Map<InstrumentId, DailyBar> bars) {
        this.time = Objects.requireNonNull(time);
        this.bars = Collections.unmodifiableMap(new TreeMap<>(bars));
    }

    /**
     * Creates a slice for the OPEN phase — only open prices are visible.
     * Callers must ensure they do not read high/low/close through this slice.
     */
    public static MarketSlice forOpen(MarketTime time, Map<InstrumentId, DailyBar> bars) {
        if (time.phase() != MarketPhase.OPEN) {
            throw new IllegalArgumentException("Expected OPEN phase, got " + time.phase());
        }
        return new MarketSlice(time, bars);
    }

    /**
     * Creates a slice for the CLOSE phase — full OHLC is visible.
     */
    public static MarketSlice forClose(MarketTime time, Map<InstrumentId, DailyBar> bars) {
        if (time.phase() != MarketPhase.CLOSE) {
            throw new IllegalArgumentException("Expected CLOSE phase, got " + time.phase());
        }
        return new MarketSlice(time, bars);
    }

    public MarketTime time() {
        return time;
    }

    /**
     * Returns the price visible for the given instrument at this slice's phase.
     *
     * <p>OPEN  → open price.
     * <br>CLOSE → close price.
     *
     * @throws IllegalArgumentException if the instrument is unknown
     */
    public BigDecimal price(InstrumentId instrument) {
        DailyBar bar = bars.get(instrument);
        if (bar == null) {
            throw new IllegalArgumentException("No bar for instrument " + instrument
                    + " at " + time);
        }
        return time.phase() == MarketPhase.OPEN ? bar.open() : bar.close();
    }

    /**
     * Returns the open price (visible in both phases).
     */
    public BigDecimal open(InstrumentId instrument) {
        DailyBar bar = bars.get(instrument);
        if (bar == null) {
            throw new IllegalArgumentException("No bar for instrument " + instrument);
        }
        return bar.open();
    }

    /**
     * Returns the full bar for the given instrument.
     * Callers must respect phase visibility:
     * at OPEN, only {@link DailyBar#open()} should be consumed.
     */
    public DailyBar bar(InstrumentId instrument) {
        DailyBar bar = bars.get(instrument);
        if (bar == null) {
            throw new IllegalArgumentException("No bar for instrument " + instrument);
        }
        return bar;
    }

    /**
     * All instruments in this slice, in stable order.
     */
    public Map<InstrumentId, DailyBar> bars() {
        return bars;
    }

    public LocalDate tradingDate() {
        return time.tradingDate();
    }

    public MarketPhase phase() {
        return time.phase();
    }
}
