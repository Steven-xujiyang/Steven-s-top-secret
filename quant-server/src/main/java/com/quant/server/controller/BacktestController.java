package com.quant.server.controller;

import com.quant.server.dto.BacktestRequest;
import com.quant.server.dto.BacktestResponse;
import com.quant.server.service.BacktestJobService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {

    private final BacktestJobService backtestJobService;

    public BacktestController(BacktestJobService backtestJobService) {
        this.backtestJobService = backtestJobService;
    }

    /**
     * Submit a new backtest job.
     */
    @PostMapping
    public ResponseEntity<BacktestResponse> submit(@RequestBody BacktestRequest request) {
        String jobId = backtestJobService.submit(request);
        return ResponseEntity.ok(new BacktestResponse(jobId, "PENDING", "Backtest job submitted"));
    }

    /**
     * Get backtest job status.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<BacktestResponse> getStatus(@PathVariable String jobId) {
        return backtestJobService.getStatus(jobId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get backtest result.
     */
    @GetMapping("/{jobId}/result")
    public ResponseEntity<?> getResult(@PathVariable String jobId) {
        return backtestJobService.getResult(jobId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List recent backtest jobs.
     */
    @GetMapping("/list")
    public ResponseEntity<?> listJobs() {
        return ResponseEntity.ok(backtestJobService.listRecent());
    }

    /**
     * Cancel a running backtest.
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> cancel(@PathVariable String jobId) {
        boolean cancelled = backtestJobService.cancel(jobId);
        if (cancelled) {
            return ResponseEntity.ok(Map.of("jobId", jobId, "status", "CANCELLED"));
        }
        return ResponseEntity.notFound().build();
    }
}
