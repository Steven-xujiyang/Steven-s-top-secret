package com.quant.core.event;

import java.time.Instant;

/**
 * Sealed interface for all events in the backtesting engine.
 * Uses Java 17 sealed classes for exhaustive pattern matching in switch expressions.
 */
public sealed interface Event
    permits MarketEvent, SignalEvent, OrderEvent, FillEvent, PortfolioEvent, SystemEvent {

    /**
     * Unique event identifier.
     */
    String eventId();

    /**
     * Event timestamp (simulated clock time, not wall clock).
     */
    Instant timestamp();

    /**
     * The type of this event for dispatch.
     */
    EventType eventType();
}
