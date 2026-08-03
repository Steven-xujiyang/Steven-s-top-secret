package com.quant.kernel.backtest;

import com.quant.kernel.execution.CommissionModel;
import com.quant.kernel.execution.ExecutionModel;
import com.quant.kernel.market.InstrumentId;
import com.quant.kernel.market.MarketDataFeed;
import com.quant.kernel.strategy.Strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Immutable specification for a single deterministic backtest run.
 *
 * <p>All fields are required. Two {@code RunSpec} instances with identical
 * field values produce identical backtest outputs (determinism).
 */
public record RunSpec(
        String runId,
        BigDecimal initialCash,
        BigDecimal commissionRate,
        int moneyScale,
        RoundingMode roundingMode,
        List<InstrumentId> instruments,
        LocalDate startDate,
        LocalDate endDate,
        MarketDataFeed dataFeed,
        Strategy strategy) {

    public RunSpec {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(initialCash, "initialCash must not be null");
        Objects.requireNonNull(commissionRate, "commissionRate must not be null");
        Objects.requireNonNull(roundingMode, "roundingMode must not be null");
        Objects.requireNonNull(instruments, "instruments must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        Objects.requireNonNull(dataFeed, "dataFeed must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");

        if (initialCash.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialCash must be > 0");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("commissionRate must be >= 0");
        }
        if (moneyScale < 0) {
            throw new IllegalArgumentException("moneyScale must be >= 0");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate " + startDate + " after endDate " + endDate);
        }

        instruments = List.copyOf(instruments);
    }
}
