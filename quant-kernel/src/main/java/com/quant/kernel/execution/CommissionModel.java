package com.quant.kernel.execution;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Side;

import java.math.BigDecimal;

/**
 * Calculates commission for a single fill.
 *
 * <p>Commission is only charged when a Fill is generated; no Fill → no commission.
 */
@FunctionalInterface
public interface CommissionModel {

    /**
     * Computes commission for a hypothetical fill.
     *
     * @param instrument the instrument
     * @param side       BUY or SELL
     * @param quantity   executed quantity
     * @param price      execution price
     * @return commission amount (may be zero)
     */
    BigDecimal calculate(InstrumentId instrument, Side side, long quantity, BigDecimal price);
}
