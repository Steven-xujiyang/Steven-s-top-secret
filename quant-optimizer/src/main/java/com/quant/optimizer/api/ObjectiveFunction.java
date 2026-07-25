package com.quant.optimizer.api;

import com.quant.engine.loop.BacktestResult;

/**
 * Evaluates a backtest result into a fitness score.
 */
@FunctionalInterface
public interface ObjectiveFunction {

    double evaluate(BacktestResult result);

    /**
     * Maximize Sharpe ratio (annual return / annual volatility).
     */
    static ObjectiveFunction maximizeSharpe() {
        return result -> {
            if (!result.isSuccess() || result.getPortfolioHistory().isEmpty()) return Double.NEGATIVE_INFINITY;
            var history = result.getPortfolioHistory();
            double initial = history.get(0).initialCapital();
            double finalNav = history.get(history.size() - 1).nav();
            double totalReturn = (finalNav - initial) / initial;

            double[] returns = new double[history.size() - 1];
            for (int i = 1; i < history.size(); i++) {
                returns[i - 1] = (history.get(i).nav() - history.get(i - 1).nav()) / history.get(i - 1).nav();
            }
            double mean = 0;
            for (double r : returns) mean += r;
            mean /= returns.length;
            double variance = 0;
            for (double r : returns) variance += (r - mean) * (r - mean);
            double std = Math.sqrt(variance / returns.length);

            return std > 0 ? totalReturn / std : 0;
        };
    }

    /**
     * Maximize Calmar ratio (total return / max drawdown).
     */
    static ObjectiveFunction maximizeCalmar() {
        return result -> {
            if (!result.isSuccess() || result.getPortfolioHistory().isEmpty()) return Double.NEGATIVE_INFINITY;
            var history = result.getPortfolioHistory();
            double initial = history.get(0).initialCapital();
            double finalNav = history.get(history.size() - 1).nav();
            double totalReturn = (finalNav - initial) / initial;

            double peak = history.get(0).nav();
            double maxDD = 0;
            for (var snapshot : history) {
                if (snapshot.nav() > peak) peak = snapshot.nav();
                double dd = (peak - snapshot.nav()) / peak;
                if (dd > maxDD) maxDD = dd;
            }
            return maxDD > 0 ? totalReturn / maxDD : 0;
        };
    }
}
