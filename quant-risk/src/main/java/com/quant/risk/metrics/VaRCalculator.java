package com.quant.risk.metrics;

import java.util.Arrays;
import java.util.List;

/**
 * Value at Risk (VaR) calculator.
 */
public class VaRCalculator {

    /**
     * Historical VaR: the alpha-percentile of historical returns.
     *
     * @param returns    historical return series
     * @param confidence confidence level (e.g., 0.95 for 95%)
     * @return VaR as a positive value (loss amount)
     */
    public static double historicalVaR(List<Double> returns, double confidence) {
        double[] sorted = returns.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        int index = (int) (sorted.length * (1 - confidence));
        return -sorted[index];
    }

    /**
     * Parametric VaR assuming normal distribution.
     *
     * @param mean        mean return
     * @param std         standard deviation of returns
     * @param confidence  confidence level
     * @param portfolioValue current portfolio value
     * @return VaR in currency units
     */
    public static double parametricVaR(double mean, double std, double confidence, double portfolioValue) {
        double zScore = normalQuantile(1 - confidence);
        return portfolioValue * (zScore * std - mean);
    }

    /**
     * Approximate normal quantile (inverse CDF).
     * For common confidence levels: 0.95→1.645, 0.99→2.326
     */
    private static double normalQuantile(double p) {
        // Approximation using Abramowitz and Stegun
        double t = Math.sqrt(-2 * Math.log(Math.min(p, 1 - p)));
        double c0 = 2.515517, c1 = 0.802853, c2 = 0.010328;
        double d1 = 1.432788, d2 = 0.189269, d3 = 0.001308;
        double z = t - (c0 + c1 * t + c2 * t * t) / (1 + d1 * t + d2 * t * t + d3 * t * t * t);
        return p < 0.5 ? -z : z;
    }
}
