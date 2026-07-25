package com.quant.server.service;

import com.quant.analytics.performance.PerformanceAnalyzer;
import com.quant.analytics.report.ReportGenerator;
import com.quant.common.enums.BarFrequency;
import com.quant.engine.BacktestEngine;
import com.quant.engine.loop.BacktestResult;
import com.quant.server.dto.BacktestRequest;
import com.quant.server.dto.BacktestResponse;
import com.quant.server.job.JobStatus;
import com.quant.strategy.api.Strategy;
import com.quant.strategy.api.StrategyConfig;
import com.quant.strategy.examples.DualMovingAverageStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BacktestJobService {

    private static final Logger log = LoggerFactory.getLogger(BacktestJobService.class);

    private final Map<String, BacktestResponse> jobs = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public BacktestJobService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Submit a backtest job for async execution.
     */
    public String submit(BacktestRequest request) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        BacktestResponse response = new BacktestResponse(jobId, "PENDING", "Job submitted");
        jobs.put(jobId, response);

        runBacktest(jobId, request);
        return jobId;
    }

    @Async("backtestExecutor")
    public void runBacktest(String jobId, BacktestRequest request) {
        try {
            updateStatus(jobId, "RUNNING", "Backtest in progress");

            // Resolve strategy
            Strategy strategy = resolveStrategy(request.getStrategyClass());
            strategy.init(new StrategyConfig(request.getStrategyClass(), request.getParams()));

            // Build engine
            BacktestEngine engine = BacktestEngine.builder()
                .csvData(Path.of("./data"), com.quant.common.enums.Market.ASHARE)
                .initialCapital(request.getInitialCapital())
                .frequency(BarFrequency.fromAlias(request.getFrequency()))
                .build();

            // Run backtest
            Set<String> symbols = new HashSet<>(request.getSymbols());
            LocalDate start = LocalDate.parse(request.getStartDate());
            LocalDate end = LocalDate.parse(request.getEndDate());

            BacktestResult result = engine.run(strategy, symbols, start, end);

            // Build response
            BacktestResponse response = jobs.get(jobId);
            if (response != null) {
                response.setStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
                response.setMessage(result.isSuccess() ? "Backtest completed" : result.getErrorMessage());
                response.setStrategyName(result.getStrategyName());

                if (result.isSuccess()) {
                    PerformanceAnalyzer analyzer = new PerformanceAnalyzer(
                        result.getPortfolioHistory(), result.getFills());
                    response.setMetrics(analyzer.compute().toMap());
                }
            }

            updateStatus(jobId, response.getStatus(), response.getMessage());
            log.info("Backtest job {} completed: {}", jobId, response.getStatus());

        } catch (Exception e) {
            log.error("Backtest job {} failed: {}", jobId, e.getMessage(), e);
            updateStatus(jobId, "FAILED", e.getMessage());
        }
    }

    private Strategy resolveStrategy(String className) {
        // Simple strategy resolution — in production, use SPI/ServiceLoader
        return switch (className) {
            case "DualMovingAverageStrategy" -> new DualMovingAverageStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy: " + className);
        };
    }

    private void updateStatus(String jobId, String status, String message) {
        BacktestResponse response = jobs.get(jobId);
        if (response != null) {
            response.setStatus(status);
            response.setMessage(message);
        }
        messagingTemplate.convertAndSend("/topic/backtest/" + jobId,
            Map.of("jobId", jobId, "status", status, "message", message));
    }

    public Optional<BacktestResponse> getStatus(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Optional<BacktestResponse> getResult(String jobId) {
        BacktestResponse r = jobs.get(jobId);
        if (r != null && "COMPLETED".equals(r.getStatus())) {
            return Optional.of(r);
        }
        return Optional.empty();
    }

    public List<BacktestResponse> listRecent() {
        return new ArrayList<>(jobs.values());
    }

    public boolean cancel(String jobId) {
        BacktestResponse r = jobs.get(jobId);
        if (r != null && ("PENDING".equals(r.getStatus()) || "RUNNING".equals(r.getStatus()))) {
            r.setStatus("CANCELLED");
            r.setMessage("Job cancelled by user");
            return true;
        }
        return false;
    }
}
