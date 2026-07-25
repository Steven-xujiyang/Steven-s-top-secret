package com.quant.strategy.api;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy configuration: name and parameter map.
 */
public class StrategyConfig {

    private final String strategyName;
    private final Map<String, Object> parameters;

    public StrategyConfig(String strategyName, Map<String, Object> parameters) {
        this.strategyName = strategyName;
        this.parameters = parameters != null ? parameters : Collections.emptyMap();
    }

    /**
     * Create with empty parameters.
     */
    public static StrategyConfig empty(String strategyName) {
        return new StrategyConfig(strategyName, Collections.emptyMap());
    }

    /**
     * Get a string parameter.
     */
    public String getString(String key, String defaultValue) {
        Object val = parameters.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    /**
     * Get a double parameter.
     */
    public double getDouble(String key, double defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Get an int parameter.
     */
    public int getInt(String key, int defaultValue) {
        return (int) getDouble(key, defaultValue);
    }

    /**
     * Get a boolean parameter.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    /**
     * Check if a parameter exists.
     */
    public boolean has(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Get raw parameter value.
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    public String getStrategyName() { return strategyName; }
    public Map<String, Object> getParameters() { return Collections.unmodifiableMap(parameters); }
}
