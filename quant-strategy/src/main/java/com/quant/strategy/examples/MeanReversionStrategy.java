package com.quant.strategy.examples;

import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;

import java.util.*;

/**
 * Mean reversion strategy using Bollinger Bands.
 * Buy when price touches the lower band (oversold), sell when price returns to the middle.
 */
public class MeanReversionStrategy implements Strategy {

    private int period = 20;
    private double stdMultiplier = 2.0;
    private final Map<String, List<Double>> closeHistory = new LinkedHashMap<>();

    @Override
    public void init(StrategyConfig config) {
        this.period = config.getInt("period", 20);
        this.stdMultiplier = config.getDouble("stdMultiplier", 2.0);
    }

    @Override
    public List<Signal> onBar(Bar bar, PortfolioSnapshot portfolio) {
        String symbol = bar.symbol();
        List<Double> closes = closeHistory.computeIfAbsent(symbol, k -> new ArrayList<>());
        closes.add(bar.close());

        if (closes.size() < period) return noSignal();

        double[] recent = new double[period];
        for (int i = 0; i < period; i++) {
            recent[i] = closes.get(closes.size() - period + i);
        }

        double smaB = sma(recent);
        double std = standardDeviation(recent, smaB);
        double lowerBand = smaB - stdMultiplier * std;
        double upperBand = smaB + stdMultiplier * std;
        double currentPrice = bar.close();

        // Check existing position
        var position = portfolio.positions().get(symbol);
        boolean hasPosition = position != null && !position.isFlat() && position.direction() > 0;

        // Buy signal: price at or below lower band (oversold)
        if (!hasPosition && currentPrice <= lowerBand) {
            return List.of(Signal.buy(symbol, 0.8, name(),
                String.format("Oversold: %.2f <= lower band %.2f", currentPrice, lowerBand)));
        }

        // Sell signal: price returns to middle band (take profit)
        if (hasPosition && currentPrice >= smaB) {
            return List.of(Signal.sell(symbol, name(),
                String.format("Mean reverted: %.2f >= SMA %.2f", currentPrice, smaB)));
        }

        return noSignal();
    }

    private double sma(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double standardDeviation(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }

    @Override
    public String name() {
        return "MeanReversion(" + period + "," + stdMultiplier + ")";
    }
}
