package com.quant.strategy.factor;

import com.quant.core.model.market.Bar;
import com.quant.core.model.signal.FactorValue;

import java.time.Instant;
import java.util.List;

/**
 * Factor interface: compute a factor value from bar series data.
 */
@FunctionalInterface
public interface Factor {

    /**
     * Calculate the factor value for a symbol at a point in time.
     *
     * @param symbol  instrument symbol
     * @param bars    historical bar series (ordered by timestamp, most recent last)
     * @return factor value result
     */
    FactorValue calculate(String symbol, List<Bar> bars);

    /**
     * Factor name for registration.
     */
    default String name() {
        return getClass().getSimpleName().replace("Factor", "");
    }

    /**
     * Human-readable description.
     */
    default String description() {
        return "";
    }

    /**
     * Category this factor belongs to.
     */
    default FactorCategory category() {
        return FactorCategory.OTHER;
    }

    enum FactorCategory {
        MOMENTUM,
        VALUE,
        VOLATILITY,
        QUALITY,
        TECHNICAL,
        OTHER
    }
}
