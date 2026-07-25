package com.quant.core.model.order;

import java.time.Instant;
import java.util.UUID;

/**
 * A fill (execution) event: a matched order at a specific price and quantity.
 */
public record Fill(
    String fillId,
    String orderId,
    String symbol,
    double price,
    double quantity,
    double commission,
    double slippage,
    Instant timestamp
) {
    /**
     * Total cost including commission.
     */
    public double totalCost() {
        return price * quantity + commission;
    }

    /**
     * Create a fill from an order execution.
     */
    public static Fill create(String orderId, String symbol, double price, double quantity,
                              double commission, double slippage) {
        return new Fill(
            UUID.randomUUID().toString().substring(0, 12),
            orderId, symbol, price, quantity, commission, slippage, Instant.now()
        );
    }
}
