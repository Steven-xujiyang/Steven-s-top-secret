package com.quant.core.event;

import com.quant.common.enums.OrderStatus;
import com.quant.core.model.order.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an order is created or its status changes.
 */
public record OrderEvent(
    String eventId,
    Instant timestamp,
    EventType eventType,
    Order order,
    OrderStatus status
) implements Event {

    public OrderEvent {
        eventType = EventType.ORDER;
    }

    /**
     * Create an OrderEvent.
     */
    public OrderEvent(Instant timestamp, Order order, OrderStatus status) {
        this(UUID.randomUUID().toString().substring(0, 12), timestamp, EventType.ORDER,
             order, status);
    }
}
