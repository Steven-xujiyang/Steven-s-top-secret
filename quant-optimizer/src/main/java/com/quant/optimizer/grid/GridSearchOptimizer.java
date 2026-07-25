package com.quant.optimizer.grid;

import com.quant.engine.loop.BacktestResult;
import com.quant.optimizer.api.ObjectiveFunction;
import com.quant.optimizer.api.OptimizationResult;
import com.quant.optimizer.api.ParameterSpace;
import com.quant.optimizer.executor.OptimizationExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exhaustive grid search parameter optimizer.
 * Tests every combination in the parameter space and ranks by objective function.
 */
public class GridSearchOptimizer {

    private static final Logger log = LoggerFactory.getLogger(GridSearchOptimizer.class);

    private final OptimizationExecutor executor;
    private final ObjectiveFunction objective;
    private final int topK;

    public GridSearchOptimizer(OptimizationExecutor executor, ObjectiveFunction objective, int topK) {
        this.executor = executor;
        this.objective = objective;
        this.topK = topK;
    }

    /**
     * Run grid search optimization.
     *
     * @param paramSpace parameter space definition
     * @return optimization result with all evaluated combinations sorted by fitness
     */
    public OptimizationResult optimize(ParameterSpace paramSpace) {
        List<Map<String, Object>> grid = paramSpace.generateGrid();
        log.info("Grid search: {} parameters, {} combinations", paramSpace.size(), grid.size());

        List<BacktestResult> results = executor.executeAll(grid);
        log.info("Grid search complete: {} results", results.size());

        // Score and rank
        List<OptimizationResult.ScoredParams> scored = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            BacktestResult result = results.get(i);
            double fitness = objective.evaluate(result);
            scored.add(new OptimizationResult.ScoredParams(grid.get(i), fitness, result));
        }

        scored.sort(Comparator.comparingDouble(OptimizationResult.ScoredParams::fitness).reversed());

        List<OptimizationResult.ScoredParams> top = scored.stream()
            .limit(topK > 0 ? topK : scored.size())
            .collect(Collectors.toList());

        OptimizationResult.ScoredParams best = scored.isEmpty() ? null : scored.get(0);

        return new OptimizationResult("GRID_SEARCH", best, top, scored);
    }
}
