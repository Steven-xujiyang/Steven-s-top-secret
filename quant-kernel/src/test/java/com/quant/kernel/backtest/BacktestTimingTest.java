package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.execution.Fill;
import com.quant.kernel.time.MarketPhase;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Falsifiable property #1:
 * Orders generated at CLOSE must NOT fill at that same day's CLOSE price.
 *
 * <p>This test was designed to FAIL before implementation exists.
 * After implementation, it verifies the NEXT_BAR_OPEN contract:
 * fills happen at the next day's OPEN, not at the CLOSE price.
 */
class BacktestTimingTest {

    @Test
    void closeDecisionCannotFillAtSameClose() {
        RunSpec spec = GoldenFixture.goldenRunSpec("timing-test");
        BacktestEngine engine = new BacktestEngine();
        BacktestResult result = engine.run(spec);

        // D1 CLOSE generated orders. They must NOT fill at D1 CLOSE.
        // Find any fill that occurred at a CLOSE phase
        for (Fill fill : result.fills()) {
            assertThat(fill.filledAt().phase())
                    .as("Fill %s must not occur at CLOSE phase", fill.id())
                    .isEqualTo(MarketPhase.OPEN);

            // Fill date must be strictly after the order creation date
            // Order created at D1 CLOSE → fill at D2 OPEN (or later)
            assertThat(fill.filledAt().tradingDate())
                    .as("Fill %s date must be > order creation (D1 CLOSE)", fill.id())
                    .isAfter(LocalDate.of(2024, 1, 2));
        }

        // The first fill uses D2 open price (110), NOT D1 close (100)
        Fill firstFill = result.fills().get(0);
        assertThat(firstFill.price())
                .as("First fill price must be D2 open=110, not D1 close=100")
                .isEqualByComparingTo(new BigDecimal("110"));

        // Explicitly verify: D1 has NO fills
        long d1Fills = result.fills().stream()
                .filter(f -> f.filledAt().tradingDate().equals(GoldenFixture.D1))
                .count();
        assertThat(d1Fills).as("D1 must have zero fills").isZero();
    }
}
