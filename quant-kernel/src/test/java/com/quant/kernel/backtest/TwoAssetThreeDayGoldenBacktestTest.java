package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.accounting.Position;
import com.quant.kernel.execution.Fill;
import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Side;
import com.quant.kernel.time.MarketPhase;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden test: two-asset, three-day fixture with hand-calculated expected values.
 *
 * <p>ALL expected values are hand-computed in {@link GoldenFixture} — NOT generated
 * by calling the production commission/ledger implementation. This is the
 * single most important test in the suite.
 */
class TwoAssetThreeDayGoldenBacktestTest {

    @Test
    void matchesHandCalculatedLedger() {
        RunSpec spec = GoldenFixture.goldenRunSpec("golden-test");
        BacktestEngine engine = new BacktestEngine();
        BacktestResult result = engine.run(spec);

        // ——— D1 has no fills ———
        long d1Fills = result.fills().stream()
                .filter(f -> f.filledAt().tradingDate().equals(GoldenFixture.D1))
                .count();
        assertThat(d1Fills).as("D1 must have zero fills").isZero();

        // ——— The first fill is at D2 OPEN, price=110 (NOT D1 close=100) ———
        assertThat(result.fills()).isNotEmpty();
        Fill firstFill = result.fills().get(0);
        assertThat(firstFill.filledAt().tradingDate())
                .as("First fill date").isEqualTo(GoldenFixture.D2);
        assertThat(firstFill.filledAt().phase())
                .as("First fill phase").isEqualTo(MarketPhase.OPEN);
        assertThat(firstFill.price())
                .as("First fill must be D2 open=110, NOT D1 close=100")
                .isEqualByComparingTo(new BigDecimal("110"));
        assertThat(firstFill.instrument()).isEqualTo(GoldenFixture.A);
        assertThat(firstFill.side()).isEqualTo(Side.BUY);
        assertThat(firstFill.quantity()).isEqualTo(50);
        assertThat(firstFill.commission())
                .isEqualByComparingTo(new BigDecimal("5.50"));

        // ——— D2 orders execute at D3 OPEN ———
        Fill d3FillA = result.fills().stream()
                .filter(f -> f.instrument().equals(GoldenFixture.A)
                        && f.filledAt().tradingDate().equals(GoldenFixture.D3))
                .findFirst().orElseThrow();
        assertThat(d3FillA.side()).isEqualTo(Side.SELL);
        assertThat(d3FillA.quantity()).isEqualTo(50);
        assertThat(d3FillA.price()).isEqualByComparingTo(new BigDecimal("107"));
        assertThat(d3FillA.commission()).isEqualByComparingTo(new BigDecimal("5.35"));

        Fill d3FillB = result.fills().stream()
                .filter(f -> f.instrument().equals(GoldenFixture.B)
                        && f.filledAt().tradingDate().equals(GoldenFixture.D3))
                .findFirst().orElseThrow();
        assertThat(d3FillB.side()).isEqualTo(Side.BUY);
        assertThat(d3FillB.quantity()).isEqualTo(100);
        assertThat(d3FillB.price()).isEqualByComparingTo(new BigDecimal("52"));
        assertThat(d3FillB.commission()).isEqualByComparingTo(new BigDecimal("5.20"));

        // ——— Daily snapshots: field-by-field match against hand-computed table ———
        Object[][] expected = GoldenFixture.expectedSnapshots();
        assertThat(result.dailySnapshots()).hasSize(expected.length);

        for (int i = 0; i < expected.length; i++) {
            LocalDate expDate = (LocalDate) expected[i][0];
            MarketPhase expPhase = (MarketPhase) expected[i][1];
            String expCash = (String) expected[i][2];
            String expMv = (String) expected[i][3];
            String expEq = (String) expected[i][4];
            String expComm = (String) expected[i][5];

            @SuppressWarnings("unchecked")
            Map<InstrumentId, Long> expPos = (Map<InstrumentId, Long>) expected[i][6];

            LedgerSnapshot snap = result.dailySnapshots().get(i);

            assertThat(snap.time().tradingDate())
                    .as("snapshot[%d] date", i).isEqualTo(expDate);
            assertThat(snap.time().phase())
                    .as("snapshot[%d] phase", i).isEqualTo(expPhase);
            assertThat(snap.cash())
                    .as("snapshot[%d] cash", i).isEqualByComparingTo(new BigDecimal(expCash));
            assertThat(snap.marketValue())
                    .as("snapshot[%d] mv", i).isEqualByComparingTo(new BigDecimal(expMv));
            assertThat(snap.equity())
                    .as("snapshot[%d] eq", i).isEqualByComparingTo(new BigDecimal(expEq));
            assertThat(snap.cumulativeCommission())
                    .as("snapshot[%d] comm", i).isEqualByComparingTo(new BigDecimal(expComm));

            assertThat(snap.positions()).hasSize(expPos.size());
            for (Map.Entry<InstrumentId, Long> entry : expPos.entrySet()) {
                Position pos = snap.positions().get(entry.getKey());
                assertThat(pos)
                        .as("snapshot[%d] position %s", i, entry.getKey())
                        .isNotNull();
                assertThat(pos.quantity())
                        .as("snapshot[%d] position %s qty", i, entry.getKey())
                        .isEqualTo(entry.getValue());
            }
        }

        // ——— Orders and fills are in stable order ———
        // (implied by the deterministic canonical output)

        // ——— No commission on days with no fills ———
        // D1 has no fills → cumulativeCommission = 0 for both D1 snapshots
        assertThat(result.dailySnapshots().get(0).cumulativeCommission())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dailySnapshots().get(1).cumulativeCommission())
                .isEqualByComparingTo(BigDecimal.ZERO);

        // ——— Total commission matches ———
        assertThat(result.totalCommission())
                .isEqualByComparingTo(new BigDecimal("16.05"));
    }
}
