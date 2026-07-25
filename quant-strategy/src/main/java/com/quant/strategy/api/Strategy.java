package com.quant.strategy.api;

import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;

import java.util.Collections;
import java.util.List;

/**
 * Core strategy interface.
 * Strategies are stateful: init() is called once, onBar() is called for each bar,
 * and onComplete() is called when the backtest ends.
 */
public interface Strategy {

    /**
     * Initialize the strategy with configuration.
     * Called once before the backtest loop begins.
     *
     * @param config strategy configuration (parameters)
     */
    void init(StrategyConfig config);

    /**
     * Called for each new bar (once per bar per symbol during the backtest).
     * The strategy should update internal state (indicators, factor values)
     * and may return trading signals.
     *
     * @param bar       the current bar data (OHLCV)
     * @param portfolio current portfolio snapshot
     * @return list of signals (empty list if no action)
     */
    List<Signal> onBar(Bar bar, PortfolioSnapshot portfolio);

    /**
     * Called when the backtest completes.
     * Cleanup, final calculations, or logging.
     */
    default void onComplete() {
    }

    /**
     * Display name for this strategy.
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Convenience: return empty signals.
     */
    default List<Signal> noSignal() {
        return Collections.emptyList();
    }
}
