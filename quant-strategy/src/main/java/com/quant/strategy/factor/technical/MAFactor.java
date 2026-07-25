package com.quant.strategy.factor.technical;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;
import com.quant.strategy.factor.Factor;

import java.time.Instant;
import java.util.List;

/**
 * Moving Average factor: computes SMA/EMA and returns price relative to MA.
 * Positive = price above MA (bullish), Negative = price below MA (bearish).
 */
public class MAFactor implements Factor {

    private final int period;
    private final MAType type;

    public enum MAType { SMA, EMA }

    public MAFactor(int period, MAType type) {
        this.period = period;
        this.type = type;
    }

    public MAFactor(int period) {
        this(period, MAType.SMA);
    }

    @Override
    public FactorValue calculate(String symbol, List<Bar> bars) {
        if (bars.size() < period) {
            return FactorValue.raw("MA", symbol, Instant.now(), Double.NaN);
        }

        double[] closes = bars.stream().mapToDouble(Bar::close).toArray();
        double ma = switch (type) {
            case SMA -> sma(closes, closes.length - period, closes.length);
            case EMA -> ema(closes, closes.length);
        };

        double currentClose = closes[closes.length - 1];
        double value = (currentClose - ma) / ma; // normalized deviation from MA

        return FactorValue.raw("MA" + period, symbol,
            bars.get(bars.size() - 1).timestamp(), value);
    }

    private double sma(double[] values, int start, int end) {
        double sum = 0;
        for (int i = start; i < end; i++) sum += values[i];
        return sum / (end - start);
    }

    private double ema(double[] values, int index) {
        if (index < period) return values[index];
        double multiplier = 2.0 / (period + 1);
        double ema = values[index - period];
        for (int i = index - period + 1; i <= index; i++) {
            ema = (values[i] - ema) * multiplier + ema;
        }
        return ema;
    }

    @Override
    public String name() { return type + "_" + period; }

    @Override
    public FactorCategory category() { return FactorCategory.TECHNICAL; }
}
