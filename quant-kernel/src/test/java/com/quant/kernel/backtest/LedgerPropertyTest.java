package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test: after every event, {@code equity == cash + marketValue}.
 *
 * <p>This test runs the full golden fixture and checks the invariant
 * on every single snapshot produced.
 */
class LedgerPropertyTest {

    @Test
    void equityAlwaysEqualsCashPlusMarketValue() {
        RunSpec spec = GoldenFixture.goldenRunSpec("ledger-prop");
        BacktestEngine engine = new BacktestEngine();
        BacktestResult result = engine.run(spec);

        for (LedgerSnapshot snap : result.dailySnapshots()) {
            BigDecimal computedEquity = snap.cash().add(snap.marketValue());
            assertThat(snap.equity())
                    .as("Equity invariant at %s %s: cash=%s + mv=%s != equity=%s",
                            snap.time().tradingDate(), snap.time().phase(),
                            snap.cash(), snap.marketValue(), snap.equity())
                    .isEqualByComparingTo(computedEquity);
        }
    }
}
