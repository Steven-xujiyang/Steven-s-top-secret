package com.quant.engine.recorder;

import com.quant.core.event.*;
import com.quant.core.model.order.Fill;
import com.quant.core.model.portfolio.PortfolioSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records all backtest events and portfolio snapshots for later analysis.
 * Subscribes to EventBus to capture fills and portfolio updates.
 */
public class BacktestRecorder {

    private static final Logger log = LoggerFactory.getLogger(BacktestRecorder.class);

    private final List<Fill> fills;
    private final List<PortfolioSnapshot> portfolioHistory;
    private final List<BacktestRecord> records;

    public BacktestRecorder() {
        this.fills = new ArrayList<>();
        this.portfolioHistory = new CopyOnWriteArrayList<>();
        this.records = new ArrayList<>();
    }

    /**
     * Subscribe to events from an EventBus.
     */
    public void attach(EventBus eventBus) {
        eventBus.subscribe(EventType.FILL, this::onFill);
        eventBus.subscribe(EventType.PORTFOLIO, this::onPortfolio);
        log.info("Recorder attached to event bus");
    }

    private void onFill(Event event) {
        if (event instanceof FillEvent fe) {
            fills.add(fe.fill());
        }
    }

    private void onPortfolio(Event event) {
        if (event instanceof PortfolioEvent pe) {
            portfolioHistory.add(pe.snapshot());
        }
    }

    /**
     * Record a manual entry.
     */
    public void record(Instant timestamp, String type, String detail, double value) {
        records.add(new BacktestRecord(timestamp, type, detail, value));
    }

    /**
     * Record a NAV point.
     */
    public void recordNav(PortfolioSnapshot snapshot) {
        portfolioHistory.add(snapshot);
    }

    /**
     * Get all fills in order.
     */
    public List<Fill> getFills() {
        return fills;
    }

    /**
     * Get portfolio history (NAV over time).
     */
    public List<PortfolioSnapshot> getPortfolioHistory() {
        return portfolioHistory;
    }

    /**
     * Total number of trades.
     */
    public int tradeCount() {
        return fills.size();
    }

    /**
     * Clear all recorded data.
     */
    public void clear() {
        fills.clear();
        portfolioHistory.clear();
        records.clear();
    }

    /**
     * Internal record structure.
     */
    public record BacktestRecord(Instant timestamp, String type, String detail, double value) {}
}
