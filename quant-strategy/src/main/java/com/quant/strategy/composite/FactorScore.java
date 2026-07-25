package com.quant.strategy.composite;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;
import com.quant.strategy.factor.Factor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes a weighted composite factor score from multiple factors.
 * Each factor is weighted and summed to produce a final score per symbol.
 */
public class FactorScore {

    private final Map<Factor, Double> factorWeights;

    public FactorScore() {
        this.factorWeights = new LinkedHashMap<>();
    }

    /**
     * Add a factor with a weight.
     */
    public FactorScore addFactor(Factor factor, double weight) {
        factorWeights.put(factor, weight);
        return this;
    }

    /**
     * Compute composite scores for a set of symbols.
     * Returns symbols sorted by score descending (highest first).
     */
    public List<ScoredSymbol> scoreSymbols(Map<String, List<Bar>> symbolBarData) {
        List<ScoredSymbol> results = new ArrayList<>();

        for (var entry : symbolBarData.entrySet()) {
            String symbol = entry.getKey();
            List<Bar> bars = entry.getValue();
            if (bars.isEmpty()) continue;

            double totalScore = 0;
            double totalWeight = 0;

            for (var factorEntry : factorWeights.entrySet()) {
                Factor factor = factorEntry.getKey();
                double weight = factorEntry.getValue();
                FactorValue fv = factor.calculate(symbol, bars);
                if (!Double.isNaN(fv.rawValue())) {
                    totalScore += fv.rawValue() * weight;
                    totalWeight += Math.abs(weight);
                }
            }

            if (totalWeight > 0) {
                results.add(new ScoredSymbol(symbol, totalScore / totalWeight));
            }
        }

        results.sort(Comparator.comparingDouble(ScoredSymbol::score).reversed());
        return results;
    }

    /**
     * A symbol with a composite score.
     */
    public record ScoredSymbol(String symbol, double score) {}

    public int factorCount() {
        return factorWeights.size();
    }
}
