package com.quant.strategy.examples;

import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;

import java.util.*;

/**
 * Classic dual moving average crossover strategy.
 * Buy when short MA crosses above long MA, sell when short MA crosses below.
 */
public class DualMovingAverageStrategy implements Strategy {

    private int shortPeriod = 5;
    private int longPeriod = 20;
    private final Map<String, List<Double>> closeHistory = new LinkedHashMap<>();
    private final Map<String, Double> lastShortMA = new HashMap<>();
    private final Map<String, Double> lastLongMA = new HashMap<>();

    @Override
    public void init(StrategyConfig config) {
        this.shortPeriod = config.getInt("shortPeriod", 5);
        this.longPeriod = config.getInt("longPeriod", 20);
    }

    @Override
    public List<Signal> onBar(Bar bar, PortfolioSnapshot portfolio) {
        String symbol = bar.symbol();
        List<Double> closes = closeHistory.computeIfAbsent(symbol, k -> new ArrayList<>());
        closes.add(bar.close());

        if (closes.size() < longPeriod) return noSignal();

        double shortMA = sma(closes, shortPeriod);
        double longMA = sma(closes, longPeriod);

        Double prevShort = lastShortMA.get(symbol);
        Double prevLong = lastLongMA.get(symbol);

        lastShortMA.put(symbol, shortMA);
        lastLongMA.put(symbol, longMA);

        if (prevShort == null || prevLong == null) return noSignal();

        // Golden cross: short MA crosses above long MA → buy
        if (prevShort <= prevLong && shortMA > longMA) {
            return List.of(Signal.buy(symbol, 1.0, name(),
                String.format("Golden cross: MA%d(%.2f) > MA%d(%.2f)",
                    shortPeriod, shortMA, longPeriod, longMA)));
        }

        // Death cross: short MA crosses below long MA → sell
        if (prevShort >= prevLong && shortMA < longMA) {
            return List.of(Signal.sell(symbol, name(),
                String.format("Death cross: MA%d(%.2f) < MA%d(%.2f)",
                    shortPeriod, shortMA, longPeriod, longMA)));
        }

        return noSignal();
    }

    private double sma(List<Double> values, int period) {
        int size = values.size();
        double sum = 0;
        for (int i = size - period; i < size; i++) sum += values.get(i);
        return sum / period;
    }

    @Override
    public String name() {
        return "DualMA(" + shortPeriod + "," + longPeriod + ")";
    }
}
