package com.quant.core.event;

import com.quant.core.model.order.Fill;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an order is filled (executed).
 */
public record FillEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    Fill fill
) implements Event {

    public FillEvent {
        eventType = EventType.FILL;
    }

    /**
     * Create a FillEvent.
     */
    public FillEvent(Instant timestamp, Fill fill) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.FILL, fill);
    }
}
