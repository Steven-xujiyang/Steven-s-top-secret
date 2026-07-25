package com.quant.engine.broker;

import com.quant.core.model.order.Fill;
import com.quant.core.model.order.Order;

/**
 * Models slippage: the difference between expected execution price and actual fill price.
 */
@FunctionalInterface
public interface SlippageModel {

    /**
     * Apply slippage to an expected price.
     *
     * @param order      the order being filled
     * @param basePrice  the reference price (e.g., next bar's open)
     * @return the adjusted fill price after slippage
     */
    double apply(Order order, double basePrice);

    /**
     * No slippage: fill exactly at the base price.
     */
    static SlippageModel none() {
        return (order, basePrice) -> basePrice;
    }

    /**
     * Fixed percentage slippage (e.g., 0.1% = 0.001).
     * Bad for buy orders (price higher), bad for sell orders (price lower).
     */
    static SlippageModel fixedPercent(double percent) {
        return (order, basePrice) -> {
            double slippage = basePrice * percent;
            return switch (order.getSide()) {
                case BUY -> basePrice + slippage;
                case SELL, SHORT -> basePrice - slippage;
                case COVER -> basePrice + slippage;
            };
        };
    }
}
