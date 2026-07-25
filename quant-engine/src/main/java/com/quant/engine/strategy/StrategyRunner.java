package com.quant.engine.strategy;

import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;
import com.quant.strategy.api.Strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps strategy execution: calls strategy.onBar() and collects signals.
 */
public class StrategyRunner {

    private static final Logger log = LoggerFactory.getLogger(StrategyRunner.class);

    private final Strategy strategy;
    private PortfolioSnapshot latestPortfolio;

    public StrategyRunner(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Run strategy evaluation on new bars.
     *
     * @param bars      current bar data
     * @param portfolio latest portfolio snapshot
     * @return list of signals generated (may be empty)
     */
    public List<Signal> onBars(List<Bar> bars, PortfolioSnapshot portfolio) {
        this.latestPortfolio = portfolio;
        List<Signal> allSignals = new ArrayList<>();

        for (Bar bar : bars) {
            try {
                List<Signal> signals = strategy.onBar(bar, portfolio);
                if (signals != null && !signals.isEmpty()) {
                    allSignals.addAll(signals);
                }
            } catch (Exception e) {
                log.error("Strategy {} failed on bar {}: {}",
                    strategy.getClass().getSimpleName(), bar.timestamp(), e.getMessage(), e);
            }
        }
        return allSignals;
    }

    /**
     * Call strategy's onComplete lifecycle method.
     */
    public void complete() {
        try {
            strategy.onComplete();
        } catch (Exception e) {
            log.error("Strategy onComplete failed: {}", e.getMessage(), e);
        }
    }

    public Strategy getStrategy() { return strategy; }
    public String strategyName() { return strategy.getClass().getSimpleName(); }
}
