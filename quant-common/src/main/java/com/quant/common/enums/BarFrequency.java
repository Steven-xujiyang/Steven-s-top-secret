package com.quant.common.enums;

import java.time.Duration;

/**
 * Bar frequency / time interval.
 */
public enum BarFrequency {
    TICK(Duration.ZERO, "tick"),
    M1(Duration.ofMinutes(1), "1min"),
    M5(Duration.ofMinutes(5), "5min"),
    M15(Duration.ofMinutes(15), "15min"),
    M30(Duration.ofMinutes(30), "30min"),
    H1(Duration.ofHours(1), "1h"),
    H4(Duration.ofHours(4), "4h"),
    D(Duration.ofDays(1), "day"),
    W(Duration.ofDays(7), "week"),
    M(Duration.ofDays(30), "month");

    private final Duration duration;
    private final String alias;

    BarFrequency(Duration duration, String alias) {
        this.duration = duration;
        this.alias = alias;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getAlias() {
        return alias;
    }

    /**
     * Parse a frequency string like "1min", "5min", "day", etc.
     */
    public static BarFrequency fromAlias(String alias) {
        for (BarFrequency f : values()) {
            if (f.alias.equalsIgnoreCase(alias) || f.name().equalsIgnoreCase(alias)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown bar frequency: " + alias);
    }
}
