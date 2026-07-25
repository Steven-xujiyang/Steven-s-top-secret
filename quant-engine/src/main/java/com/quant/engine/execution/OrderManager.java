package com.quant.engine.execution;

import com.quant.common.enums.OrderSide;
import com.quant.common.enums.OrderStatus;
import com.quant.core.model.order.Order;
import com.quant.core.model.portfolio.Portfolio;
import com.quant.core.model.signal.Signal;
import com.quant.engine.broker.SimpleBroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the order lifecycle: creation, validation, status tracking.
 * Converts signals into orders using position sizing rules.
 */
public class OrderManager {

    private static final Logger log = LoggerFactory.getLogger(OrderManager.class);

    private final Map<String, Order> activeOrders;
    private final OrderValidator validator;

    public OrderManager() {
        this.activeOrders = new ConcurrentHashMap<>();
        this.validator = new OrderValidator();
    }

    /**
     * Generate orders from a list of signals.
     * A signal with strength S means "allocate S fraction of capital to this symbol".
     */
    public List<Order> generateOrders(List<Signal> signals, Portfolio portfolio,
                                       Map<String, Double> currentPrices) {
        List<Order> orders = new ArrayList<>();

        for (Signal signal : signals) {
            double price = currentPrices.getOrDefault(signal.symbol(), 0.0);
            if (price <= 0) {
                log.warn("No current price for {}, skipping signal", signal.symbol());
                continue;
            }

            Order order = createOrderFromSignal(signal, portfolio, price);
            if (order != null) {
                try {
                    validator.validate(order, portfolio, price);
                    activeOrders.put(order.getOrderId(), order);
                    orders.add(order);
                    log.debug("Generated order: {} {} {} @ {}", order.getSide(), order.getSymbol(),
                        order.getQuantity(), price);
                } catch (Exception e) {
                    log.warn("Order validation failed for {}: {}", signal.symbol(), e.getMessage());
                }
            }
        }
        return orders;
    }

    private Order createOrderFromSignal(Signal signal, Portfolio portfolio, double price) {
        double availableCapital = portfolio.availableCash();
        double currentNav = portfolio.nav();

        return switch (signal.direction()) {
            case LONG -> {
                double allocation = availableCapital * signal.strength();
                double qty = Math.floor(allocation / price / 100) * 100; // round to lot size (100 for A-share)
                if (qty >= 100) {
                    yield Order.marketBuy(signal.symbol(), qty);
                }
                yield null;
            }
            case FLAT -> {
                var position = portfolio.getPosition(signal.symbol());
                if (position != null && !position.isFlat()) {
                    double flatQty = Math.abs(position.getQuantity());
                    if (flatQty > 0) {
                        yield position.direction() > 0
                            ? Order.marketSell(signal.symbol(), flatQty)
                            : Order.marketBuy(signal.symbol(), flatQty); // cover short
                    }
                }
                yield null;
            }
            case SHORT -> {
                double allocation = availableCapital * signal.strength() * 0.5; // 50% margin
                double qty = Math.floor(allocation / price / 100) * 100;
                if (qty >= 100) {
                    yield new Order(signal.symbol(), OrderSide.SHORT,
                        com.quant.common.enums.OrderType.MARKET, qty, 0);
                }
                yield null;
            }
        };
    }

    /**
     * Cancel all active orders.
     */
    public List<Order> cancelAll() {
        List<Order> cancelled = new ArrayList<>();
        for (Order order : activeOrders.values()) {
            if (order.isActive()) {
                order.updateStatus(OrderStatus.CANCELLED);
                cancelled.add(order);
            }
        }
        activeOrders.clear();
        return cancelled;
    }

    /**
     * Cancel a specific order by ID.
     */
    public boolean cancelOrder(String orderId) {
        Order order = activeOrders.get(orderId);
        if (order != null && order.isActive()) {
            order.updateStatus(OrderStatus.CANCELLED);
            activeOrders.remove(orderId);
            return true;
        }
        return false;
    }

    /**
     * Get active (unfilled, uncancelled) orders.
     */
    public List<Order> getActiveOrders() {
        return activeOrders.values().stream()
            .filter(Order::isActive)
            .toList();
    }

    public Order getOrder(String orderId) {
        return activeOrders.get(orderId);
    }
}
