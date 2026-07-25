package com.quant.common.util;

/**
 * Precision-safe numeric operations for financial calculations.
 */
public final class NumberUtils {

    private static final double EPSILON = 1e-10;

    private NumberUtils() {
    }

    /**
     * Round to specified decimal places.
     */
    public static double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    /**
     * Check if two doubles are approximately equal.
     */
    public static boolean almostEqual(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    /**
     * Check if a double is approximately zero.
     */
    public static boolean isZero(double value) {
        return Math.abs(value) < EPSILON;
    }

    /**
     * Format a double as percentage string.
     */
    public static String toPercent(double value, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f%%", value * 100);
    }

    /**
     * Clamp a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Calculate log return: ln(p1/p0).
     */
    public static double logReturn(double priceNow, double priceBefore) {
        if (priceBefore <= 0) return 0;
        return Math.log(priceNow / priceBefore);
    }

    /**
     * Calculate simple return: (p1-p0)/p0.
     */
    public static double simpleReturn(double priceNow, double priceBefore) {
        if (priceBefore <= 0) return 0;
        return (priceNow - priceBefore) / priceBefore;
    }
}
