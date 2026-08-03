package com.quant.kernel.execution;

import com.quant.kernel.accounting.Ledger;
import com.quant.kernel.market.MarketSlice;
import com.quant.kernel.portfolio.Order;

import java.util.List;

/**
 * Executes pending orders against a market slice and returns the resulting fills.
 *
 * <p>Implementations may reject orders (e.g. insufficient cash) by not producing
 * a fill and instead adding a warning to the result.
 */
public interface ExecutionModel {

    /**
     * Result of attempting to execute orders.
     *
     * @param fills    fills that were generated (may be empty)
     * @param warnings human-readable descriptions of rejected orders or other issues
     */
    record ExecutionResult(List<Fill> fills, List<String> warnings) {

        public ExecutionResult {
            fills = List.copyOf(fills);
            warnings = List.copyOf(warnings);
        }

        public static ExecutionResult of(List<Fill> fills, List<String> warnings) {
            return new ExecutionResult(fills, warnings);
        }

        public static ExecutionResult empty() {
            return new ExecutionResult(List.of(), List.of());
        }
    }

    /**
     * Executes pending orders at the market prices visible in {@code slice}.
     *
     * @param orders pending NEXT_BAR_OPEN orders
     * @param slice  the OPEN market slice providing execution prices
     * @param ledger the current ledger (read-only for cash checks)
     * @return execution result with fills and any warnings
     */
    ExecutionResult execute(List<Order> orders, MarketSlice slice, Ledger ledger);
}
