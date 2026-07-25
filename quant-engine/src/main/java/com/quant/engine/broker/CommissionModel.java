package com.quant.engine.broker;

import com.quant.core.model.order.Order;

/**
 * Models commission fees for different markets.
 */
@FunctionalInterface
public interface CommissionModel {

    /**
     * Calculate commission for an order fill.
     *
     * @param order      the order being filled
     * @param fillPrice  execution price
     * @param fillQty    executed quantity
     * @return commission amount
     */
    double calculate(Order order, double fillPrice, double fillQty);

    /**
     * Zero commission (for crypto or testing).
     */
    static CommissionModel zero() {
        return (order, price, qty) -> 0.0;
    }

    /**
     * A-Share commission model:
     * - Stamp tax (印花税): 0.05% on sell only
     * - Broker fee (券商佣金): 0.025% (min 5 CNY)
     * - Transfer fee (过户费): 0.002%
     */
    static CommissionModel aShare() {
        return (order, price, qty) -> {
            double turnover = price * Math.abs(qty);
            double commission = 0;

            // Broker fee: 0.025%, min 5 CNY
            double brokerFee = Math.max(turnover * 0.00025, 5.0);
            commission += brokerFee;

            // Stamp tax: 0.05% on sell only
            if (order.getSide().name().contains("SELL")) {
                commission += turnover * 0.0005;
            }

            // Transfer fee: 0.002%
            commission += turnover * 0.00002;

            return commission;
        };
    }

    /**
     * US stock commission model:
     * - $0.005 per share (or $0 flat for commission-free brokers)
     * - SEC fee: 0.0008% on sell only
     */
    static CommissionModel usStock() {
        return (order, price, qty) -> {
            double shares = Math.abs(qty);
            double commission = shares * 0.005; // per share

            // SEC fee on sells
            if (order.getSide().name().contains("SELL")) {
                commission += price * shares * 0.000008;
            }

            return commission;
        };
    }

    /**
     * Fixed percentage commission.
     */
    static CommissionModel fixedPercent(double percent) {
        return (order, price, qty) -> price * Math.abs(qty) * percent;
    }
}
