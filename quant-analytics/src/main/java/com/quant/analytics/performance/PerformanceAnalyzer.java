package com.quant.analytics.performance;

import com.quant.core.model.order.Fill;
import com.quant.core.model.portfolio.PortfolioSnapshot;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive performance analysis of backtest results.
 */
public class PerformanceAnalyzer {

    private final List<PortfolioSnapshot> navHistory;
    private final List<Fill> fills;

    public PerformanceAnalyzer(List<PortfolioSnapshot> navHistory, List<Fill> fills) {
        this.navHistory = navHistory;
        this.fills = fills;
    }

    public PerformanceMetrics compute() {
        PerformanceMetrics m = new PerformanceMetrics();

        if (navHistory.isEmpty()) return m;

        double initialCapital = navHistory.get(0).initialCapital();
        double finalNav = navHistory.get(navHistory.size() - 1).nav();
        double[] navs = navHistory.stream().mapToDouble(PortfolioSnapshot::nav).toArray();

        m.totalReturn = (finalNav - initialCapital) / initialCapital;
        m.annualReturn = computeAnnualReturn(navs);
        m.annualVolatility = computeVolatility(navs);
        m.sharpeRatio = m.annualVolatility > 0 ? m.annualReturn / m.annualVolatility : 0;
        m.maxDrawdown = computeMaxDrawdown(navs);
        m.calmarRatio = m.maxDrawdown > 0 ? m.annualReturn / m.maxDrawdown : 0;

        // Trade statistics
        m.totalTrades = fills.size();
        if (!fills.isEmpty()) {
            computeTradeStats(m);
        }

        return m;
    }

    private double computeAnnualReturn(double[] navs) {
        if (navs.length < 2) return 0;
        return (navs[navs.length - 1] - navs[0]) / navs[0] * (252.0 / navs.length);
    }

    private double computeVolatility(double[] navs) {
        if (navs.length < 2) return 0;
        double[] returns = new double[navs.length - 1];
        double mean = 0;
        for (int i = 1; i < navs.length; i++) {
            returns[i - 1] = (navs[i] - navs[i - 1]) / navs[i - 1];
            mean += returns[i - 1];
        }
        mean /= returns.length;
        double variance = 0;
        for (double r : returns) variance += (r - mean) * (r - mean);
        variance /= returns.length;
        return Math.sqrt(variance * 252); // annualized
    }

    private double computeMaxDrawdown(double[] navs) {
        double peak = navs[0];
        double maxDD = 0;
        for (double nav : navs) {
            if (nav > peak) peak = nav;
            double dd = (peak - nav) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        return maxDD;
    }

    private void computeTradeStats(PerformanceMetrics m) {
        double[] pnls = new double[fills.size()];
        int wins = 0, losses = 0;
        double totalProfit = 0, totalLoss = 0;

        for (int i = 0; i < fills.size(); i++) {
            Fill f = fills.get(i);
            // Approximate: use commission as the cost; PnL requires matching buys to sells
            pnls[i] = -f.commission();
            if (pnls[i] > 0) { wins++; totalProfit += pnls[i]; }
            else { losses++; totalLoss += Math.abs(pnls[i]); }
        }

        m.winRate = (double) wins / fills.size();
        m.profitFactor = totalLoss > 0 ? totalProfit / totalLoss : Double.POSITIVE_INFINITY;
        m.totalCommission = fills.stream().mapToDouble(Fill::commission).sum();
    }

    /**
     * Metrics container.
     */
    public static class PerformanceMetrics {
        public double totalReturn;
        public double annualReturn;
        public double annualVolatility;
        public double sharpeRatio;
        public double maxDrawdown;
        public double calmarRatio;
        public int totalTrades;
        public double winRate;
        public double profitFactor;
        public double totalCommission;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("totalReturn", totalReturn);
            map.put("annualReturn", annualReturn);
            map.put("annualVolatility", annualVolatility);
            map.put("sharpeRatio", sharpeRatio);
            map.put("maxDrawdown", maxDrawdown);
            map.put("calmarRatio", calmarRatio);
            map.put("totalTrades", totalTrades);
            map.put("winRate", winRate);
            map.put("profitFactor", profitFactor);
            map.put("totalCommission", totalCommission);
            return map;
        }
    }
}
