package com.quant.kernel.portfolio;

import com.quant.kernel.market.*;
import com.quant.kernel.strategy.*;
import com.quant.kernel.time.MarketTime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates order planning constraints: never sells more than long position.
 */
class OrderPlannerTest {

    private static final InstrumentId A = new InstrumentId("A");
    private static final LocalDate D1 = LocalDate.of(2024, 1, 2);
    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void neverSellsMoreThanLongPosition() {
        OrderPlanner planner = new OrderPlanner();

        // We hold 10 A. Target says 0% A. Should sell at most 10.
        MarketTime close = MarketTime.atClose(D1);
        TargetPortfolio target = new TargetPortfolio(close, Map.of()); // 0% everything

        // Bar: A close = 100
        DailyBar bar = new DailyBar(A, D1,
                new BigDecimal("100"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("100"), 0, T0);
        MarketSlice slice = MarketSlice.forClose(close, Map.of(A, bar));

        // Current position: 10 A, equity = 10000
        Map<InstrumentId, Long> positions = Map.of(A, 10L);
        BigDecimal equity = new BigDecimal("10000");

        List<Order> orders = planner.plan(
                StrategyDecision.of(target), slice, equity, positions);

        // Should create 1 SELL order for exactly 10 A
        assertThat(orders).hasSize(1);
        Order sell = orders.get(0);
        assertThat(sell.side()).isEqualTo(Side.SELL);
        assertThat(sell.quantity()).isEqualTo(10);
        assertThat(sell.instrument()).isEqualTo(A);
    }

    @Test
    void doesNotCreateOrdersWhenNoDelta() {
        OrderPlanner planner = new OrderPlanner();

        MarketTime close = MarketTime.atClose(D1);
        // Target 50% A. With equity=10000, close=100, targetQty = floor(5000/100) = 50
        // But we already hold 50. Delta = 0 → no orders.
        TargetPortfolio target = new TargetPortfolio(close,
                Map.of(A, new BigDecimal("0.5")));

        DailyBar bar = new DailyBar(A, D1,
                new BigDecimal("100"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("100"), 0, T0);
        MarketSlice slice = MarketSlice.forClose(close, Map.of(A, bar));

        Map<InstrumentId, Long> positions = Map.of(A, 50L);
        BigDecimal equity = new BigDecimal("10000");

        List<Order> orders = planner.plan(
                StrategyDecision.of(target), slice, equity, positions);

        assertThat(orders).isEmpty();
    }
}
