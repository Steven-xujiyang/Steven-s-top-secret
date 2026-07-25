package com.quant.strategy.factor.momentum;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;
import com.quant.strategy.factor.Factor;

import java.time.Instant;
import java.util.List;

/**
 * MACD (Moving Average Convergence Divergence) factor.
 * MACD = EMA(fast) - EMA(slow)
 * Signal = EMA(signalPeriod) of MACD
 * Returns: MACD - Signal (histogram value, >0 bullish, <0 bearish)
 */
public class MACDFactor implements Factor {

    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;

    public MACDFactor(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    public MACDFactor() {
        this(12, 26, 9);
    }

    @Override
    public FactorValue calculate(String symbol, List<Bar> bars) {
        if (bars.size() < slowPeriod + signalPeriod) {
            return FactorValue.raw("MACD", symbol, Instant.now(), Double.NaN);
        }

        double[] closes = bars.stream().mapToDouble(Bar::close).toArray();
        double fastEma = emaAt(closes, closes.length - 1, fastPeriod);
        double slowEma = emaAt(closes, closes.length - 1, slowPeriod);
        double macd = fastEma - slowEma;

        // Compute signal line (EMA of MACD values)
        double[] macdValues = new double[signalPeriod];
        for (int i = 0; i < signalPeriod; i++) {
            int idx = closes.length - signalPeriod + i;
            macdValues[i] = emaAt(closes, idx, fastPeriod) - emaAt(closes, idx, slowPeriod);
        }
        double signal = emaOf(macdValues);

        return FactorValue.raw("MACD", symbol,
            bars.get(bars.size() - 1).timestamp(), macd - signal);
    }

    private double emaAt(double[] prices, int index, int period) {
        if (index < period) return prices[index];
        double multiplier = 2.0 / (period + 1);
        double ema = prices[index - period];
        for (int i = index - period + 1; i <= index; i++) {
            ema = (prices[i] - ema) * multiplier + ema;
        }
        return ema;
    }

    private double emaOf(double[] values) {
        double multiplier = 2.0 / (signalPeriod + 1);
        double ema = values[0];
        for (int i = 1; i < values.length; i++) {
            ema = (values[i] - ema) * multiplier + ema;
        }
        return ema;
    }

    @Override
    public String name() { return "MACD"; }

    @Override
    public FactorCategory category() { return FactorCategory.MOMENTUM; }
}
