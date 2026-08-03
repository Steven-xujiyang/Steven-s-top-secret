package com.quant.kernel.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Validates DailyBar construction constraints.
 */
class DailyBarTest {

    private static final InstrumentId AAPL = new InstrumentId("AAPL");
    private static final LocalDate D1 = LocalDate.of(2024, 1, 2);
    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void rejectsInvalidOhlcAndNegativeVolume() {
        // Valid bar
        assertDoesNotThrow(() ->
                new DailyBar(AAPL, D1, new BigDecimal("100"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0));

        // Zero open
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, BigDecimal.ZERO, new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("> 0");

        // open > high
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, new BigDecimal("106"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[low, high]");

        // open < low
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, new BigDecimal("98"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[low, high]");

        // close > high
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, new BigDecimal("100"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("106"), 1000, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[low, high]");

        // close < low
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, new BigDecimal("100"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("98"), 1000, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[low, high]");

        // negative volume
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, new BigDecimal("100"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), -1, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");

        // null instrument
        assertThatThrownBy(() ->
                new DailyBar(null, D1, new BigDecimal("100"), new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0))
                .isInstanceOf(NullPointerException.class);

        // null price
        assertThatThrownBy(() ->
                new DailyBar(AAPL, D1, null, new BigDecimal("105"),
                        new BigDecimal("99"), new BigDecimal("102"), 1000, T0))
                .isInstanceOf(NullPointerException.class);
    }
}
