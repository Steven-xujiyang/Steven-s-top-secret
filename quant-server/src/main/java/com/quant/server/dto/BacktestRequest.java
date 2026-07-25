package com.quant.server.dto;

import java.util.List;
import java.util.Map;

/**
 * REST request DTO for submitting a backtest job.
 */
public class BacktestRequest {

    private String strategyClass;
    private List<String> symbols;
    private String startDate;
    private String endDate;
    private double initialCapital = 1_000_000;
    private String frequency = "day";
    private Map<String, Object> params;

    public String getStrategyClass() { return strategyClass; }
    public void setStrategyClass(String s) { this.strategyClass = s; }
    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> s) { this.symbols = s; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String d) { this.startDate = d; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String d) { this.endDate = d; }
    public double getInitialCapital() { return initialCapital; }
    public void setInitialCapital(double c) { this.initialCapital = c; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String f) { this.frequency = f; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> p) { this.params = p; }
}
