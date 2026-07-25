package com.quant.optimizer.api;

import com.quant.engine.loop.BacktestResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of a parameter optimization run.
 */
public class OptimizationResult {

    private final String algorithm;
    private final ScoredParams best;
    private final List<ScoredParams> topResults;
    private final List<ScoredParams> allResults;

    public OptimizationResult(String algorithm, ScoredParams best,
                               List<ScoredParams> topResults, List<ScoredParams> allResults) {
        this.algorithm = algorithm;
        this.best = best;
        this.topResults = topResults;
        this.allResults = allResults;
    }

    public String getAlgorithm() { return algorithm; }
    public ScoredParams getBest() { return best; }
    public List<ScoredParams> getTopResults() { return Collections.unmodifiableList(topResults); }
    public List<ScoredParams> getAllResults() { return Collections.unmodifiableList(allResults); }

    public int totalEvaluations() { return allResults.size(); }

    /**
     * A parameter set with its fitness score and backtest result.
     */
    public static class ScoredParams {
        private final Map<String, Object> params;
        private final double fitness;
        private final BacktestResult backtestResult;

        public ScoredParams(Map<String, Object> params, double fitness, BacktestResult backtestResult) {
            this.params = params;
            this.fitness = fitness;
            this.backtestResult = backtestResult;
        }

        public Map<String, Object> getParams() { return params; }
        public double fitness() { return fitness; }
        public BacktestResult getBacktestResult() { return backtestResult; }

        @Override
        public String toString() {
            return String.format("fitness=%.4f params=%s", fitness, params);
        }
    }
}
