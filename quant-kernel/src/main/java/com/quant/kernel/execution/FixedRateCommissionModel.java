package com.quant.kernel.execution;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Side;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Commission = {@code abs(quantity × price) × rate}, rounded once using
 * the configured scale and {@link RoundingMode#HALF_EVEN}.
 */
public class FixedRateCommissionModel implements CommissionModel {

    private final BigDecimal rate;
    private final int scale;

    /**
     * @param rate  commission rate, e.g. 0.001 for 0.1%
     * @param scale number of decimal places for the rounded commission amount
     */
    public FixedRateCommissionModel(BigDecimal rate, int scale) {
        Objects.requireNonNull(rate, "rate must not be null");
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("rate must be >= 0, got " + rate);
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale must be >= 0, got " + scale);
        }
        this.rate = rate;
        this.scale = scale;
    }

    @Override
    public BigDecimal calculate(InstrumentId instrument, Side side,
                                long quantity, BigDecimal price) {
        BigDecimal turnover = price.multiply(BigDecimal.valueOf(quantity));
        return turnover.multiply(rate)
                .setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * The number of decimal places used for commission rounding.
     */
    public int scale() {
        return scale;
    }
}
