package com.quant.kernel.portfolio;

import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.market.MarketSlice;
import com.quant.kernel.strategy.StrategyDecision;
import com.quant.kernel.strategy.TargetPortfolio;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Converts a {@link TargetPortfolio} into a list of {@link Order} instances
 * using the simple algorithm described in PR-001:
 *
 * <ol>
 *   <li>Take equity after close mark-to-market.</li>
 *   <li>{@code targetValue = equity × targetWeight}.</li>
 *   <li>Use the currently-known close price to calculate desired quantity.</li>
 *   <li>{@code targetQuantity = floor(targetValue / close)}.</li>
 *   <li>{@code delta = targetQuantity - currentQuantity}.</li>
 *   <li>Generate BUY/SELL orders for the deltas.</li>
 * </ol>
 *
 * <p>This planner is long-only and never sells more than the current position.
 * It does NOT predict the next day's open price; the execution model handles
 * price gaps (and may reject orders when cash is insufficient).
 */
public class OrderPlanner {

    private int nextSequence = 1;

    /**
     * Plans orders from a strategy decision.
     *
     * @param decision  the strategy's target portfolio (ignored if NOOP)
     * @param slice     the CLOSE market slice (provides close prices)
     * @param equity    current equity (cash + marketValue marked at close)
     * @param positions current positions (quantity per instrument)
     * @return ordered list of orders, empty if NOOP or no deltas needed
     */
    public List<Order> plan(StrategyDecision decision,
                            MarketSlice slice,
                            BigDecimal equity,
                            Map<InstrumentId, Long> positions) {

        if (decision.isNoop()) {
            return List.of();
        }

        TargetPortfolio target = decision.targetPortfolio();
        MarketTime decidedAt = target.decidedAt();

        List<Order> orders = new ArrayList<>();
        Set<InstrumentId> allInstruments = new TreeSet<>();
        allInstruments.addAll(target.weights().keySet());
        allInstruments.addAll(positions.keySet());

        for (InstrumentId instrument : allInstruments) {
            BigDecimal weight = target.weight(instrument);
            BigDecimal targetValue = equity.multiply(weight);
            BigDecimal close = slice.price(instrument);
            long currentQty = positions.getOrDefault(instrument, 0L);

            // targetQuantity = floor(targetValue / close)
            long targetQty = targetValue.divide(close, 0, RoundingMode.FLOOR).longValue();

            long delta = targetQty - currentQty;

            if (delta == 0) {
                continue;
            }

            if (delta > 0) {
                orders.add(new Order(
                        OrderId.fromSequence(nextSequence++),
                        instrument, Side.BUY, delta, decidedAt,
                        ExecutionPolicy.NEXT_BAR_OPEN));
            } else {
                // delta < 0: sell, but never more than current position
                long sellQty = Math.min(-delta, currentQty);
                if (sellQty > 0) {
                    orders.add(new Order(
                            OrderId.fromSequence(nextSequence++),
                            instrument, Side.SELL, sellQty, decidedAt,
                            ExecutionPolicy.NEXT_BAR_OPEN));
                }
            }
        }

        // Stable ordering: by (instrumentId.value, orderSequence) — TreeSet iteration
        // already gives us instrument ordering, and we generate one order per instrument
        return orders;
    }

    /**
     * Resets the internal sequence counter (e.g. for a new backtest run).
     */
    public void reset() {
        nextSequence = 1;
    }
}
