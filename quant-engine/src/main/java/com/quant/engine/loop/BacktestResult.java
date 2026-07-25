package com.quant.engine.loop;

import com.quant.core.model.order.Fill;
import com.quant.core.model.portfolio.PortfolioSnapshot;

import java.util.List;

/**
 * Result of a completed backtest run.
 * Contains all data needed for performance analysis.
 */
public class BacktestResult {

    private final String strategyName;
    private final List<Fill> fills;
    private final List<PortfolioSnapshot> portfolioHistory;
    private final BacktestMetrics metrics;
    private final boolean success;
    private final String errorMessage;

    public BacktestResult(String strategyName, List<Fill> fills,
                          List<PortfolioSnapshot> portfolioHistory,
                          BacktestMetrics metrics, boolean success, String errorMessage) {
        this.strategyName = strategyName;
        this.fills = fills;
        this.portfolioHistory = portfolioHistory;
        this.metrics = metrics;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Create a successful result.
     */
    public static BacktestResult success(String strategyName, List<Fill> fills,
                                          List<PortfolioSnapshot> history, BacktestMetrics metrics) {
        return new BacktestResult(strategyName, fills, history, metrics, true, null);
    }

    /**
     * Create a failed result.
     */
    public static BacktestResult failure(String strategyName, String errorMessage) {
        return new BacktestResult(strategyName, List.of(), List.of(), new BacktestMetrics(),
            false, errorMessage);
    }

    // Getters
    public String getStrategyName() { return strategyName; }
    public List<Fill> getFills() { return fills; }
    public List<PortfolioSnapshot> getPortfolioHistory() { return portfolioHistory; }
    public BacktestMetrics getMetrics() { return metrics; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    public int tradeCount() { return fills.size(); }

    /**
     * Final NAV, or 0 if no history.
     */
    public double finalNav() {
        if (portfolioHistory.isEmpty()) return 0;
        return portfolioHistory.get(portfolioHistory.size() - 1).nav();
    }

    /**
     * Engine performance metrics.
     */
    public static class BacktestMetrics {
        private long wallTimeMs;
        private long barCount;
        private long eventCount;
        private int tradeCount;

        public long getWallTimeMs() { return wallTimeMs; }
        public void setWallTimeMs(long wallTimeMs) { this.wallTimeMs = wallTimeMs; }
        public long getBarCount() { return barCount; }
        public void setBarCount(long barCount) { this.barCount = barCount; }
        public long getEventCount() { return eventCount; }
        public void setEventCount(long eventCount) { this.eventCount = eventCount; }
        public int getTradeCount() { return tradeCount; }
        public void setTradeCount(int tradeCount) { this.tradeCount = tradeCount; }
    }
}
