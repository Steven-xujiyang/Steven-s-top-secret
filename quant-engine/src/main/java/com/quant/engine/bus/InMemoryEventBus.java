package com.quant.engine.bus;

import com.quant.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of EventBus.
 * Handlers are called synchronously on the publishing thread.
 * Each EventType dispatches independently.
 */
public class InMemoryEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);

    private final Map<EventType, List<EventHandler>> subscribers;
    private volatile boolean running;

    public InMemoryEventBus() {
        this.subscribers = new ConcurrentHashMap<>();
        this.running = true;
    }

    @Override
    public void publish(Event event) {
        if (!running) return;
        List<EventHandler> handlers = subscribers.get(event.eventType());
        if (handlers == null || handlers.isEmpty()) return;

        for (EventHandler handler : handlers) {
            try {
                handler.onEvent(event);
            } catch (Exception e) {
                log.error("Handler failed for event {}: {}", event.eventId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public Disposable subscribe(EventType eventType, EventHandler handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.debug("Subscribed handler for event type: {}", eventType);
        return () -> {
            List<EventHandler> handlers = subscribers.get(eventType);
            if (handlers != null) {
                handlers.remove(handler);
                log.debug("Unsubscribed handler for event type: {}", eventType);
            }
        };
    }

    @Override
    public void shutdown() {
        running = false;
        subscribers.clear();
        log.info("EventBus shut down");
    }

    @Override
    public int pendingCount() {
        return 0; // In-memory bus has no queue; events are dispatched synchronously
    }
}
