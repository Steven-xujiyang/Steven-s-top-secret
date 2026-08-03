package com.quant.kernel.execution;

import com.quant.kernel.accounting.Ledger;
import com.quant.kernel.market.MarketSlice;
import com.quant.kernel.portfolio.Order;
import com.quant.kernel.portfolio.Side;
import com.quant.kernel.time.MarketTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Executes NEXT_BAR_OPEN orders at the open price of the next trading day.
 *
 * <p>For BUY orders: if the required cash (turnover + commission) exceeds
 * available cash, the order is REJECTED with a clear warning.
 * For SELL orders: always executed (the OrderPlanner already enforces no
 * overselling, so quantity ≤ current position).
 */
public class NextBarOpenExecutionModel implements ExecutionModel {

    private final CommissionModel commissionModel;
    private int nextFillSequence = 1;

    public NextBarOpenExecutionModel(CommissionModel commissionModel) {
        this.commissionModel = Objects.requireNonNull(commissionModel);
    }

    @Override
    public ExecutionResult execute(List<Order> orders, MarketSlice slice, Ledger ledger) {
        List<Fill> fills = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        MarketTime fillTime = slice.time();

        // Track running cash within this batch so that sell proceeds
        // from earlier fills are available for later buys in the same batch.
        BigDecimal runningCash = ledger.cash();

        for (Order order : orders) {
            BigDecimal price = slice.price(order.instrument());

            BigDecimal commission = commissionModel.calculate(
                    order.instrument(), order.side(), order.quantity(), price);

            if (order.side() == Side.BUY) {
                BigDecimal cost = price.multiply(BigDecimal.valueOf(order.quantity()))
                        .add(commission);
                if (runningCash.compareTo(cost) < 0) {
                    warnings.add("Rejected BUY " + order.id() + " for "
                            + order.instrument() + ": need " + cost
                            + " but only have " + runningCash
                            + " running cash at " + fillTime);
                    continue;
                }
            }

            Fill fill = new Fill(
                    FillId.fromSequence(nextFillSequence++),
                    order.id(),
                    order.instrument(),
                    order.side(),
                    order.quantity(),
                    price,
                    commission,
                    fillTime);
            fills.add(fill);

            // Update running cash
            runningCash = runningCash.add(fill.cashEffect());
        }

        return new ExecutionResult(fills, warnings);
    }

    /**
     * Resets the internal fill sequence counter (e.g. for a new backtest run).
     */
    public void reset() {
        nextFillSequence = 1;
    }
}
