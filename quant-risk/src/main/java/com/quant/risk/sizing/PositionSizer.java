package com.quant.risk.sizing;

/**
 * Determines position size based on portfolio state and risk parameters.
 */
@FunctionalInterface
public interface PositionSizer {

    /**
     * Calculate position size (number of shares/contracts).
     *
     * @param capital   available capital
     * @param price     current price per unit
     * @param riskPct   risk per trade as fraction of capital
     * @param atr       average true range (for volatility-based sizing; 0 if unused)
     * @return suggested quantity
     */
    double calculate(double capital, double price, double riskPct, double atr);

    /**
     * Fixed fraction: risk a fixed % of capital per trade.
     */
    static PositionSizer fixedFraction() {
        return (capital, price, riskPct, atr) -> {
            double allocation = capital * riskPct;
            return Math.floor(allocation / price);
        };
    }

    /**
     * Volatility-targeted sizing: adjust position size based on ATR.
     * Larger positions in low volatility, smaller in high volatility.
     */
    static PositionSizer volatilityTarget(double targetVol) {
        return (capital, price, riskPct, atr) -> {
            if (atr <= 0) {
                double allocation = capital * riskPct;
                return Math.floor(allocation / price);
            }
            double riskPerShare = atr * 2; // 2x ATR as risk per share
            double riskBudget = capital * riskPct * targetVol;
            return Math.floor(riskBudget / riskPerShare);
        };
    }

    /**
     * Equal weight: allocate equal capital across N positions.
     */
    static PositionSizer equalWeight(int numPositions) {
        return (capital, price, riskPct, atr) -> {
            double allocation = capital / numPositions;
            return Math.floor(allocation / price);
        };
    }
}
