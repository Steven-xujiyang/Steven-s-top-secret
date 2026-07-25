package com.quant.optimizer.executor;

import com.quant.engine.BacktestEngine;
import com.quant.engine.loop.BacktestResult;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Executes backtest runs in parallel for parameter optimization.
 * Wraps BacktestEngine and StrategySupplier for repeated evaluation.
 */
public class OptimizationExecutor {

    private static final Logger log = LoggerFactory.getLogger(OptimizationExecutor.class);

    private final BacktestEngine engine;
    private final Supplier<Strategy> strategySupplier;
    private final Set<String> symbols;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ExecutorService threadPool;

    public OptimizationExecutor(BacktestEngine engine, Supplier<Strategy> strategySupplier,
                                 Set<String> symbols, LocalDate startDate, LocalDate endDate,
                                 int parallelism) {
        this.engine = engine;
        this.strategySupplier = strategySupplier;
        this.symbols = symbols;
        this.startDate = startDate;
        this.endDate = endDate;
        this.threadPool = Executors.newFixedThreadPool(Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Execute all parameter combinations in parallel.
     */
    public List<BacktestResult> executeAll(List<Map<String, Object>> paramList) {
        List<Future<BacktestResult>> futures = new ArrayList<>();

        for (int i = 0; i < paramList.size(); i++) {
            final Map<String, Object> params = paramList.get(i);
            final int index = i;
            futures.add(threadPool.submit(() -> {
                log.debug("Running backtest {}/{} with params {}", index + 1, paramList.size(), params);
                Strategy strategy = strategySupplier.get();
                strategy.init(new StrategyConfig(strategy.name(), params));
                return engine.run(strategy, symbols, startDate, endDate);
            }));
        }

        List<BacktestResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get(5, TimeUnit.MINUTES));
            } catch (TimeoutException e) {
                log.error("Backtest {} timed out", i);
                results.add(BacktestResult.failure("timeout", "Evaluation timed out"));
            } catch (Exception e) {
                log.error("Backtest {} failed: {}", i, e.getMessage());
                results.add(BacktestResult.failure("error", e.getMessage()));
            }
        }
        return results;
    }

    /**
     * Shutdown the thread pool.
     */
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
