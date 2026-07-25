package com.quant.server.dto;

import com.quant.analytics.performance.PerformanceAnalyzer;

import java.util.List;
import java.util.Map;

/**
 * REST response DTO for backtest results.
 */
public class BacktestResponse {

    private String jobId;
    private String status;
    private String message;
    private String strategyName;
    private Map<String, Object> metrics;
    private List<Map<String, Object>> navCurve;

    public BacktestResponse() {}

    public BacktestResponse(String jobId, String status, String message) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
    }

    // Getters and setters
    public String getJobId() { return jobId; }
    public void setJobId(String id) { this.jobId = id; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String n) { this.strategyName = n; }
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> m) { this.metrics = m; }
    public List<Map<String, Object>> getNavCurve() { return navCurve; }
    public void setNavCurve(List<Map<String, Object>> c) { this.navCurve = c; }
}
