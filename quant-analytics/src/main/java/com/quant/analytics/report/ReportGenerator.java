package com.quant.analytics.report;

import com.quant.analytics.performance.PerformanceAnalyzer;
import com.quant.core.model.order.Fill;
import com.quant.core.model.portfolio.PortfolioSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Generates structured backtest reports.
 */
public class ReportGenerator {

    private final PerformanceAnalyzer analyzer;
    private final String strategyName;

    public ReportGenerator(String strategyName, List<PortfolioSnapshot> navHistory, List<Fill> fills) {
        this.strategyName = strategyName;
        this.analyzer = new PerformanceAnalyzer(navHistory, fills);
    }

    /**
     * Generate a full report as a structured map.
     */
    public Map<String, Object> generate() {
        PerformanceAnalyzer.PerformanceMetrics metrics = analyzer.compute();

        return Map.of(
            "strategy", strategyName,
            "metrics", metrics.toMap(),
            "navCurve", buildNavCurveData(),
            "drawdownCurve", buildDrawdownData()
        );
    }

    private List<Map<String, Object>> buildNavCurveData() {
        // Stub: return NAV time series for chart rendering
        return List.of();
    }

    private List<Map<String, Object>> buildDrawdownData() {
        // Stub: return drawdown time series for chart rendering
        return List.of();
    }
}
