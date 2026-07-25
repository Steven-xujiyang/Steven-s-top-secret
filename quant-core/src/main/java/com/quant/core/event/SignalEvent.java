package com.quant.core.event;

import com.quant.core.model.signal.Signal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when a strategy generates trading signals.
 */
public record SignalEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    String strategyId,
    List<Signal> signals
) implements Event {

    public SignalEvent {
        eventType = EventType.SIGNAL;
    }

    /**
     * Create a SignalEvent.
     */
    public SignalEvent(Instant timestamp, String strategyId, List<Signal> signals) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.SIGNAL,
             strategyId, signals);
    }

    /**
     * Whether this event contains any signals.
     */
    public boolean hasSignals() {
        return signals != null && !signals.isEmpty();
    }
}
