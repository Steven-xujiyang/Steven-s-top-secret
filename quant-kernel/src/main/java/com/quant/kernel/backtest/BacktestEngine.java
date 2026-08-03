package com.quant.kernel.backtest;

import com.quant.kernel.accounting.Ledger;
import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.execution.*;
import com.quant.kernel.market.*;
import com.quant.kernel.portfolio.*;
import com.quant.kernel.strategy.*;
import com.quant.kernel.time.MarketPhase;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Deterministic daily-frequency backtesting engine.
 *
 * <p>Event ordering per trading day (fixed, non-negotiable):
 *
 * <pre>
 * OPEN
 *   1. publish open-only MarketSlice
 *   2. execute previously created NEXT_BAR_OPEN orders
 *   3. apply fills to ledger
 *   4. value at open, record snapshot
 *
 * CLOSE
 *   5. publish MarketSlice containing today's close
 *   6. value at close
 *   7. call strategy
 *   8. convert target weights to orders (waiting for next OPEN)
 *   9. record end-of-day snapshot
 * </pre>
 *
 * <p>The engine is deterministic: given the same {@link RunSpec},
 * it produces identical outputs every time. There is no use of
 * {@code Instant.now()}, {@code UUID.randomUUID()}, or
 * {@code HashMap} iteration.
 */
public class BacktestEngine {

    /**
     * Runs a backtest from the given specification.
     *
     * @param spec fully-parameterized run specification
     * @return the complete result with all artifacts in stable order
     */
    public BacktestResult run(RunSpec spec) {
        Objects.requireNonNull(spec, "spec must not be null");

        List<Order> allOrders = new ArrayList<>();
        List<Fill> allFills = new ArrayList<>();
        List<LedgerSnapshot> dailySnapshots = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();

        // ——— wiring ———
        CommissionModel commissionModel = new FixedRateCommissionModel(
                spec.commissionRate(), spec.moneyScale());
        ExecutionModel executionModel = new NextBarOpenExecutionModel(commissionModel);
        OrderPlanner orderPlanner = new OrderPlanner();

        Ledger ledger = new Ledger(spec.initialCash(), spec.moneyScale(),
                spec.roundingMode(), spec.runId());

        // Pending orders created at CLOSE, executed at next OPEN
        List<Order> pendingOrders = new ArrayList<>();

        NavigableSet<LocalDate> tradingDates = spec.dataFeed().tradingDates();
        LocalDate date = spec.startDate();

        // ——— main loop ———
        while (date != null && !date.isAfter(spec.endDate())) {

            Map<InstrumentId, DailyBar> dayBars = spec.dataFeed().barsFor(date);

            if (dayBars.isEmpty()) {
                // No data for this date — skip to next available trading date
                date = spec.dataFeed().nextTradingDate(date);
                continue;
            }

            // ========== OPEN ==========
            MarketTime openTime = MarketTime.atOpen(date);
            MarketSlice openSlice = MarketSlice.forOpen(openTime, dayBars);

            // Execute pending NEXT_BAR_OPEN orders at today's open
            if (!pendingOrders.isEmpty()) {
                ExecutionModel.ExecutionResult execResult =
                        executionModel.execute(pendingOrders, openSlice, ledger);
                allFills.addAll(execResult.fills());
                allWarnings.addAll(execResult.warnings());

                // Apply fills to ledger
                for (Fill fill : execResult.fills()) {
                    ledger.applyFill(fill);
                }
            }

            // Record open snapshot
            Map<InstrumentId, BigDecimal> openPrices = openPrices(dayBars);
            LedgerSnapshot openSnapshot = ledger.snapshot(openTime, openPrices);
            dailySnapshots.add(openSnapshot);

            // ========== CLOSE ==========
            MarketTime closeTime = MarketTime.atClose(date);
            MarketSlice closeSlice = MarketSlice.forClose(closeTime, dayBars);

            // Value at close
            Map<InstrumentId, BigDecimal> closePrices = closePrices(dayBars);
            LedgerSnapshot closeSnapshot = ledger.snapshot(closeTime, closePrices);
            dailySnapshots.add(closeSnapshot);

            // Call strategy
            StrategyContext context = new StrategyContext(
                    ledger.cash(), ledger.positions(), closeSnapshot.equity());
            StrategyDecision decision = spec.strategy().decide(context, closeSlice);

            // Convert target weights to orders
            if (!decision.isNoop()) {
                List<Order> newOrders = orderPlanner.plan(
                        decision, closeSlice, closeSnapshot.equity(),
                        ledger.positions());
                allOrders.addAll(newOrders);
                pendingOrders = newOrders; // Replace pending — only latest targets
            } else {
                pendingOrders = List.of();
            }

            // Advance to next trading date
            date = spec.dataFeed().nextTradingDate(date);
        }

        return new BacktestResult(
                spec.runId(),
                allOrders,
                allFills,
                dailySnapshots,
                ledger.cumulativeCommission(),
                allWarnings);
    }

    private static Map<InstrumentId, BigDecimal> openPrices(
            Map<InstrumentId, DailyBar> bars) {
        Map<InstrumentId, BigDecimal> prices = new TreeMap<>();
        for (Map.Entry<InstrumentId, DailyBar> entry : bars.entrySet()) {
            prices.put(entry.getKey(), entry.getValue().open());
        }
        return prices;
    }

    private static Map<InstrumentId, BigDecimal> closePrices(
            Map<InstrumentId, DailyBar> bars) {
        Map<InstrumentId, BigDecimal> prices = new TreeMap<>();
        for (Map.Entry<InstrumentId, DailyBar> entry : bars.entrySet()) {
            prices.put(entry.getKey(), entry.getValue().close());
        }
        return prices;
    }
}
