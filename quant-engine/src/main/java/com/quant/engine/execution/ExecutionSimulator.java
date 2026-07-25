package com.quant.engine.execution;

import com.quant.core.model.market.Bar;
import com.quant.core.model.order.Fill;
import com.quant.core.model.order.Order;
import com.quant.engine.broker.SimpleBroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates order execution by matching pending orders against bar data.
 * Orders are evaluated against each new bar to determine if they fill.
 */
public class ExecutionSimulator {

    private static final Logger log = LoggerFactory.getLogger(ExecutionSimulator.class);

    private final SimpleBroker broker;

    public ExecutionSimulator(SimpleBroker broker) {
        this.broker = broker;
    }

    /**
     * Try to execute all active orders against the current bar.
     * Uses the next bar's OHLC data for fill simulation.
     *
     * @param activeOrders currently active orders
     * @param bars         current bar data for all symbols
     * @return list of fills generated
     */
    public List<Fill> execute(List<Order> activeOrders, List<Bar> bars) {
        List<Fill> fills = new ArrayList<>();

        for (Order order : activeOrders) {
            if (!order.isActive()) continue;

            Bar bar = findBar(order.getSymbol(), bars);
            if (bar == null) continue;

            Fill fill = broker.tryFill(order, bar);
            if (fill != null) {
                order.addFill(fill.quantity());
                fills.add(fill);
                log.debug("Filled {} {} {}@{} commission={}",
                    order.getSide(), order.getSymbol(), fill.quantity(),
                    fill.price(), fill.commission());
            }
        }
        return fills;
    }

    private Bar findBar(String symbol, List<Bar> bars) {
        return bars.stream()
            .filter(b -> b.symbol().equals(symbol))
            .findFirst()
            .orElse(null);
    }

    public SimpleBroker getBroker() { return broker; }
}
