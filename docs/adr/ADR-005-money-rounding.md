# ADR-005: Money Rounding — BigDecimal with HALF_EVEN

**Status:** Accepted
**Date:** 2024-01-02
**Deciders:** quant-platform team

## Context

Financial calculations must use exact decimal arithmetic with well-defined
rounding rules. Using `double` leads to floating-point drift that can cause:
- Equity identity violations (cash + marketValue ≠ equity).
- Non-deterministic results across platforms.
- Penny-off errors that accumulate over thousands of trades.

## Decision

### Type

All monetary amounts (cash, equity, commission, market value) and prices
use `java.math.BigDecimal`.

### Scale

- **Money scale:** 2 decimal places (cents/pennies).
- **Commission scale:** same as money scale (2 decimal places).
- **Price scale:** not enforced by a global constant; prices are stored
  as provided by the data feed. Scale of price × quantity products is
  determined at calculation time.

### Rounding mode

`RoundingMode.HALF_EVEN` (banker's rounding) for all monetary operations.
This is the default in financial systems and minimizes cumulative bias.

### Rounding points

Each monetary operation rounds exactly once:
1. After applying a fill: `cash' = (cash ± turnover ± commission).setScale(2, HALF_EVEN)`
2. After computing commission: `commission = (turnover × rate).setScale(2, HALF_EVEN)`
3. After computing market value: `mv = Σ(qty × price).setScale(2, HALF_EVEN)`
4. After computing equity: `equity = (cash + mv).setScale(2, HALF_EVEN)`

### Comparison

All `BigDecimal` comparisons use `.compareTo()` (value comparison), not
`.equals()` (scale-sensitive). This is critical for `TargetPortfolio` weights,
where `0.5` and `0.50` must be treated as equal.

## Consequences

- Calculations are deterministic across all platforms.
- Performance is adequate for daily-frequency backtesting (thousands of
  instruments × decades of data). For high-frequency use cases, decimal
  performance must be re-evaluated.
- Changing the scale or rounding mode requires a new ADR and will change
  golden test expectations.
