# ADR-003: Time Semantics — Discrete Phases per Trading Day

**Status:** Accepted
**Date:** 2024-01-02
**Deciders:** quant-platform team

## Context

Daily-frequency backtesting requires a disciplined event ordering to prevent
look-ahead bias. A strategy running at the close of day D must not see day D+1's
data, and an order placed at day D's close must execute at day D+1's open.

## Decision

Each trading day is modeled as two discrete phases:

```
OPEN  — only the day's opening price is visible
CLOSE — the full OHLC bar is visible; strategies may produce targets
```

### Event ordering (fixed, per trading day)

```
OPEN
  1. Publish open-only MarketSlice
  2. Execute pending NEXT_BAR_OPEN orders at today's open
  3. Apply fills to ledger
  4. Value at open, record snapshot

CLOSE
  5. Publish MarketSlice with today's close
  6. Value at close
  7. Call strategy → StrategyDecision
  8. Convert target weights to orders (wait for next OPEN)
  9. Record end-of-day snapshot
```

### Key invariants

- An order created at CLOSE(D) cannot fill at CLOSE(D).
- The first possible fill time is OPEN(D+1).
- Strategy never sees D+1 open/close/high/low.
- Execution at OPEN(D+1) uses D+1's open price, which may differ from D's close.

## Consequences

- Intraday timing (hour/minute/second) is out of scope.
- Partial fills, volume-based execution, and continuous-time models are
  not supported in this kernel version.
