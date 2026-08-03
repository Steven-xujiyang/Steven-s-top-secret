package com.quant.kernel.execution;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.OrderId;
import com.quant.kernel.portfolio.Side;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An immutable fill: the factual record that an order (or part of it) was executed
 * at a specific price, with a specific commission charge.
 *
 * <p>{@code commission} is the actual amount deducted; zero commission is
 * represented as {@code BigDecimal.ZERO} with the appropriate scale.
 */
public record Fill(
        FillId id,
        OrderId orderId,
        InstrumentId instrument,
        Side side,
        long quantity,
        BigDecimal price,
        BigDecimal commission,
        MarketTime filledAt) {

    public Fill {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(instrument, "instrument must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(commission, "commission must not be null");
        Objects.requireNonNull(filledAt, "filledAt must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive, got " + quantity);
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be > 0, got " + price);
        }
        if (commission.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("commission must be >= 0, got " + commission);
        }
    }

    /**
     * Turnover: {@code abs(quantity × price)}.
     */
    public BigDecimal turnover() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Total cash effect (including commission).
     * Positive for sells (cash received minus commission),
     * negative for buys (cash paid plus commission).
     */
    public BigDecimal cashEffect() {
        BigDecimal turnover = turnover();
        return side == Side.BUY
                ? turnover.add(commission).negate()
                : turnover.subtract(commission);
    }
}
