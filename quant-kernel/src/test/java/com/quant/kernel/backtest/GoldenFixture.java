package com.quant.kernel.backtest;

import com.quant.kernel.execution.FixedRateCommissionModel;
import com.quant.kernel.market.*;
import com.quant.kernel.strategy.ScheduledTargetStrategy;
import com.quant.kernel.time.MarketPhase;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Shared golden fixture: two assets, three trading days, hand-verifiable.
 *
 * <h3>Golden ledger (hand-computed)</h3>
 * <pre>
 * initial cash = 10000.00, commission rate = 0.001, scale = 2, HALF_EVEN
 *
 * D1 OPEN:  cash=10000.00 mv=0      eq=10000.00 comm=0     pos=[]
 * D1 CLOSE: cash=10000.00 mv=0      eq=10000.00 comm=0     pos=[]   target A=50%
 * D2 OPEN:  cash= 4494.50 mv=5500.00 eq= 9994.50 comm=5.50  pos=[A=50]
 * D2 CLOSE: cash= 4494.50 mv=5400.00 eq= 9894.50 comm=5.50  pos=[A=50] target A=0%,B=50%
 * D3 OPEN:  cash= 4633.95 mv=5200.00 eq= 9833.95 comm=16.05 pos=[B=100]
 * D3 CLOSE: cash= 4633.95 mv=5100.00 eq= 9733.95 comm=16.05 pos=[B=100]
 * </pre>
 */
public final class GoldenFixture {

    public static final InstrumentId A = new InstrumentId("A");
    public static final InstrumentId B = new InstrumentId("B");

    public static final LocalDate D1 = LocalDate.of(2024, 1, 2);
    public static final LocalDate D2 = LocalDate.of(2024, 1, 3);
    public static final LocalDate D3 = LocalDate.of(2024, 1, 4);

    public static final BigDecimal INITIAL_CASH = new BigDecimal("10000.00");
    public static final BigDecimal COMMISSION_RATE = new BigDecimal("0.001");
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private static final Instant AVAIL = Instant.parse("2024-01-01T00:00:00Z");

    /**
     * Golden bars: 2 instruments × 3 days.
     */
    public static List<DailyBar> goldenBars() {
        return List.of(
                // D1
                new DailyBar(A, D1, p("100"), p("100"), p("100"), p("100"), 0, AVAIL),
                new DailyBar(B, D1, p("50"), p("50"), p("50"), p("50"), 0, AVAIL),
                // D2
                new DailyBar(A, D2, p("110"), p("110"), p("108"), p("108"), 0, AVAIL),
                new DailyBar(B, D2, p("48"), p("49"), p("48"), p("49"), 0, AVAIL),
                // D3
                new DailyBar(A, D3, p("107"), p("109"), p("107"), p("109"), 0, AVAIL),
                new DailyBar(B, D3, p("52"), p("52"), p("51"), p("51"), 0, AVAIL)
        );
    }

    /**
     * Golden strategy schedule.
     */
    public static ScheduledTargetStrategy goldenStrategy() {
        return new ScheduledTargetStrategy(Map.of(
                MarketTime.atClose(D1), Map.of(A, new BigDecimal("0.50")),
                MarketTime.atClose(D2), Map.of(
                        A, BigDecimal.ZERO,
                        B, new BigDecimal("0.50"))
        ));
    }

    /**
     * Golden RunSpec.
     */
    public static RunSpec goldenRunSpec(String runId) {
        MarketDataFeed feed = new MarketDataFeed(goldenBars());
        return new RunSpec(runId, INITIAL_CASH, COMMISSION_RATE,
                MONEY_SCALE, ROUNDING_MODE,
                List.of(A, B), D1, D3, feed, goldenStrategy());
    }

    /**
     * Expected daily snapshot values as [date, phase, cash, marketValue, equity, cumulativeCommission]
     */
    public static Object[][] expectedSnapshots() {
        return new Object[][]{
                // {date, phase, cash, mv, eq, cumComm, positions}
                {D1, MarketPhase.OPEN,  "10000.00", "0",     "10000.00", "0",    Map.of()},
                {D1, MarketPhase.CLOSE, "10000.00", "0",     "10000.00", "0",    Map.of()},
                {D2, MarketPhase.OPEN,  "4494.50",  "5500.00", "9994.50", "5.50", Map.of(A, 50L)},
                {D2, MarketPhase.CLOSE, "4494.50",  "5400.00", "9894.50", "5.50", Map.of(A, 50L)},
                {D3, MarketPhase.OPEN,  "4633.95",  "5200.00", "9833.95", "16.05", Map.of(B, 100L)},
                {D3, MarketPhase.CLOSE, "4633.95",  "5100.00", "9733.95", "16.05", Map.of(B, 100L)},
        };
    }

    private static BigDecimal p(String s) {
        return new BigDecimal(s);
    }
}
