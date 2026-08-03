package com.quant.kernel.strategy;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.market.MarketSlice;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A strategy that returns pre-scheduled target weights on specific dates,
 * and NOOP on all other dates.
 *
 * <p>This is a test strategy designed for deterministic golden-fixture verification.
 * It does not compute factors, signals, or optimization — it simply looks up
 * the target for the current trading date.
 */
public class ScheduledTargetStrategy implements Strategy {

    private final SortedMap<MarketTime, Map<InstrumentId, BigDecimal>> schedule;

    /**
     * @param schedule map from decision time to target weights;
     *                 each entry's weights must satisfy {@link TargetPortfolio} constraints
     */
    public ScheduledTargetStrategy(Map<MarketTime, Map<InstrumentId, BigDecimal>> schedule) {
        Objects.requireNonNull(schedule, "schedule must not be null");
        this.schedule = new TreeMap<>(schedule);
    }

    @Override
    public StrategyDecision decide(StrategyContext context, MarketSlice slice) {
        MarketTime now = slice.time();
        Map<InstrumentId, BigDecimal> weights = schedule.get(now);
        if (weights == null) {
            return StrategyDecision.noop();
        }
        TargetPortfolio target = new TargetPortfolio(now, weights);
        return StrategyDecision.of(target);
    }
}
