package com.quant.core.event;

import com.quant.core.model.market.Bar;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when new bar data is available.
 * Contains bars for all symbols at a given timestamp.
 */
public record MarketEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    List<Bar> bars
) implements Event {

    public MarketEvent {
        eventType = EventType.MARKET_DATA;
    }

    /**
     * Create a MarketEvent with a single bar.
     */
    public MarketEvent(Instant timestamp, Bar bar) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.MARKET_DATA,
             List.of(bar));
    }

    /**
     * Create a MarketEvent with multiple bars.
     */
    public MarketEvent(Instant timestamp, Bar... bars) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.MARKET_DATA,
             Arrays.asList(bars));
    }

    /**
     * Get the bar for a specific symbol, or null if not present.
     */
    public Bar barForSymbol(String symbol) {
        return bars.stream()
            .filter(b -> b.symbol().equals(symbol))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get all symbols in this event.
     */
    public List<String> symbols() {
        return bars.stream().map(Bar::symbol).toList();
    }
}
