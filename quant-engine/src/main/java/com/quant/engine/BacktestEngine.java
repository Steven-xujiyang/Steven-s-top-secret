package com.quant.engine;

import com.quant.common.enums.BarFrequency;
import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.data.cleaning.DataCleaner;
import com.quant.data.provider.CsvDataProvider;
import com.quant.data.provider.MarketDataProvider;
import com.quant.engine.broker.SimpleBroker;
import com.quant.engine.execution.ExecutionSimulator;
import com.quant.engine.execution.OrderManager;
import com.quant.engine.loop.BacktestResult;
import com.quant.engine.loop.EventLoop;
import com.quant.engine.loop.EventLoopConfig;
import com.quant.engine.portfolio.PortfolioManager;
import com.quant.engine.strategy.StrategyRunner;
import com.quant.strategy.api.Strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

/**
 * Facade for the entire backtesting engine.
 * Wires together all components and provides a simple API:
 * {@code engine.run(strategy, symbols, startDate, endDate) → BacktestResult}
 */
public class BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);

    private final MarketDataProvider dataProvider;
    private final SimpleBroker broker;
    private final DataCleaner dataCleaner;
    private final double initialCapital;
    private final BarFrequency frequency;

    private BacktestEngine(Builder builder) {
        this.dataProvider = builder.dataProvider;
        this.broker = builder.broker != null ? builder.broker : SimpleBroker.aShare();
        this.dataCleaner = builder.dataCleaner != null ? builder.dataCleaner : DataCleaner.standardPipeline();
        this.initialCapital = builder.initialCapital;
        this.frequency = builder.frequency;
    }

    /**
     * Run a backtest for a strategy on a set of symbols over a date range.
     *
     * @param strategy  the trading strategy
     * @param symbols   set of symbols to trade
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return BacktestResult with fills, portfolio history, and metrics
     */
    public BacktestResult run(Strategy strategy, Set<String> symbols,
                               LocalDate startDate, LocalDate endDate) {
        log.info("Starting backtest: strategy={}, symbols={}, period=[{}, {}], capital={}",
            strategy.getClass().getSimpleName(), symbols.size(), startDate, endDate, initialCapital);

        try {
            // Fetch and clean data
            List<Bar[]> barData = loadBarData(symbols, startDate, endDate, frequency);
            if (barData.isEmpty()) {
                return BacktestResult.failure(strategy.getClass().getSimpleName(),
                    "No data available for the given symbols and date range");
            }

            // Wire components
            PortfolioManager portfolioManager = new PortfolioManager(initialCapital);
            OrderManager orderManager = new OrderManager();
            ExecutionSimulator executionSim = new ExecutionSimulator(broker);
            StrategyRunner runner = new StrategyRunner(strategy);

            // Create and run event loop
            EventLoopConfig loopConfig = EventLoopConfig.forDateRange(startDate, endDate);
            EventLoop eventLoop = new EventLoop(loopConfig, dataProvider, runner,
                orderManager, executionSim, portfolioManager);

            eventLoop.run(barData);

            // Build result
            BacktestResult.BacktestMetrics metrics = new BacktestResult.BacktestMetrics();
            metrics.setWallTimeMs(eventLoop.getMetrics().wallTimeMs());
            metrics.setBarCount(eventLoop.getMetrics().getBarCount());
            metrics.setEventCount(eventLoop.getMetrics().getEventCount());
            metrics.setTradeCount(eventLoop.getMetrics().getTradeCount());

            List<PortfolioSnapshot> history = new ArrayList<>();
            for (int i = 0; i < eventLoop.getRecorder().getPortfolioHistory().size(); i++) {
                history.add(eventLoop.getRecorder().getPortfolioHistory().get(i));
            }

            return BacktestResult.success(
                strategy.getClass().getSimpleName(),
                eventLoop.getRecorder().getFills(),
                history,
                metrics
            );
        } catch (Exception e) {
            log.error("Backtest failed: {}", e.getMessage(), e);
            return BacktestResult.failure(strategy.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Load and clean bar data, grouping by timestamp for event-loop processing.
     */
    private List<Bar[]> loadBarData(Set<String> symbols, LocalDate start, LocalDate end,
                                     BarFrequency frequency) {
        // Load data for each symbol
        Map<String, List<Bar>> symbolBars = new LinkedHashMap<>();
        for (String symbol : symbols) {
            List<Bar> bars = dataProvider.fetchBars(symbol, start, end, frequency);
            if (!bars.isEmpty()) {
                symbolBars.put(symbol, dataCleaner.clean(bars));
            }
        }

        if (symbolBars.isEmpty()) return List.of();

        // Group bars by timestamp across all symbols
        Map<java.time.Instant, List<Bar>> byTime = new TreeMap<>();
        for (List<Bar> bars : symbolBars.values()) {
            for (Bar bar : bars) {
                byTime.computeIfAbsent(bar.timestamp(), k -> new ArrayList<>()).add(bar);
            }
        }

        // Convert to array form for event loop
        return byTime.values().stream()
            .map(list -> list.toArray(new Bar[0]))
            .toList();
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for BacktestEngine.
     */
    public static class Builder {
        private MarketDataProvider dataProvider;
        private SimpleBroker broker;
        private DataCleaner dataCleaner;
        private double initialCapital = 1_000_000;
        private BarFrequency frequency = BarFrequency.D;

        public Builder dataProvider(MarketDataProvider provider) {
            this.dataProvider = provider;
            return this;
        }

        public Builder csvData(java.nio.file.Path csvDir, com.quant.common.enums.Market market) {
            this.dataProvider = new CsvDataProvider(csvDir, market);
            return this;
        }

        public Builder broker(SimpleBroker broker) {
            this.broker = broker;
            return this;
        }

        public Builder dataCleaner(DataCleaner cleaner) {
            this.dataCleaner = cleaner;
            return this;
        }

        public Builder initialCapital(double capital) {
            this.initialCapital = capital;
            return this;
        }

        public Builder frequency(BarFrequency frequency) {
            this.frequency = frequency;
            return this;
        }

        public BacktestEngine build() {
            if (dataProvider == null) {
                throw new IllegalStateException("DataProvider is required");
            }
            return new BacktestEngine(this);
        }
    }
}
