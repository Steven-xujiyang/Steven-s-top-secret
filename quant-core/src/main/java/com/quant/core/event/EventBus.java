package com.quant.core.event;

/**
 * EventBus interface: publish-subscribe mechanism for event-driven architecture.
 * Implementations handle thread safety and dispatch order.
 */
public interface EventBus {

    /**
     * Publish an event to all registered subscribers of its EventType.
     * Called by the EventLoop during dispatch.
     *
     * @param event the event to publish
     */
    void publish(Event event);

    /**
     * Subscribe a handler to a specific event type.
     *
     * @param eventType the type of event to listen for
     * @param handler   the handler to invoke
     * @return a Disposable that can be used to unsubscribe
     */
    Disposable subscribe(EventType eventType, EventHandler handler);

    /**
     * Shutdown the event bus, drain pending events, and stop subscriber threads.
     */
    void shutdown();

    /**
     * Number of pending (not yet delivered) events.
     */
    int pendingCount();

    /**
     * Functional interface for event handlers.
     */
    @FunctionalInterface
    interface EventHandler {
        void onEvent(Event event);
    }

    /**
     * Handle returned from subscribe() to allow unsubscribing.
     */
    @FunctionalInterface
    interface Disposable {
        void dispose();
    }
}
