package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.execution.Fill;
import com.quant.kernel.market.*;
import com.quant.kernel.portfolio.Order;
import com.quant.kernel.strategy.*;
import com.quant.kernel.time.MarketTime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Falsifiable property #2:
 * Changing future bars must NOT change past artifacts.
 *
 * <p>We run the golden fixture, then modify D3's data and re-run.
 * All artifacts up to D2 CLOSE must be identical.
 */
class BacktestCausalityTest {

    @Test
    void changingFutureBarsDoesNotChangePastArtifacts() {
        // Run 1: original data
        RunSpec spec1 = GoldenFixture.goldenRunSpec("causality-1");
        BacktestEngine engine = new BacktestEngine();
        BacktestResult result1 = engine.run(spec1);

        // Run 2: modify D3 bars (e.g., extreme prices)
        List<DailyBar> modifiedBars = new ArrayList<>(GoldenFixture.goldenBars());
        // Remove D3 bars and replace with wildly different ones
        modifiedBars.removeIf(b -> b.tradingDate().equals(GoldenFixture.D3));
        Instant avail = Instant.parse("2024-01-01T00:00:00Z");
        modifiedBars.add(new DailyBar(GoldenFixture.A, GoldenFixture.D3,
                new BigDecimal("999"), new BigDecimal("999"),
                new BigDecimal("999"), new BigDecimal("999"), 0, avail));
        modifiedBars.add(new DailyBar(GoldenFixture.B, GoldenFixture.D3,
                new BigDecimal("1"), new BigDecimal("1"),
                new BigDecimal("1"), new BigDecimal("1"), 0, avail));

        MarketDataFeed modifiedFeed = new MarketDataFeed(modifiedBars);
        RunSpec spec2 = new RunSpec("causality-2",
                GoldenFixture.INITIAL_CASH, GoldenFixture.COMMISSION_RATE,
                GoldenFixture.MONEY_SCALE, GoldenFixture.ROUNDING_MODE,
                List.of(GoldenFixture.A, GoldenFixture.B),
                GoldenFixture.D1, GoldenFixture.D3,
                modifiedFeed, GoldenFixture.goldenStrategy());
        BacktestResult result2 = engine.run(spec2);

        // All artifacts up to and including D2 CLOSE must be identical
        // Orders are created at D1 CLOSE and D2 CLOSE — both before D3 data is visible
        assertThat(result2.orders())
                .as("Orders must be identical regardless of D3 data")
                .containsExactlyElementsOf(result1.orders());

        // Only fills before D3 must be identical (D3 fills depend on D3 data)
        List<Fill> fills1BeforeD3 = result1.fills().stream()
                .filter(f -> f.filledAt().tradingDate().isBefore(GoldenFixture.D3))
                .toList();
        List<Fill> fills2BeforeD3 = result2.fills().stream()
                .filter(f -> f.filledAt().tradingDate().isBefore(GoldenFixture.D3))
                .toList();
        assertThat(fills2BeforeD3)
                .as("Pre-D3 fills must be identical regardless of D3 data")
                .containsExactlyElementsOf(fills1BeforeD3);

        // Compare snapshots up to D2 CLOSE
        List<LedgerSnapshot> snaps1 = result1.dailySnapshots().stream()
                .filter(s -> !s.time().tradingDate().isAfter(GoldenFixture.D2))
                .toList();
        List<LedgerSnapshot> snaps2 = result2.dailySnapshots().stream()
                .filter(s -> !s.time().tradingDate().isAfter(GoldenFixture.D2))
                .toList();

        assertThat(snaps2).hasSameSizeAs(snaps1);
        for (int i = 0; i < snaps1.size(); i++) {
            LedgerSnapshot s1 = snaps1.get(i);
            LedgerSnapshot s2 = snaps2.get(i);
            assertThat(s2.cash()).as("cash at snapshot %d [%s %s]", i, s1.time().tradingDate(), s1.time().phase())
                    .isEqualByComparingTo(s1.cash());
            assertThat(s2.marketValue()).as("mv at snapshot %d", i)
                    .isEqualByComparingTo(s1.marketValue());
            assertThat(s2.equity()).as("equity at snapshot %d", i)
                    .isEqualByComparingTo(s1.equity());
            assertThat(s2.cumulativeCommission()).as("comm at snapshot %d", i)
                    .isEqualByComparingTo(s1.cumulativeCommission());
        }
    }
}
