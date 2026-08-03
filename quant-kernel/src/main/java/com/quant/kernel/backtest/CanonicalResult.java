package com.quant.kernel.backtest;

import com.quant.kernel.accounting.LedgerSnapshot;
import com.quant.kernel.accounting.Position;
import com.quant.kernel.execution.Fill;
import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.portfolio.Order;

import java.math.BigDecimal;
import java.util.StringJoiner;

/**
 * Produces a stable, human-readable canonical representation of a backtest result.
 *
 * <p>The output is suitable for deterministic comparison: two runs with the same
 * inputs MUST produce byte-for-byte identical canonical output.
 *
 * <p>The representation does NOT include wall-clock time, machine info, or
 * object addresses. Field order is fixed. Decimals use {@link BigDecimal#toPlainString()}.
 */
public final class CanonicalResult {

    private CanonicalResult() {
        // static utility
    }

    /**
     * Renders a full backtest result as a canonical string.
     */
    public static String render(BacktestResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("runId: ").append(result.runId()).append('\n');
        sb.append("totalCommission: ").append(result.totalCommission().toPlainString()).append('\n');
        sb.append('\n');

        sb.append("=== ORDERS (").append(result.orders().size()).append(") ===\n");
        for (Order o : result.orders()) {
            sb.append(orderLine(o)).append('\n');
        }
        sb.append('\n');

        sb.append("=== FILLS (").append(result.fills().size()).append(") ===\n");
        for (Fill f : result.fills()) {
            sb.append(fillLine(f)).append('\n');
        }
        sb.append('\n');

        sb.append("=== DAILY SNAPSHOTS (").append(result.dailySnapshots().size()).append(") ===\n");
        for (LedgerSnapshot s : result.dailySnapshots()) {
            sb.append(snapshotLine(s)).append('\n');
        }
        sb.append('\n');

        if (!result.warnings().isEmpty()) {
            sb.append("=== WARNINGS (").append(result.warnings().size()).append(") ===\n");
            for (String w : result.warnings()) {
                sb.append("  ").append(w).append('\n');
            }
        }

        return sb.toString();
    }

    private static String orderLine(Order o) {
        return String.format("order %s | %s | %s | qty=%d | created=%s | policy=%s",
                o.id().value(),
                o.instrument().value(),
                o.side(),
                o.quantity(),
                o.createdAt().tradingDate() + " " + o.createdAt().phase(),
                o.executionPolicy());
    }

    private static String fillLine(Fill f) {
        return String.format("fill %s <- %s | %s | %s | qty=%d | price=%s | comm=%s | filled=%s",
                f.id().value(),
                f.orderId().value(),
                f.instrument().value(),
                f.side(),
                f.quantity(),
                f.price().toPlainString(),
                f.commission().toPlainString(),
                f.filledAt().tradingDate() + " " + f.filledAt().phase());
    }

    private static String snapshotLine(LedgerSnapshot s) {
        StringJoiner posJoiner = new StringJoiner(", ");
        for (Position p : s.positions().values()) {
            posJoiner.add(p.instrument().value() + "=" + p.quantity());
        }
        return String.format("snapshot %s | %s | cash=%s | mv=%s | eq=%s | comm=%s | [%s]",
                s.time().tradingDate(),
                s.time().phase(),
                s.cash().toPlainString(),
                s.marketValue().toPlainString(),
                s.equity().toPlainString(),
                s.cumulativeCommission().toPlainString(),
                posJoiner);
    }
}
