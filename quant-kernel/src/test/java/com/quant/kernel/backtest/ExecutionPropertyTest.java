package com.quant.kernel.backtest;

import com.quant.kernel.execution.Fill;
import com.quant.kernel.portfolio.Order;
import com.quant.kernel.portfolio.Side;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test: fill never precedes its order, and fill quantity
 * never exceeds the order quantity.
 */
class ExecutionPropertyTest {

    @Test
    void fillNeverPrecedesOrderAndNeverExceedsQuantity() {
        RunSpec spec = GoldenFixture.goldenRunSpec("exec-prop");
        BacktestEngine engine = new BacktestEngine();
        BacktestResult result = engine.run(spec);

        // Build order lookup
        Set<String> orderIds = new HashSet<>();
        for (Order o : result.orders()) {
            orderIds.add(o.id().value());
        }

        for (Fill fill : result.fills()) {
            // Fill must reference a known order
            assertThat(orderIds)
                    .as("Fill %s references unknown order %s", fill.id(), fill.orderId())
                    .contains(fill.orderId().value());

            // Fill date must be strictly after order creation date
            // (order created at CLOSE, fill at next OPEN)
            Order matchedOrder = result.orders().stream()
                    .filter(o -> o.id().equals(fill.orderId()))
                    .findFirst().orElseThrow();

            assertThat(fill.filledAt().tradingDate())
                    .as("Fill %s must not precede order %s creation date",
                            fill.id(), fill.orderId())
                    .isAfter(matchedOrder.createdAt().tradingDate());

            // Fill quantity never exceeds order quantity
            assertThat(fill.quantity())
                    .as("Fill %s quantity %d > order %s quantity %d",
                            fill.id(), fill.quantity(), fill.orderId(), matchedOrder.quantity())
                    .isLessThanOrEqualTo(matchedOrder.quantity());

            // Fill side matches order side
            assertThat(fill.side())
                    .as("Fill %s side doesn't match order %s", fill.id(), fill.orderId())
                    .isEqualTo(matchedOrder.side());
        }
    }
}
