package com.quant.engine.loop;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Configuration for the event-driven backtesting loop.
 */
public class EventLoopConfig {

    private Instant startTime;
    private Instant endTime;
    private long warmUpBars = 0;
    private ZoneId zone = ZoneId.of("Asia/Shanghai");

    public EventLoopConfig() {}

    /**
     * Create a config for a date range.
     */
    public static EventLoopConfig forDateRange(LocalDate start, LocalDate end) {
        EventLoopConfig config = new EventLoopConfig();
        config.startTime = start.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        config.endTime = end.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        return config;
    }

    // Getters and setters
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public long getWarmUpBars() { return warmUpBars; }
    public void setWarmUpBars(long warmUpBars) { this.warmUpBars = warmUpBars; }
    public ZoneId getZone() { return zone; }
    public void setZone(ZoneId zone) { this.zone = zone; }
}
