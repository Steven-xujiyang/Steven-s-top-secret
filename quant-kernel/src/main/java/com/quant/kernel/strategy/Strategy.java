package com.quant.kernel.strategy;

import com.quant.kernel.market.MarketSlice;

/**
 * A strategy that examines market data and produces a {@link StrategyDecision}.
 *
 * <p>Strategies are invoked at the CLOSE phase, after the day's full bar is visible.
 * They must NOT depend on execution or accounting details — only on market data and
 * their own internal state.
 */
@FunctionalInterface
public interface Strategy {

    /**
     * Produce a trading decision given the current market context and data slice.
     *
     * @param context read-only view of current portfolio state (positions, cash, equity)
     * @param slice   market data visible at the current phase
     * @return a decision (may be {@link StrategyDecision#NOOP})
     */
    StrategyDecision decide(StrategyContext context, MarketSlice slice);
}
