package com.quant.engine.recorder;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time metrics collection during backtest execution.
 * Tracks throughput, event counts, and performance indicators.
 */
public class MetricsCollector {

    private final AtomicLong eventCount;
    private final AtomicLong barCount;
    private final AtomicInteger tradeCount;
    private final AtomicInteger signalCount;

    private long startWallTime;
    private long endWallTime;

    public MetricsCollector() {
        this.eventCount = new AtomicLong(0);
        this.barCount = new AtomicLong(0);
        this.tradeCount = new AtomicInteger(0);
        this.signalCount = new AtomicInteger(0);
    }

    public void start() {
        this.startWallTime = System.currentTimeMillis();
    }

    public void stop() {
        this.endWallTime = System.currentTimeMillis();
    }

    public void incrementEvent() { eventCount.incrementAndGet(); }
    public void incrementBar() { barCount.incrementAndGet(); }
    public void incrementTrade() { tradeCount.incrementAndGet(); }
    public void incrementSignal(int count) { signalCount.addAndGet(count); }

    public long getEventCount() { return eventCount.get(); }
    public long getBarCount() { return barCount.get(); }
    public int getTradeCount() { return tradeCount.get(); }
    public int getSignalCount() { return signalCount.get(); }

    /**
     * Wall clock duration in milliseconds.
     */
    public long wallTimeMs() {
        long end = endWallTime > 0 ? endWallTime : System.currentTimeMillis();
        return end - startWallTime;
    }

    /**
     * Events processed per second.
     */
    public double eventsPerSecond() {
        double seconds = wallTimeMs() / 1000.0;
        return seconds > 0 ? eventCount.get() / seconds : 0;
    }

    /**
     * Bars processed per second.
     */
    public double barsPerSecond() {
        double seconds = wallTimeMs() / 1000.0;
        return seconds > 0 ? barCount.get() / seconds : 0;
    }

    @Override
    public String toString() {
        return String.format("Events: %d | Bars: %d | Trades: %d | Signals: %d | Time: %dms | Speed: %.0f bars/s",
            eventCount.get(), barCount.get(), tradeCount.get(), signalCount.get(),
            wallTimeMs(), barsPerSecond());
    }
}
