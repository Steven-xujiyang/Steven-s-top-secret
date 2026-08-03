package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.execution.Fill;
import com.quant.kernel.portfolio.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * The output of a completed backtest run.
 *
 * <p>Contains ordered lists of all artifacts: orders created, fills generated,
 * daily ledger snapshots, total commission, and any warnings.
 */
public record BacktestResult(
        String runId,
        List<Order> orders,
        List<Fill> fills,
        List<LedgerSnapshot> dailySnapshots,
        BigDecimal totalCommission,
        List<String> warnings) {

    public BacktestResult {
        orders = List.copyOf(orders);
        fills = List.copyOf(fills);
        dailySnapshots = List.copyOf(dailySnapshots);
        warnings = List.copyOf(warnings);
    }

    /**
     * The final equity from the last daily snapshot, or the initial cash
     * if there are no snapshots.
     */
    public BigDecimal finalEquity(BigDecimal initialCash) {
        if (dailySnapshots.isEmpty()) {
            return initialCash;
        }
        return dailySnapshots.get(dailySnapshots.size() - 1).equity();
    }

    /**
     * Number of trades (fills).
     */
    public int tradeCount() {
        return fills.size();
    }
}
