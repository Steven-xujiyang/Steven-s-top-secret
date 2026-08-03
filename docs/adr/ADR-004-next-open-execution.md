# ADR-004: Next-Bar-Open Execution Model

**Status:** Accepted
**Date:** 2024-01-02
**Deciders:** quant-platform team

## Context

The simplest execution assumption for daily backtesting is that all orders
created at the close of day D are executed at the open of day D+1.
This eliminates intraday timing complexity while preserving the key
mechanism of price slippage: the execution price (D+1 open) may differ
from the planning price (D close).

## Decision

### Execution policy

Only `NEXT_BAR_OPEN` is supported. Orders carry an `ExecutionPolicy` enum
but no other values are valid in this kernel version.

### Price for execution

The execution price is the opening price of the next trading day, obtained
from the OPEN-phase `MarketSlice`.

### Cash sufficiency

At execution time, buy orders are checked against available cash:
```
requiredCash = quantity × openPrice + commission
```

If `requiredCash > availableCash`, the order is **rejected** (not partially
filled). A warning is recorded in the backtest result.

### Sell enforcement

Sell orders are always executed in full because the `OrderPlanner` already
ensures `sellQuantity ≤ currentPosition`.

### Batch cash tracking

When multiple orders execute in the same OPEN phase, the execution model
tracks a **running cash balance**: sell proceeds from earlier orders in the
batch are available to fund later buys.

## Consequences

- The strategy does not know the next day's open when planning orders.
- Gap opens can cause cash insufficiency and rejected orders.
- No partial fills, no limit orders, no volume constraints in this version.
