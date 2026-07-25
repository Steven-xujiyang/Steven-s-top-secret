package com.quant.core.model.order;

import com.quant.common.enums.OrderSide;
import com.quant.common.enums.OrderStatus;
import com.quant.common.enums.OrderType;

import java.time.Instant;
import java.util.UUID;

/**
 * An order sent to the simulated broker.
 */
public class Order {

    private final String orderId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final double quantity;
    private final double limitPrice;
    private OrderStatus status;
    private double filledQuantity;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(String symbol, OrderSide side, OrderType type, double quantity, double limitPrice) {
        this.orderId = UUID.randomUUID().toString().substring(0, 12);
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.status = OrderStatus.CREATED;
        this.filledQuantity = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Create a market buy order.
     */
    public static Order marketBuy(String symbol, double quantity) {
        return new Order(symbol, OrderSide.BUY, OrderType.MARKET, quantity, 0);
    }

    /**
     * Create a market sell order.
     */
    public static Order marketSell(String symbol, double quantity) {
        return new Order(symbol, OrderSide.SELL, OrderType.MARKET, quantity, 0);
    }

    /**
     * Create a limit buy order.
     */
    public static Order limitBuy(String symbol, double quantity, double price) {
        return new Order(symbol, OrderSide.BUY, OrderType.LIMIT, quantity, price);
    }

    /**
     * Create a limit sell order.
     */
    public static Order limitSell(String symbol, double quantity, double price) {
        return new Order(symbol, OrderSide.SELL, OrderType.LIMIT, quantity, price);
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void addFill(double qty) {
        this.filledQuantity += qty;
        this.updatedAt = Instant.now();
        if (this.filledQuantity >= this.quantity) {
            this.status = OrderStatus.FILLED;
        } else if (this.filledQuantity > 0) {
            this.status = OrderStatus.PARTIAL_FILLED;
        }
    }

    public double remainingQuantity() {
        return quantity - filledQuantity;
    }

    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }

    public boolean isActive() {
        return status == OrderStatus.CREATED || status == OrderStatus.SUBMITTED || status == OrderStatus.PARTIAL_FILLED;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public OrderType getType() { return type; }
    public double getQuantity() { return quantity; }
    public double getLimitPrice() { return limitPrice; }
    public OrderStatus getStatus() { return status; }
    public double getFilledQuantity() { return filledQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
