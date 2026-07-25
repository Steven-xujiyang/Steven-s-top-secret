package com.quant.strategy.factor.momentum;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;
import com.quant.strategy.factor.Factor;

import java.time.Instant;
import java.util.List;

/**
 * Price momentum factor: (close_now - close_N_periods_ago) / close_N_periods_ago.
 */
public class MomentumFactor implements Factor {

    private final int period;

    public MomentumFactor(int period) {
        this.period = period;
    }

    public MomentumFactor() {
        this(20);
    }

    @Override
    public FactorValue calculate(String symbol, List<Bar> bars) {
        if (bars.size() < period + 1) {
            return FactorValue.raw("Momentum", symbol, Instant.now(), Double.NaN);
        }

        double currentClose = bars.get(bars.size() - 1).close();
        double pastClose = bars.get(bars.size() - 1 - period).close();

        double momentum = (pastClose > 0) ? (currentClose - pastClose) / pastClose : 0;

        return FactorValue.raw("Momentum", symbol,
            bars.get(bars.size() - 1).timestamp(), momentum);
    }

    @Override
    public String name() { return "Momentum"; }

    @Override
    public FactorCategory category() { return FactorCategory.MOMENTUM; }
}
