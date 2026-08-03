package com.quant.kernel.time;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A point in simulation time, identified by a trading date and a {@link MarketPhase}.
 *
 * <p>Instances are created through the static factories {@link #atOpen(LocalDate)}
 * and {@link #atClose(LocalDate)}, which reject null dates.
 */
public record MarketTime(LocalDate tradingDate, MarketPhase phase)
        implements Comparable<MarketTime> {

    public MarketTime {
        Objects.requireNonNull(tradingDate, "tradingDate must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
    }

    @Override
    public int compareTo(MarketTime other) {
        int dateCmp = this.tradingDate.compareTo(other.tradingDate);
        if (dateCmp != 0) return dateCmp;
        return this.phase.compareTo(other.phase);
    }

    public static MarketTime atOpen(LocalDate date) {
        return new MarketTime(date, MarketPhase.OPEN);
    }

    public static MarketTime atClose(LocalDate date) {
        return new MarketTime(date, MarketPhase.CLOSE);
    }

    /**
     * Returns the OPEN MarketTime for this trading date.
     */
    public MarketTime toOpen() {
        return atOpen(tradingDate);
    }

    /**
     * Returns the CLOSE MarketTime for this trading date.
     */
    public MarketTime toClose() {
        return atClose(tradingDate);
    }
}
