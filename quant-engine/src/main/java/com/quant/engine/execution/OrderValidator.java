package com.quant.engine.execution;

import com.quant.common.enums.OrderSide;
import com.quant.common.exception.InsufficientCapitalException;
import com.quant.core.model.order.Order;
import com.quant.core.model.portfolio.Portfolio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates orders against portfolio constraints before submission:
 * - Sufficient capital for buy orders
 * - Sufficient position for sell orders
 * - Position limits
 */
public class OrderValidator {

    private static final Logger log = LoggerFactory.getLogger(OrderValidator.class);

    private double maxPositionRatio = 0.3;  // max 30% in single position
    private double minOrderValue = 1000;     // minimum order value in base currency

    /**
     * Validate an order against the current portfolio.
     *
     * @param order     the order to validate
     * @param portfolio current portfolio state
     * @param price     estimated execution price
     * @throws InsufficientCapitalException if insufficient capital or position
     */
    public void validate(Order order, Portfolio portfolio, double price) {
        double orderValue = price * order.getQuantity();
        double commissionEstimate = orderValue * 0.001; // rough estimate

        switch (order.getSide()) {
            case BUY -> {
                double required = orderValue + commissionEstimate;
                if (required > portfolio.availableCash()) {
                    throw new InsufficientCapitalException(portfolio.availableCash(), required);
                }
                // Check position size limit
                double navAfter = portfolio.nav() + orderValue;
                double maxPositionValue = navAfter * maxPositionRatio;
                if (orderValue > maxPositionValue) {
                    throw new InsufficientCapitalException(maxPositionValue, orderValue);
                }
            }
            case SELL -> {
                var position = portfolio.getPosition(order.getSymbol());
                if (position == null || position.getQuantity() < order.getQuantity()) {
                    throw new InsufficientCapitalException(
                        position != null ? position.getQuantity() : 0, order.getQuantity());
                }
            }
            case SHORT -> {
                // Short selling requires margin; simplified check
                double margin = orderValue * 0.5; // 50% margin requirement
                if (margin > portfolio.availableCash()) {
                    throw new InsufficientCapitalException(portfolio.availableCash(), margin);
                }
            }
            case COVER -> {
                var position = portfolio.getPosition(order.getSymbol());
                if (position == null || position.getQuantity() > -order.getQuantity()) {
                    throw new InsufficientCapitalException(
                        position != null ? Math.abs(position.getQuantity()) : 0,
                        Math.abs(order.getQuantity()));
                }
            }
        }

        // Minimum order value check
        if (orderValue < minOrderValue) {
            log.debug("Order value {} below minimum {}", orderValue, minOrderValue);
        }
    }

    public void setMaxPositionRatio(double maxPositionRatio) { this.maxPositionRatio = maxPositionRatio; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
}
