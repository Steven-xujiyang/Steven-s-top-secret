package com.quant.core.clock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Manages simulated time during a backtest.
 * The clock advances as the event loop processes bars.
 */
public class SimulationClock {

    private Instant currentTime;
    private final Instant startTime;
    private final ZoneId zone;

    public SimulationClock(Instant startTime) {
        this.startTime = startTime;
        this.currentTime = startTime;
        this.zone = ZoneId.of("Asia/Shanghai");
    }

    public SimulationClock(Instant startTime, ZoneId zone) {
        this.startTime = startTime;
        this.currentTime = startTime;
        this.zone = zone;
    }

    /**
     * Advance the clock to the next time instant.
     */
    public void tickTo(Instant newTime) {
        if (newTime.isBefore(currentTime)) {
            throw new IllegalArgumentException(
                "Cannot move clock backwards: " + newTime + " < " + currentTime);
        }
        this.currentTime = newTime;
    }

    /**
     * Get the current simulated time.
     */
    public Instant now() {
        return currentTime;
    }

    /**
     * Get the start time of the simulation.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Get the current date in the simulation's time zone.
     */
    public LocalDate currentDate() {
        return currentTime.atZone(zone).toLocalDate();
    }

    /**
     * Check if the current time is before a given instant.
     */
    public boolean isBefore(Instant other) {
        return currentTime.isBefore(other);
    }

    /**
     * Get the zone for this simulation clock.
     */
    public ZoneId getZone() {
        return zone;
    }

    /**
     * Reset the clock to start time.
     */
    public void reset() {
        this.currentTime = startTime;
    }
}
