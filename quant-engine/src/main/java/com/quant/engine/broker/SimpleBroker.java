package com.quant.engine.broker;

import com.quant.core.model.market.Bar;
import com.quant.core.model.order.Fill;
import com.quant.core.model.order.Order;

/**
 * Simulated broker that matches orders against bar data.
 * Applies slippage and commission models to generate fills.
 */
public class SimpleBroker {

    private final SlippageModel slippageModel;
    private final CommissionModel commissionModel;

    public SimpleBroker(SlippageModel slippageModel, CommissionModel commissionModel) {
        this.slippageModel = slippageModel;
        this.commissionModel = commissionModel;
    }

    /**
     * Create a broker with A-share defaults.
     */
    public static SimpleBroker aShare() {
        return new SimpleBroker(SlippageModel.fixedPercent(0.001), CommissionModel.aShare());
    }

    /**
     * Create a broker with US stock defaults.
     */
    public static SimpleBroker usStock() {
        return new SimpleBroker(SlippageModel.fixedPercent(0.0005), CommissionModel.usStock());
    }

    /**
     * Create a broker with zero slippage and zero commission (for testing).
     */
    public static SimpleBroker zero() {
        return new SimpleBroker(SlippageModel.none(), CommissionModel.zero());
    }

    /**
     * Try to execute an order against a bar.
     *
     * @param order the pending order
     * @param bar   the current bar data
     * @return a Fill if the order was executed, null otherwise
     */
    public Fill tryFill(Order order, Bar bar) {
        if (!order.isActive()) return null;
        if (!order.getSymbol().equals(bar.symbol())) return null;

        double fillPrice;
        double fillQty = order.remainingQuantity();

        switch (order.getType()) {
            case MARKET -> {
                // Market orders fill at open price with slippage
                fillPrice = slippageModel.apply(order, bar.open());
            }
            case LIMIT -> {
                // Limit order: check if price is reachable
                boolean canFill = switch (order.getSide()) {
                    case BUY -> bar.low() <= order.getLimitPrice();
                    case SELL, SHORT -> bar.high() >= order.getLimitPrice();
                    case COVER -> bar.low() <= order.getLimitPrice();
                };
                if (!canFill) return null;
                fillPrice = order.getLimitPrice();
            }
            case STOP -> {
                // Stop order: triggered when price crosses stop level
                boolean triggered = switch (order.getSide()) {
                    case BUY, COVER -> bar.high() >= order.getLimitPrice();
                    case SELL, SHORT -> bar.low() <= order.getLimitPrice();
                };
                if (!triggered) return null;
                fillPrice = slippageModel.apply(order, order.getLimitPrice());
            }
            default -> {
                return null;
            }
        }

        double commission = commissionModel.calculate(order, fillPrice, fillQty);
        double slippageCost = Math.abs(fillPrice - bar.open()) * fillQty;

        return Fill.create(order.getOrderId(), order.getSymbol(), fillPrice, fillQty,
            commission, slippageCost);
    }

    public SlippageModel getSlippageModel() { return slippageModel; }
    public CommissionModel getCommissionModel() { return commissionModel; }
}
