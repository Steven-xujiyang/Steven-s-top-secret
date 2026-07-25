package com.quant.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * System lifecycle events (start, pause, resume, stop, heartbeat).
 */
public record SystemEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    SystemEventType systemEventType,
    String message
) implements Event {

    public SystemEvent {
        eventType = EventType.SYSTEM;
    }

    public SystemEvent(Instant timestamp, SystemEventType type, String message) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.SYSTEM,
             type, message);
    }

    public SystemEvent(Instant timestamp, SystemEventType type) {
        this(timestamp, type, "");
    }

    /**
     * System event subtypes.
     */
    public enum SystemEventType {
        START,
        PAUSE,
        RESUME,
        STOP,
        HEARTBEAT,
        ERROR
    }
}
