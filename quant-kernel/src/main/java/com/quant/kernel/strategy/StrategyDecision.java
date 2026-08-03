package com.quant.kernel.strategy;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Output of a {@link Strategy} at a point in time.
 *
 * <p>Contains the recommended target portfolio weights. A NOOP decision
 * signals that no change is desired.
 */
public class StrategyDecision {

    private static final StrategyDecision NOOP =
            new StrategyDecision(null, "NOOP");

    private final TargetPortfolio targetPortfolio;
    private final String reason;

    private StrategyDecision(TargetPortfolio targetPortfolio, String reason) {
        this.targetPortfolio = targetPortfolio;
        this.reason = reason;
    }

    /**
     * Creates a decision with the given target portfolio.
     */
    public static StrategyDecision of(TargetPortfolio targetPortfolio) {
        Objects.requireNonNull(targetPortfolio, "targetPortfolio must not be null");
        return new StrategyDecision(targetPortfolio, null);
    }

    /**
     * Creates a decision with the given target portfolio and a human-readable reason.
     */
    public static StrategyDecision of(TargetPortfolio targetPortfolio, String reason) {
        Objects.requireNonNull(targetPortfolio, "targetPortfolio must not be null");
        return new StrategyDecision(targetPortfolio, reason);
    }

    /**
     * No action — keep current positions.
     */
    public static StrategyDecision noop() {
        return NOOP;
    }

    public boolean isNoop() {
        return this == NOOP;
    }

    public TargetPortfolio targetPortfolio() {
        if (isNoop()) {
            return null;
        }
        return targetPortfolio;
    }

    public String reason() {
        return reason;
    }
}
