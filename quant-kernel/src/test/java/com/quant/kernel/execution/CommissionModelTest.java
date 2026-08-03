package com.quant.kernel.execution;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Side;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates commission calculation:
 * <ul>
 *   <li>Commission only when Fill is generated (model itself doesn't decide "when")</li>
 *   <li>Rounds exactly once using HALF_EVEN</li>
 * </ul>
 */
class CommissionModelTest {

    private final InstrumentId AAPL = new InstrumentId("AAPL");
    private final FixedRateCommissionModel model =
            new FixedRateCommissionModel(new BigDecimal("0.001"), 2);

    @Test
    void chargesOnlyOnFillAndRoundsOnce() {
        // turnover = 100 * 50 = 5000
        // commission = 5000 * 0.001 = 5.000 → rounded to 5.00
        BigDecimal comm = model.calculate(AAPL, Side.BUY, 100, new BigDecimal("50"));
        assertThat(comm).isEqualByComparingTo(new BigDecimal("5.00"));
        // verify scale: exactly 2 decimal places
        assertThat(comm.scale()).isEqualTo(2);

        // turnover = 333 * 99.99 = 33296.67
        // commission = 33296.67 * 0.001 = 33.29667 → HALF_EVEN to scale 2 = 33.30
        BigDecimal comm2 = model.calculate(AAPL, Side.BUY, 333, new BigDecimal("99.99"));
        assertThat(comm2).isEqualByComparingTo(new BigDecimal("33.30"));
        assertThat(comm2.scale()).isEqualTo(2);

        // turnover = 1 * 1.50 = 1.50
        // commission = 1.50 * 0.001 = 0.0015 → HALF_EVEN to scale 2 = 0.00
        BigDecimal comm3 = model.calculate(AAPL, Side.SELL, 1, new BigDecimal("1.50"));
        assertThat(comm3).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(comm3.scale()).isEqualTo(2);
    }

    @Test
    void zeroCommissionForZeroRate() {
        FixedRateCommissionModel zeroModel =
                new FixedRateCommissionModel(BigDecimal.ZERO, 2);
        BigDecimal comm = zeroModel.calculate(AAPL, Side.BUY, 100, new BigDecimal("50"));
        assertThat(comm).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
