package com.quant.optimizer.genetic;

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
 * Genetic algorithm for parameter optimization.
 *
 * Algorithm:
 * 1. Initialize random population
 * 2. Evaluate fitness of each chromosome via backtest
 * 3. Select parents (tournament selection)
 * 4. Crossover to produce children
 * 5. Mutate children
 * 6. Replace weakest with children
 * 7. Repeat until convergence or max generations
 */
public class GeneticOptimizer {

    private static final Logger log = LoggerFactory.getLogger(GeneticOptimizer.class);

    private final OptimizationExecutor executor;
    private final ObjectiveFunction objective;
    private final int populationSize;
    private final int generations;
    private final double mutationRate;
    private final double crossoverRate;
    private final Random random;

    public GeneticOptimizer(Builder builder) {
        this.executor = builder.executor;
        this.objective = builder.objective;
        this.populationSize = builder.populationSize;
        this.generations = builder.generations;
        this.mutationRate = builder.mutationRate;
        this.crossoverRate = builder.crossoverRate;
        this.random = new Random();
    }

    /**
     * Run genetic optimization.
     */
    public OptimizationResult optimize(ParameterSpace paramSpace) {
        log.info("GA optimization: pop={}, gens={}, mutRate={}", populationSize, generations, mutationRate);

        List<OptimizationResult.ScoredParams> allEvaluated = new ArrayList<>();

        // Initialize population
        List<Map<String, Object>> population = paramSpace.sampleRandom(populationSize);
        Map<Map<String, Object>, Double> fitnessCache = new LinkedHashMap<>();

        // Evaluate initial population
        List<BacktestResult> results = executor.executeAll(population);
        for (int i = 0; i < population.size(); i++) {
            double fitness = objective.evaluate(results.get(i));
            fitnessCache.put(population.get(i), fitness);
            allEvaluated.add(new OptimizationResult.ScoredParams(population.get(i), fitness, results.get(i)));
        }

        // Evolution loop
        for (int gen = 0; gen < generations; gen++) {
            // Sort by fitness
            List<Map<String, Object>> sorted = population.stream()
                .sorted((a, b) -> Double.compare(fitnessCache.getOrDefault(b, Double.NEGATIVE_INFINITY),
                    fitnessCache.getOrDefault(a, Double.NEGATIVE_INFINITY)))
                .collect(Collectors.toCollection(ArrayList::new));

            double bestFitness = fitnessCache.getOrDefault(sorted.get(0), Double.NEGATIVE_INFINITY);
            log.debug("Gen {}: best fitness = {:.4f}", gen, bestFitness);

            // Elitism: keep top 2
            List<Map<String, Object>> newPopulation = new ArrayList<>();
            newPopulation.add(new LinkedHashMap<>(sorted.get(0)));
            newPopulation.add(new LinkedHashMap<>(sorted.get(1)));

            // Fill rest via selection, crossover, mutation
            while (newPopulation.size() < populationSize) {
                Map<String, Object> parent1 = tournamentSelect(sorted, fitnessCache, 3);
                Map<String, Object> parent2 = tournamentSelect(sorted, fitnessCache, 3);

                Map<String, Object> child;
                if (random.nextDouble() < crossoverRate) {
                    child = crossover(parent1, parent2, paramSpace);
                } else {
                    child = new LinkedHashMap<>(parent1);
                }

                if (random.nextDouble() < mutationRate) {
                    child = mutate(child, paramSpace);
                }

                newPopulation.add(child);
            }

            // Evaluate new individuals
            List<Map<String, Object>> toEvaluate = new ArrayList<>();
            for (var individual : newPopulation) {
                if (!fitnessCache.containsKey(individual)) {
                    toEvaluate.add(individual);
                }
            }

            if (!toEvaluate.isEmpty()) {
                List<BacktestResult> newResults = executor.executeAll(toEvaluate);
                for (int i = 0; i < toEvaluate.size(); i++) {
                    double fitness = objective.evaluate(newResults.get(i));
                    fitnessCache.put(toEvaluate.get(i), fitness);
                    allEvaluated.add(new OptimizationResult.ScoredParams(toEvaluate.get(i), fitness, newResults.get(i)));
                }
            }

            population = newPopulation;
        }

        // Final ranking
        List<OptimizationResult.ScoredParams> sorted = allEvaluated.stream()
            .sorted(Comparator.comparingDouble(OptimizationResult.ScoredParams::fitness).reversed())
            .toList();

        return new OptimizationResult("GENETIC", sorted.get(0),
            sorted.stream().limit(10).toList(), sorted);
    }

    private Map<String, Object> tournamentSelect(List<Map<String, Object>> sorted,
                                                   Map<Map<String, Object>, Double> fitness, int k) {
        Map<String, Object> best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < k; i++) {
            int idx = random.nextInt(sorted.size());
            Map<String, Object> candidate = sorted.get(idx);
            double f = fitness.getOrDefault(candidate, Double.NEGATIVE_INFINITY);
            if (f > bestFitness) {
                bestFitness = f;
                best = candidate;
            }
        }
        return new LinkedHashMap<>(best != null ? best : sorted.get(0));
    }

    private Map<String, Object> crossover(Map<String, Object> p1, Map<String, Object> p2,
                                            ParameterSpace space) {
        Map<String, Object> child = new LinkedHashMap<>();
        for (String key : space.paramNames()) {
            child.put(key, random.nextBoolean() ? p1.get(key) : p2.get(key));
        }
        return child;
    }

    private Map<String, Object> mutate(Map<String, Object> individual, ParameterSpace space) {
        Map<String, Object> mutated = new LinkedHashMap<>(individual);
        List<String> keys = new ArrayList<>(space.paramNames());
        String key = keys.get(random.nextInt(keys.size()));
        Object val = mutated.get(key);
        if (val instanceof Integer i) {
            int delta = random.nextInt(3) - 1; // -1, 0, +1
            mutated.put(key, i + delta);
        } else if (val instanceof Double d) {
            double delta = (random.nextDouble() - 0.5) * d * 0.2; // ±10%
            mutated.put(key, d + delta);
        }
        return mutated;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private OptimizationExecutor executor;
        private ObjectiveFunction objective = ObjectiveFunction.maximizeSharpe();
        private int populationSize = 50;
        private int generations = 20;
        private double mutationRate = 0.1;
        private double crossoverRate = 0.8;

        public Builder executor(OptimizationExecutor e) { this.executor = e; return this; }
        public Builder objective(ObjectiveFunction o) { this.objective = o; return this; }
        public Builder populationSize(int s) { this.populationSize = s; return this; }
        public Builder generations(int g) { this.generations = g; return this; }
        public Builder mutationRate(double r) { this.mutationRate = r; return this; }
        public Builder crossoverRate(double r) { this.crossoverRate = r; return this; }

        public GeneticOptimizer build() {
            if (executor == null) throw new IllegalStateException("Executor is required");
            return new GeneticOptimizer(this);
        }
    }
}
