package com.quant.kernel.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single-instrument daily OHLCV bar with availability control.
 *
 * <p>Constraints (enforced at construction):
 * <ul>
 *   <li>All prices strictly &gt; 0.</li>
 *   <li>{@code low <= open/close <= high}.</li>
 *   <li>{@code volume >= 0}.</li>
 * </ul>
 *
 * <p>The {@code availableAt} instant models the time at which this bar's data
 * becomes visible to market participants. For backtesting, this is used to
 * prevent look-ahead: a strategy running at D1 close cannot inspect D2's bar
 * unless D2's {@code availableAt} is before D1's close event.
 */
public record DailyBar(
        InstrumentId instrument,
        LocalDate tradingDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume,
        Instant availableAt) {

    public DailyBar {
        Objects.requireNonNull(instrument, "instrument must not be null");
        Objects.requireNonNull(tradingDate, "tradingDate must not be null");
        Objects.requireNonNull(open, "open must not be null");
        Objects.requireNonNull(high, "high must not be null");
        Objects.requireNonNull(low, "low must not be null");
        Objects.requireNonNull(close, "close must not be null");
        Objects.requireNonNull(availableAt, "availableAt must not be null");

        if (open.compareTo(BigDecimal.ZERO) <= 0
                || high.compareTo(BigDecimal.ZERO) <= 0
                || low.compareTo(BigDecimal.ZERO) <= 0
                || close.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("All prices must be > 0, got open="
                    + open + " high=" + high + " low=" + low + " close=" + close);
        }

        if (low.compareTo(open) > 0 || open.compareTo(high) > 0) {
            throw new IllegalArgumentException("open must be in [low, high]: open="
                    + open + " low=" + low + " high=" + high);
        }
        if (low.compareTo(close) > 0 || close.compareTo(high) > 0) {
            throw new IllegalArgumentException("close must be in [low, high]: close="
                    + close + " low=" + low + " high=" + high);
        }

        if (volume < 0) {
            throw new IllegalArgumentException("volume must be >= 0, got " + volume);
        }
    }
}
