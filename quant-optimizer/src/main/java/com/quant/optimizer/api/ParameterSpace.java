package com.quant.optimizer.api;

import java.util.*;

/**
 * Defines the search space for parameter optimization.
 */
public class ParameterSpace {

    private final Map<String, ParamDef> params = new LinkedHashMap<>();

    /**
     * Add an integer parameter range.
     */
    public ParameterSpace intParam(String name, int min, int max, int step) {
        params.put(name, new ParamDef(name, ParamType.INT, (double) min, (double) max, (double) step));
        return this;
    }

    /**
     * Add a double parameter range.
     */
    public ParameterSpace doubleParam(String name, double min, double max, double step) {
        params.put(name, new ParamDef(name, ParamType.DOUBLE, min, max, step));
        return this;
    }

    /**
     * Generate all combinations (grid).
     */
    public List<Map<String, Object>> generateGrid() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<List<ParamValue>> valueLists = new ArrayList<>();

        for (ParamDef def : params.values()) {
            List<ParamValue> values = new ArrayList<>();
            for (double v = def.min; v <= def.max; v += def.step) {
                double rounded = Math.round(v / def.step) * def.step;
                if (rounded > def.max) break;
                values.add(new ParamValue(def.name, def.type == ParamType.INT ? (int) rounded : rounded));
                if (def.type == ParamType.INT) v = Math.floor(v);
            }
            valueLists.add(values);
        }

        cartesianProduct(valueLists, 0, new LinkedHashMap<>(), result);
        return result;
    }

    /**
     * Sample N random combinations (for genetic algorithm initialization).
     */
    public List<Map<String, Object>> sampleRandom(int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            Map<String, Object> combo = new LinkedHashMap<>();
            for (ParamDef def : params.values()) {
                double range = def.max - def.min;
                double val = def.min + rand.nextDouble() * range;
                val = Math.round(val / def.step) * def.step;
                if (def.type == ParamType.INT) {
                    combo.put(def.name, (int) val);
                } else {
                    combo.put(def.name, val);
                }
            }
            result.add(combo);
        }
        return result;
    }

    private void cartesianProduct(List<List<ParamValue>> lists, int depth,
                                   Map<String, Object> current, List<Map<String, Object>> result) {
        if (depth == lists.size()) {
            result.add(new LinkedHashMap<>(current));
            return;
        }
        for (ParamValue val : lists.get(depth)) {
            current.put(val.name, val.value);
            cartesianProduct(lists, depth + 1, current, result);
            current.remove(val.name);
        }
    }

    public int size() { return params.size(); }
    public Set<String> paramNames() { return Collections.unmodifiableSet(params.keySet()); }

    private enum ParamType { INT, DOUBLE }
    private record ParamDef(String name, ParamType type, double min, double max, double step) {}
    private record ParamValue(String name, Object value) {}
}
