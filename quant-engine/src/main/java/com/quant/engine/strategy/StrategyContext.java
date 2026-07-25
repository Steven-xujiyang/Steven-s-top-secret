package com.quant.engine.strategy;

import com.quant.core.model.signal.Signal;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;

import java.util.Map;

/**
 * Wrapper holding a strategy instance along with its configuration.
 */
public class StrategyContext {

    private final Strategy strategy;
    private final StrategyConfig config;

    public StrategyContext(Strategy strategy, StrategyConfig config) {
        this.strategy = strategy;
        this.config = config;
    }

    /**
     * Create a context with parameters from a map.
     */
    public static StrategyContext of(Strategy strategy, Map<String, Object> params) {
        StrategyConfig config = new StrategyConfig(strategy.getClass().getSimpleName(), params);
        return new StrategyContext(strategy, config);
    }

    public Strategy getStrategy() { return strategy; }
    public StrategyConfig getConfig() { return config; }
}
