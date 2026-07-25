package com.quant.strategy.composite;

import com.quant.core.model.market.Bar;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;
import com.quant.strategy.factor.Factor;

import java.util.*;

/**
 * Long-short strategy based on multi-factor ranking.
 * Ranks stocks by composite factor score, goes long the top N, short the bottom N.
 */
public class FactorRankingStrategy implements Strategy {

    private final FactorScore factorScore;
    private int topN = 10;
    private int bottomN = 5;
    private final Map<String, List<Bar>> barHistory = new LinkedHashMap<>();
    private int minBars;

    public FactorRankingStrategy(FactorScore factorScore) {
        this.factorScore = factorScore;
    }

    @Override
    public void init(StrategyConfig config) {
        this.topN = config.getInt("topN", 10);
        this.bottomN = config.getInt("bottomN", 5);
        this.minBars = config.getInt("minBars", 60);
    }

    @Override
    public List<Signal> onBar(Bar bar, PortfolioSnapshot portfolio) {
        barHistory.computeIfAbsent(bar.symbol(), k -> new ArrayList<>()).add(bar);

        // Need enough bars for all symbols
        if (barHistory.values().stream().anyMatch(bars -> bars.size() < minBars)) {
            return noSignal();
        }

        // Compute scores
        List<FactorScore.ScoredSymbol> ranked = factorScore.scoreSymbols(barHistory);
        if (ranked.size() < topN) return noSignal();

        List<Signal> signals = new ArrayList<>();

        // Long top N
        for (int i = 0; i < Math.min(topN, ranked.size()); i++) {
            FactorScore.ScoredSymbol entry = ranked.get(i);
            double strength = 0.8 * (1.0 - (double) i / topN); // decreasing strength
            signals.add(Signal.buy(entry.symbol(), strength, name(),
                "Top-" + (i + 1) + " composite score: " + String.format("%.4f", entry.score())));
        }

        // Short bottom N
        for (int i = ranked.size() - 1; i >= Math.max(ranked.size() - bottomN, 0); i--) {
            FactorScore.ScoredSymbol entry = ranked.get(i);
            signals.add(Signal.sell(entry.symbol(), name(),
                "Bottom composite score: " + String.format("%.4f", entry.score())));
        }

        return signals;
    }

    @Override
    public String name() {
        return "FactorRanking";
    }
}
