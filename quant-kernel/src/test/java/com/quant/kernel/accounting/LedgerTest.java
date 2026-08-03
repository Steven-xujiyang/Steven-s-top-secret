package com.quant.kernel.accounting;

import com.quant.kernel.execution.Fill;
import com.quant.kernel.execution.FillId;
import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.OrderId;
import com.quant.kernel.portfolio.Side;
import com.quant.kernel.time.MarketTime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates ledger behavior: buy/sell fill application and equity identity.
 */
class LedgerTest {

    private static final InstrumentId AAPL = new InstrumentId("AAPL");
    private static final LocalDate D1 = LocalDate.of(2024, 1, 2);
    private static final MarketTime OPEN = MarketTime.atOpen(D1);

    @Test
    void appliesBuyAndSellFillWithoutBreakingEquityIdentity() {
        Ledger ledger = new Ledger(new BigDecimal("10000"), 2,
                RoundingMode.HALF_EVEN, "test-run");

        // Buy 50 AAPL at 100, commission 5.00
        Fill buy = new Fill(FillId.fromSequence(1), OrderId.fromSequence(1),
                AAPL, Side.BUY, 50, new BigDecimal("100"),
                new BigDecimal("5.00"), OPEN);
        ledger.applyFill(buy);

        assertThat(ledger.cash()).isEqualByComparingTo(new BigDecimal("4995.00"));
        assertThat(ledger.position(AAPL)).isEqualTo(50);
        assertThat(ledger.cumulativeCommission()).isEqualByComparingTo(new BigDecimal("5.00"));

        // Snapshot at price 100: marketValue = 50*100 = 5000, equity = 4995+5000 = 9995
        LedgerSnapshot snap1 = ledger.snapshot(OPEN, Map.of(AAPL, new BigDecimal("100")));
        assertThat(snap1.marketValue()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(snap1.equity()).isEqualByComparingTo(new BigDecimal("9995.00"));

        // Sell 20 AAPL at 110, commission 2.20
        Fill sell = new Fill(FillId.fromSequence(2), OrderId.fromSequence(2),
                AAPL, Side.SELL, 20, new BigDecimal("110"),
                new BigDecimal("2.20"), OPEN);
        ledger.applyFill(sell);

        // cash = 4995.00 + 20*110 - 2.20 = 4995.00 + 2200.00 - 2.20 = 7192.80
        assertThat(ledger.cash()).isEqualByComparingTo(new BigDecimal("7192.80"));
        assertThat(ledger.position(AAPL)).isEqualTo(30);
        assertThat(ledger.cumulativeCommission()).isEqualByComparingTo(new BigDecimal("7.20"));

        // Snapshot at price 110: marketValue = 30*110 = 3300, equity = 7192.80 + 3300 = 10492.80
        LedgerSnapshot snap2 = ledger.snapshot(OPEN, Map.of(AAPL, new BigDecimal("110")));
        assertThat(snap2.marketValue()).isEqualByComparingTo(new BigDecimal("3300.00"));
        assertThat(snap2.equity()).isEqualByComparingTo(new BigDecimal("10492.80"));
    }
}
