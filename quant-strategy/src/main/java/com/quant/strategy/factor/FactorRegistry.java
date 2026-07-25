package com.quant.strategy.factor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of available factors.
 * Factors can be registered programmatically and looked up by name.
 */
public class FactorRegistry {

    private static final FactorRegistry INSTANCE = new FactorRegistry();
    private final Map<String, Factor> factors = new ConcurrentHashMap<>();

    private FactorRegistry() {}

    public static FactorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a factor.
     */
    public void register(Factor factor) {
        factors.put(factor.name().toLowerCase(), factor);
    }

    /**
     * Get a factor by name.
     */
    public Optional<Factor> get(String name) {
        return Optional.ofNullable(factors.get(name.toLowerCase()));
    }

    /**
     * List all registered factors.
     */
    public Collection<Factor> listAll() {
        return Collections.unmodifiableCollection(factors.values());
    }

    /**
     * List factors by category.
     */
    public List<Factor> listByCategory(Factor.FactorCategory category) {
        return factors.values().stream()
            .filter(f -> f.category() == category)
            .toList();
    }

    /**
     * Number of registered factors.
     */
    public int size() {
        return factors.size();
    }

    /**
     * Clear all registered factors.
     */
    public void clear() {
        factors.clear();
    }
}
