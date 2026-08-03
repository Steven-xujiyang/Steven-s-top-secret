package com.quant.kernel.strategy;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Validates TargetPortfolio weight constraints.
 */
class TargetPortfolioTest {

    private static final InstrumentId A = new InstrumentId("A");
    private static final InstrumentId B = new InstrumentId("B");
    private static final MarketTime T = MarketTime.atClose(LocalDate.of(2024, 1, 2));

    @Test
    void rejectsNegativeOrOverAllocatedWeights() {
        // Valid: 50/0
        assertDoesNotThrow(() ->
                new TargetPortfolio(T, Map.of(A, new BigDecimal("0.5"))));

        // Valid: 50/50, sum = 1.0
        assertDoesNotThrow(() ->
                new TargetPortfolio(T, Map.of(
                        A, new BigDecimal("0.5"),
                        B, new BigDecimal("0.5"))));

        // Negative weight
        assertThatThrownBy(() ->
                new TargetPortfolio(T, Map.of(A, new BigDecimal("-0.1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");

        // Weight > 1.0
        assertThatThrownBy(() ->
                new TargetPortfolio(T, Map.of(A, new BigDecimal("1.5"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds 1.0");

        // Total > 1.0
        assertThatThrownBy(() ->
                new TargetPortfolio(T, Map.of(
                        A, new BigDecimal("0.6"),
                        B, new BigDecimal("0.6"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total weight exceeds 1.0");
    }
}
