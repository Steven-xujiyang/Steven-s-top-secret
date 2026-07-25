package com.quant.strategy.factor.momentum;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;
import com.quant.strategy.factor.Factor;

import java.time.Instant;
import java.util.List;

/**
 * Relative Strength Index (RSI) factor.
 * RSI = 100 - 100 / (1 + RS), where RS = avgGain / avgLoss over the period.
 */
public class RSIFactor implements Factor {

    private final int period;

    public RSIFactor(int period) {
        this.period = period;
    }

    public RSIFactor() {
        this(14);
    }

    @Override
    public FactorValue calculate(String symbol, List<Bar> bars) {
        if (bars.size() < period + 1) {
            return FactorValue.raw("RSI", symbol, Instant.now(), Double.NaN);
        }

        double[] changes = new double[bars.size() - 1];
        for (int i = 1; i < bars.size(); i++) {
            changes[i - 1] = bars.get(i).close() - bars.get(i - 1).close();
        }

        // Initial average gain/loss
        double avgGain = 0, avgLoss = 0;
        for (int i = changes.length - period; i < changes.length; i++) {
            if (changes[i] > 0) avgGain += changes[i];
            else avgLoss -= changes[i];
        }
        avgGain /= period;
        avgLoss /= period;

        double rsi = (avgLoss == 0) ? 100 : 100.0 - 100.0 / (1.0 + avgGain / avgLoss);

        return FactorValue.raw("RSI", symbol,
            bars.get(bars.size() - 1).timestamp(), rsi);
    }

    @Override
    public String name() { return "RSI"; }

    @Override
    public FactorCategory category() { return FactorCategory.MOMENTUM; }
}
