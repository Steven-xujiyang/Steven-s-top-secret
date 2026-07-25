package com.quant.core.event;

/**
 * Event type enumeration for dispatch and priority ordering.
 * Priority order: lower ordinal = processed earlier at the same timestamp.
 */
public enum EventType {
    /** New bar data arrived — highest priority */
    MARKET_DATA(0),
    /** Strategy generated signals */
    SIGNAL(1),
    /** Orders were created */
    ORDER(2),
    /** Orders were filled */
    FILL(3),
    /** Portfolio state updated — lowest priority */
    PORTFOLIO(4),
    /** System lifecycle events (start, stop, heartbeat) */
    SYSTEM(5);

    private final int priority;

    EventType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
