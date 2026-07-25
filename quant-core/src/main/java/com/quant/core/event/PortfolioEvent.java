package com.quant.core.event;

import com.quant.core.model.portfolio.PortfolioSnapshot;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted after portfolio state has been updated.
 */
public record PortfolioEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    PortfolioSnapshot snapshot
) implements Event {

    public PortfolioEvent {
        eventType = EventType.PORTFOLIO;
    }

    /**
     * Create a PortfolioEvent.
     */
    public PortfolioEvent(Instant timestamp, PortfolioSnapshot snapshot) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.PORTFOLIO, snapshot);
    }
}
