package com.quant.risk.limits;

/**
 * Configurable risk limits for pre-trade checking.
 */
public class RiskLimits {

    private double maxPositionSize = 0.3;      // max 30% of NAV in single position
    private double maxSectorExposure = 0.5;     // max 50% in single sector
    private double maxDrawdown = 0.2;           // circuit breaker at 20% drawdown
    private double maxLeverage = 2.0;           // max 2x leverage
    private int maxPositions = 50;              // max number of concurrent positions
    private double minOrderValue = 1000;        // minimum order value

    public RiskLimits() {}

    public static RiskLimits defaults() {
        return new RiskLimits();
    }

    public static RiskLimits conservativeA() {
        RiskLimits limits = new RiskLimits();
        limits.maxPositionSize = 0.1;
        limits.maxDrawdown = 0.1;
        limits.maxLeverage = 1.0;
        limits.maxPositions = 10;
        return limits;
    }

    // Getters and setters
    public double getMaxPositionSize() { return maxPositionSize; }
    public void setMaxPositionSize(double v) { this.maxPositionSize = v; }
    public double getMaxSectorExposure() { return maxSectorExposure; }
    public void setMaxSectorExposure(double v) { this.maxSectorExposure = v; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(double v) { this.maxDrawdown = v; }
    public double getMaxLeverage() { return maxLeverage; }
    public void setMaxLeverage(double v) { this.maxLeverage = v; }
    public int getMaxPositions() { return maxPositions; }
    public void setMaxPositions(int v) { this.maxPositions = v; }
    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double v) { this.minOrderValue = v; }
}
