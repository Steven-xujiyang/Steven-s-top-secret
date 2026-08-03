# Combining Alphas: Theory, Architecture, and Critique of a Composite-Score Pipeline

*An academic essay on multi-factor alpha combination, written against the implementation of a quantitative backtesting platform.*

*Equations are typeset in LaTeX math (rendered by GitHub, VS Code, Typora); the surrounding prose preserves their meaning in plain text.*

---

## Abstract

Alpha combination is usually described as a formula: a weighted sum of predictive signals. This essay argues that it is better understood as an architecture, a chain of decisions running from signal abstraction, through standardization and missing-data handling, to weighting, optimization, and portfolio construction, where each link can silently destroy the value created by the others. The argument is made against a real codebase, a Java backtesting platform whose factor interface, scoring engine, ranking strategy, deterministic kernel, and parameter optimizers provide a concrete substrate for the theory. Reading theory through implementation yields three findings: the data model anticipates cross-sectional standardization that the aggregation path never applies; missing-data renormalization makes effective factor weights symbol- and date-dependent; and the platform's determinism discipline, which is the right prerequisite for measurable weight selection, is enforced for only a subset of the pipeline. The essay hedges the severity of these findings where they remain untested, distinguishes alpha signals from factor risk premia, and closes with a prioritized research agenda. Its contribution is architectural analysis, not measured effects.

**Keywords:** alpha combination, factor models, multi-factor scoring, information coefficient, data snooping, backtest overfitting, deterministic backtesting

---

## 1. Introduction

A single predictive signal, a momentum score, a value ratio, a short-horizon mean-reversion trigger, is a fragile basis for investing. It decays, it is crowded, and its realized information is small relative to its noise. The standard institutional response is to combine many signals into a single view, on the intuition that return streams which are individually weak but imperfectly correlated can, when aggregated, produce something stronger and smoother than any of its parts. This practice, called *alpha combination*, *signal blending*, or *multi-factor scoring*, is the subject of this essay.

The argument advanced here is that alpha combination is best understood not as a single aggregation step but as an architecture, a chain of decisions running from signal abstraction, through standardization and missing-data handling, to weighting, optimization, and finally portfolio construction. Each link in the chain can quietly destroy the value created by the others. To make this concrete, the essay examines the composite-alpha pipeline of a real quantitative backtesting platform. The platform defines a factor interface, a registry, a weighted scoring engine, a ranking strategy, a deterministic backtesting kernel, and a pair of parameter optimizers. Reading the theory through the implementation yields three findings. First, the data model anticipates cross-sectional standardization (z-score and percentile fields exist on the factor-value type) that the aggregation path never applies, so the combination's effective weights are opaque. Second, the engine's missing-data handling is robust, but the renormalization it performs silently makes effective factor weights symbol- and date-dependent. Third, the platform's determinism discipline, which is exactly the right prerequisite for measurable weight selection, is currently enforced for only the kernel module, while the research pipeline around it leaks non-determinism in several places. The severity of the first finding is partially untested, and the essay says so explicitly.

The essay proceeds as follows. Section 2 states the economic case for combining alphas and distinguishes two senses of "alpha." Section 3 formalizes the weighting problem. Section 4 examines the platform's composite-score pipeline in detail, including a critical reading. Section 5 treats weight selection and the multiple-testing hazard. Section 6 considers risk-aware construction. Section 7 outlines limitations and a prioritized agenda, and Section 8 concludes.

## 2. The Economic Case for Combining Alphas

The quantitative justification for combining signals has two pillars: diversification and the fundamental law of active management.

**Diversification of return streams.** Markowitz (1952) showed that combining assets whose returns are imperfectly correlated reduces portfolio variance for a given expected return. Alpha signals can be treated the same way. Grinold and Kahn (2000) explicitly model active bets as assets and apply mean-variance logic to their allocation. If we have N alphas with equal information ratio (IR), equal volatility, and a common pairwise correlation ρ, and we weight them equally, the combined information ratio is approximately

$$\text{IR}_{\text{comb}} \approx \text{IR}\cdot\sqrt{\frac{N}{1+(N-1)\rho}}$$

a standard result of the signal-combination literature (Qian, Hua, and Sorensen, 2007). The equal-volatility assumption matters: with unequal variances the closed form does not reduce so cleanly, which is why the relation is approximate.

This formula is instructive in two directions. When alphas are independent (ρ = 0), the combined IR grows as √N, the textbook diversification dividend. But as ρ rises, the dividend collapses; at ρ = 1 the combination confers nothing. Two corollaries follow. First, the *count* of alphas matters far less than their *correlation structure*: five weakly correlated signals can dominate fifty near-duplicates. Second, naive addition of alphas that all load on the same latent driver (for example, momentum variants over different windows) overstates breadth. The practical discipline is to combine across *families* of signals, momentum, value, quality, volatility, which is precisely the taxonomy the platform encodes in its `Factor.FactorCategory` enum. The canonical evidence that distinct families carry distinct return streams is the momentum anomaly of Jegadeesh and Titman (1993) and the factor structure of Fama and French (1993).

**The fundamental law of active management.** Grinold (1989) derived that the information ratio of a strategy is well approximated by the product of its skill per forecast and the square root of its breadth:

$$\text{IR} \approx \text{IC}\cdot\sqrt{\text{BR}}$$

where IC is the information coefficient, the cross-sectional correlation between forecasts and subsequent returns, and BR is breadth, the number of independent bets per year. This is an approximation under idealized conditions (independent bets, linear forecasts, no costs), not a bound, and the phrase "is approximated by" should be read with that caveat. Combining alphas raises effective breadth, but only insofar as the signals are not redundant; overlapping signals inflate the naive BR while contributing little genuine breadth. The fundamental law thus restates the correlation result of the previous paragraph in decision-theoretic terms: the value of a combination resides in the *independent* information each component contributes.

**A necessary distinction: alpha versus factor premia.** Before proceeding, one terminological point must be settled, because it bears on everything that follows. In the Grinold-Kahn framework, an *alpha* is a forecast of residual return, the component of expected return that is not compensation for priced risk; combination is IC-weighted aggregation of such forecasts. In the Fama-French tradition, by contrast, a long-short book that holds systematic exposures (value, momentum, low volatility) earns *factor risk premia*, compensation for priced risk, with a different econometrics (the factors are mean-variance rewards, not unpriced skill) and different combination logic (correlation structure of exposures rather than IC-weighted residual forecasts). The platform's ranking strategy is closer to the second kind: it builds a factor portfolio, not a portfolio of residual forecasts. The essay uses "alpha combination" in a deliberately loose sense to cover both, because the architectural argument it makes, that careful combination is a chain of decisions, applies to either. But the distinction matters for any claim about what combination achieves, and it is flagged here so that the reader can hold the two apart.

Together, these results give the field its working assumption: build many uncorrelated, individually weak signals, then combine them carefully. The remainder of this essay is about the word "carefully."

## 3. The Weighting Problem in Theory

Given a set of factor values f_{j,i} for factor j and symbol i, the composite score S_i is most commonly a weighted sum. The theoretical question is how to set the weights w_j.

**Equal weighting.** The simplest scheme sets w_j = 1 for all j. It is remarkably defensible: DeMiguel, Garlappi, and Uppal (2009) found that the naive 1/N portfolio, computed with no estimation at all, often outperforms optimized portfolios out of sample because it avoids estimation error entirely. The same argument applies to alpha weights: equal weighting is the zero-risk default against which all sophistication must be justified.

**IC weighting.** Grinold and Kahn (2000) show that the optimal weight on a signal is proportional to its IC divided by its specific risk. Their derivation is worth making explicit, because the compact form conceals a step. Under their alpha model, a standardized signal z_j maps to a forecast alpha_j = IC_j · σ_{spec,j} · z_j, and the optimal asset weight is proportional to alpha divided by residual variance, alpha_j / σ²_{spec,j}. Substituting the alpha model for a standardized signal gives

$$w_j \propto \frac{\text{IC}_j}{\sigma_{\text{spec},j}}$$

In words: weight alphas by their quality per unit of residual noise, not by taste (Grinold and Kahn, 2000; Qian, Hua, and Sorensen, 2007). In practice, IC estimates are themselves noisy, so implementations shrink them toward the equal-weight benchmark or use rank-based versions.

**Correlation-aware weighting.** In the full mean-variance treatment, weights are not set signal-by-signal but jointly, inverting the covariance matrix of alpha returns. The benefit of accounting for correlation is real but fragile: covariance estimation error can make the optimized weights worse than equal weights, the same DeMiguel et al. result at a different level. The literature's consensus is pragmatic: weighting should reflect signal quality, should be shrunk toward simplicity, and should never be trusted to second decimals derived from a single backtest.

One further distinction belongs here. The IC in the fundamental law is a *cross-sectional* correlation, computed at each rebalance across the cross-section of securities, and it is the quantity that should drive both factor evaluation and the monitor proposed in Section 7. It differs from a *time-series* IC, the per-security correlation between a signal and its own subsequent returns, which answers a different question (does this signal predict this name over time). A factor can score on one and not the other, so the distinction is not pedantic.

These theoretical distinctions matter because they set the benchmark against which the platform's actual combination, examined next, can be judged.

## 4. A Composite-Alpha Pipeline: Case Study

The platform under study implements alpha combination across four layers: a signal abstraction (`Factor`), a scoring engine (`FactorScore`), a trading rule (`FactorRankingStrategy`), and a determinism guarantee (the pure-Java kernel of ADR-002, the platform's Architecture Decision Record). Each layer is described, then read critically.

### 4.1 The Signal Abstraction

The `Factor` interface is deliberately minimal: a single functional method,

```java
FactorValue calculate(String symbol, List<Bar> bars)
```

which maps a symbol's price history to a value. Concrete factors are parametric, `MomentumFactor(20)`, `MACDFactor(12, 26, 9)`, `RSIFactor(14)`, `MAFactor(20)`, so the same interface spans very different signal families. A `FactorRegistry` holds instances in a `ConcurrentHashMap` keyed by name, with lookup by category. The design is a small plugin architecture in the sense of Gamma, Helm, Johnson, and Vlissides (1994): research produces a factor, registers it, and the strategy layer consumes it through the interface without knowing its implementation. This decoupling is not cosmetic. It is what makes "combining alphas" a *composition* problem rather than a *rewrite* problem, a new signal enters the combination by implementing one method, and the aggregation logic is untouched.

The `FactorValue` record deserves attention, because it encodes the platform's *intended* model of a normalized signal:

```java
public record FactorValue(String factorName, String symbol, Instant timestamp,
                          double rawValue, Double zScore, Double percentile)
```

The presence of `zScore` and `percentile` fields, populated by a factory method `FactorValue.of(...)`, shows that the designers recognized standardization as first-class: raw values from different factors live on different scales and must be brought to a common footing before they can be summed. This is the correct instinct, and it is the platform's own data model that raises the bar for what follows.

### 4.2 Standardization: Anticipated but Bypassed

The critical observation about this pipeline is that the standardization the data model anticipates is never applied. Every concrete factor, `MomentumFactor`, `MACDFactor`, `RSIFactor`, and `MAFactor`, constructs its result via `FactorValue.raw(...)`, leaving `zScore` and `percentile` null. The aggregation engine, `FactorScore`, then reads only the raw value:

```java
totalScore += fv.rawValue() * weight;
totalWeight += Math.abs(weight);
```

and emits `totalScore / totalWeight` as the composite:

$$S_i=\frac{\sum_j w_j\,f_{j,i}}{\sum_j |w_j|}$$

The consequence is that the composite is a weighted average of raw, differently scaled inputs. Momentum returns a small price return, typically on the order of tenths though unbounded in extremes; RSI returns a value in [0, 100]; the MACD histogram is an unbounded price-scaled difference; and the MA factor returns a normalized deviation from its moving average, on the same ratio scale as momentum. A unit of weight allocated to RSI therefore overwhelms a unit allocated to momentum by orders of magnitude, and the *relative* influence of each factor on the final ranking is set as much by its native variance as by the researcher's intended weight. The denominator Σ|w| normalizes the *weights*; it does nothing to normalize the *factors*. Because the downstream consumer uses only the ordering of composite scores, the failure is consequential: the rank of a weighted sum of unstandardized variables is not invariant under the choice of scale, so the combination's effective weights are opaque.

Two qualifications must be stated, because the severity of this point is contingent on how the weights are chosen. If the weights are set by hand to express intended influence, then raw-scale aggregation makes the realized influence diverge from the intent, and the critique bites directly. If the weights are chosen by an optimizer that searches *on the raw scale* (Section 5), the optimizer can in principle absorb native-scale differences, making a weight of 0.001 on RSI equivalent to 1.0 on momentum for ranking purposes. In that usage the flaw is a representation choice rather than a measured defect, and its practical consequence is an empirical question the platform could settle in minutes: run the scorer on the raw inputs and on rank-standardized inputs, and measure how much the composite ranking diverges. The essay's claim is therefore not that bypassing standardization is always consequential, but that it makes the combination's realized weighting opaque, and that this opacity is an empirical liability the platform has not yet priced. The distinction is drawn explicitly so that the reader does not mistake an architectural analysis for a measured effect.

The fix is well known and is pre-figured in the platform's own `FactorValue` type. Cross-sectional standardization should be applied per period, per factor: z-scoring (or winsorized z-scoring to blunt outliers), percentile or rank transforms, or volatility scaling for price-based factors. Rank transforms are the most robust choice for a ranking strategy, since they make the input distribution irrelevant. The gap here is not ignorance of the concept but a wiring omission: the fields exist, the factory method exists, and the scoring loop simply does not use them.

### 4.3 Robust Aggregation Under Missing Data

Where the scoring engine is genuinely strong is its handling of missing values. When a factor cannot be computed, `MomentumFactor` returns NaN if fewer than `period + 1` bars are available, and `MACDFactor` behaves likewise, the scoring loop skips that factor for that symbol:

```java
if (!Double.isNaN(fv.rawValue())) {
    totalScore += fv.rawValue() * weight;
    totalWeight += Math.abs(weight);
}
```

Skipping the factor and simultaneously subtracting its weight from the denominator means the composite renormalizes over whatever factors are actually present. This is the right semantics for real data: symbols enter the universe at different times, and long-history factors (MACD with a 26+9-bar warm-up) are simply unavailable for young listings. The alternative, imputing a value or zero-weighting such symbols outright, either fabricates information or discards symbols. The platform's choice degrades gracefully, which is precisely what a production pipeline must do.

The subtle consequence is that *effective* factor weights become symbol- and date-dependent: a symbol whose MACD is unavailable is scored on momentum alone. This is acceptable as a default, but it is a hidden asymmetry between intended and realized weights. Two mitigations exist within the essay's own logic. Standardizing each factor over the cross-section of symbols for which it is available makes missingness less able to change a factor's influence, and reporting *effective* weights per rebalance would surface the divergence directly. Either would turn the silent asymmetry into a visible quantity.

A second robustness feature is the guard in `FactorRankingStrategy`: it refuses to trade until *every* symbol has at least `minBars` (default 60) bars of history. This is a coarse but effective way to ensure the cross-section is comparable. Its cost is cold-start latency and a hidden sensitivity to the newest listing.

### 4.4 From Composite Score to Portfolio

The composite score is consumed by `FactorRankingStrategy`, which converts a list of symbols scored by `scoreSymbols`, sorted descending, into a book. It buys the top N and exits the bottom M, with a strength signal that decays linearly with rank for the long side:

```java
double strength = 0.8 * (1.0 - (double) i / topN);
```

Two details deserve precision. First, the strategy is long the top N and *exits* (flattens) the bottom N, not a true long-short book: the `Signal.sell` it emits carries the direction `FLAT`, which the order manager interprets as closing an existing position, and the strategy never issues a short order. The essay therefore speaks of a long/exit book, not a long-short book, and any claim about the strategy inherits that semantics. Second, the book is asymmetric: long positions receive a strength that decays with rank, while exits are signaled uniformly. This is a defensible design, the exit decision is binary once the score falls below the bottom threshold, but the asymmetry is a choice the essay does not defend, and a reviewer would be entitled to ask whether it reflects an economic rationale (for example, short-side constraints) or an accident of implementation.

Rank-based construction has the right invariance property: the book is a function of *relative* score, not absolute level, which is appropriate for a cross-sectional strategy. But the essay must also weigh its cost. Ranking erases the *magnitudes* the composite score just computed, the conviction spread between rank 1 and rank 2, and between rank N and rank N+1, which is precisely the information a careful combination produces. It also has turnover consequences between rebalances that are never discussed. The rank transform trades away magnitude information for robustness to scale; whether that trade is favorable is an empirical question, and the essay's position is that it is a deliberate and unexamined trade, not an unmixed win.

## 5. Weight Selection and the Overfitting Hazard

The composite's weights are the platform's free parameters, and the platform ships two optimizers to search over parameter spaces: `GridSearchOptimizer`, which evaluates every combination exhaustively, and `GeneticOptimizer`, which evolves a population via tournament selection, elitism, uniform crossover, and small mutations, evaluating fitness by running a backtest through `OptimizationExecutor`. Both are sound as search machinery, and genetic search is a reasonable choice when the space is too large to grid. The objective is a choice: grid search requires an explicit `ObjectiveFunction`, while the genetic optimizer's builder defaults to `maximizeSharpe`. One caveat belongs here. The default implementation divides total return by the standard deviation of *per-period* portfolio returns, a non-annualized ratio that does not match the textbook Sharpe ratio its own javadoc claims. Since this is the statistic being selected over, the essay's argument does not depend on it being a proper Sharpe, but the reader should know the fitness function is less standardized than its name implies.

**Data snooping and multiple testing.** Every combination the optimizer tries is a hypothesis test against the same history. Exhaustive grid search makes this explicit: if the space contains K weight vectors, the best observed Sharpe is the maximum of K correlated statistics. The hazard is not new. Lo (2002) showed that the asymptotic distribution of the Sharpe ratio estimator depends on the serial correlation and higher moments of returns, so under non-iid, non-normal returns the estimator is far noisier than its point estimate suggests. The selection problem, that searching many rules against one history manufactures a winner, was established by White (2000) and Sullivan, Timmermann, and White (1999); Bailey and López de Prado (2014) quantified how far a selected maximum can exceed the true Sharpe and proposed the deflated Sharpe ratio as a correction, and Harvey, Liu, and Zhu (2016) documented the same problem across the published factor zoo, arguing for haircuts on t-statistics. The weight of the evidence is unambiguous even where individual studies differ on the remedy. The practical translation: with a grid of thousands of weight vectors, a "great" backtest result is expected by chance, and the winner is likely to be the one that best memorized the sample. How severe this hazard is for this particular platform depends on the actual size of its search space, which the essay does not report, so the claim is stated as a risk to be quantified rather than a measured loss.

**The honest posture.** What is admirable about this platform is that its own kernel documentation states the limits of what the tests prove. PR-001 (the platform's first pull request specification) explicitly lists what the backtest does *not* demonstrate: no guarantee of real-data performance, no protection against survivorship bias, no claim that passing base tests substitutes for economic scrutiny. That epistemic honesty is the correct scientific attitude, and it is rare. The engineering that makes the honest posture *actionable* is the deterministic kernel.

**Determinism as a scientific prerequisite.** ADR-002 mandates a pure-Java kernel whose policy forbids non-deterministic calls, unseeded `Random`, order-dependent `HashMap` iteration, and so on. It is important to be precise about what is and is not enforced. The automated tooling bans *dependencies* and *package dependencies*: the Maven Enforcer rule excludes forbidden artifact coordinates, and the ArchUnit test forbids kernel classes from depending on forbidden packages. Neither mechanism can ban a source-level call such as `Instant.now()`. The call-level bans in ADR-002's policy are enforced by the pull-request reviewer checklist and by a behavioral determinism test that replays a run and compares canonical output. So the essay's earlier claim that "the corresponding test infrastructure enforces it" should be corrected: the policy is comprehensive, the automated enforcement covers dependencies only, and the call-level bans rest on human review plus one behavioral test.

Why does this distinction matter? Because the research pipeline around the kernel is where determinism currently leaks, and the leaks are more numerous and more consequential than the one the first draft highlighted. Three stand out. First, every `Signal` emitted by the ranking strategy embeds `UUID.randomUUID()` and `Instant.now()` in its record, including the signal id and timestamp that flow into the recorder. These are live artifacts, consumed downstream, not dead values. Second, the genetic optimizer constructs `new Random()` unseeded, as does `ParameterSpace.sampleRandom`, which drives population initialization, tournament selection, crossover, and mutation. A genetic search over weights is therefore not reproducible across runs: run the same optimization twice and the fitness landscape explored differs. Third, the concrete factors return `Instant.now()` as the timestamp of NaN values, a line the first draft flagged and which is now best understood as illustrative: dead in effect, since the NaN is discarded downstream, but the visible tip of a pattern the live leaks confirm.

The essay's earlier point stands, and is strengthened by this catalog. Determinism converts backtesting from an anecdote into a repeatable measurement: the same run spec always yields the same canonical result, so a researcher can vary weights and attribute any change in outcome to the weights rather than to run-to-run jitter. But determinism is necessary, not sufficient, for valid weight inference. An in-sample maximization over a deterministic kernel is no less selection-biased than one over a jittery kernel; determinism removes run-to-run noise, not selection bias, and it does nothing for the external validity that only out-of-sample discipline provides. It also has costs: parallel reductions break bit-identical floating-point sums, so the reproducibility ideal trades against throughput, and "the same canonical result" holds only within a fixed build and environment unless dependencies and the JVM are pinned. These limits are worth stating plainly, because the reproducibility literature (Peng, 2011; Sandve et al., 2013) treats reproducibility as a property of the whole computational environment, not of a single module. The platform's discipline is the right first step, and the whole-pipeline enforcement proposed in Section 7 is the natural completion.

## 6. Risk-Aware Construction

Combining alphas determines *what* to bet on; the risk layer determines *how much*, and the platform ships a small but thoughtful risk stack alongside the strategy layer. `PositionSizer` offers three sizing policies, fixed-fraction, volatility targeting (scale positions inversely to 2×ATR), and equal weight, and `RiskLimits` caps position size, sector exposure, drawdown, leverage, and position count, with a conservative preset. VaR calculation is provided in `quant-risk.metrics`.

The relevant theory is that alpha construction and risk construction should be separated, but not hermetically. Clarke, de Silva, and Thorley (2002) showed that portfolio constraints, the very limits `RiskLimits` encodes, degrade the naive realization of the fundamental law. Their refinement is to insert a transfer coefficient into the law,

$$\text{IR} \approx \text{IC}\cdot\sqrt{\text{BR}}\cdot\text{TC}$$

where TC measures how much of the unconstrained alpha is actually transferred into the book through constraints and costs (Clarke, de Silva, and Thorley, 2006). A constrained book captures a lower share of the unconstrained IR. The tension is structural: the composite score says "own the top decile with conviction," while position and exposure caps say "no more than 30% of NAV in one name." Reconciliation is the art of the book. For the platform, the important design point is that the layers are cleanly separated, the strategy emits scored signals, the risk layer sizes and filters them, which is the correct architecture even if the current caps are illustrative rather than calibrated.

One gap should be acknowledged. The essay discusses optimizing the Sharpe ratio without ever naming the benchmark, the cost model, or the rebalance frequency against which the backtest is run. The kernel charges a flat commission and executes at next-open prices with a slippage model in the broader engine, but no benchmark return is specified, and capacity and slippage assumptions can swamp every architectural improvement discussed. For a case study that advertises practical implications, this is a consequential omission. The fitness function is arguably a bigger hazard than the post-hoc deflation recommended in Section 5, because in-sample Sharpe is precisely the quantity most gameable by selection; the essay's position is that fitness should eventually be cost-adjusted, drawdown-aware, and evaluated out of sample.

## 7. Limitations and Future Directions

The pipeline examined here is a first vertical slice, and its gaps are the natural agenda for alpha-combination research on this codebase. The six items below are ordered by value per unit of effort, with the highest-leverage fixes first and one explicit note on what not to build first.

**1. Cross-sectional standardization (highest priority, lowest effort).** The `zScore` and `percentile` fields exist but are unused, and the aggregation reads only `rawValue`. The robust remedy is to standardize per factor, per cross-section, at each rebalance, most safely with a rank transform, which is scale-free and insensitive to outliers, and only then weight:

```java
// Sketch: standardize inside the scorer, not inside each factor.
for (Factor f : factorWeights.keySet()) {
    Map<String, Double> raw = new HashMap<>();
    for (String sym : symbols)
        raw.put(sym, f.calculate(sym, barsOf(sym)).rawValue());
    Map<String, Double> ranks = rankTransform(raw);   // NaNs dropped; ties averaged
    for (String sym : symbols)
        if (ranks.containsKey(sym))
            totalScore += ranks.get(sym) * weightOf(f);
}
// score = Σ_j w_j · rank_j(i) / Σ|w_j|   →   now scale-free
```

One caveat: with NaNs dropped and ties averaged, per-symbol rank denominators become unstable in thin cross-sections, where most factors are unavailable for most symbols, so the sketch should specify a minimum cross-section size, or fall back to equal weights when the valid count is too small. The decisive experiment that should accompany this fix is to measure composite rank divergence between raw and standardized scoring, which settles whether Section 4.2's concern is materially consequential for the platform's actual usage. With rank inputs, a weight finally means what it appears to mean: w_j is the marginal influence of factor j on the composite ordering, independent of the factor's native scale.

**2. Determinism enforcement across the research pipeline (high priority, low effort).** The fix for the three leaks in Section 5 is mechanical. Missing factor values should carry the last bar's timestamp, which the factor already has in hand:

```java
return FactorValue.raw("Momentum", symbol,
    bars.get(bars.size() - 1).timestamp(), Double.NaN);
```

The `Signal` factory should accept an injected id and timestamp instead of calling `UUID.randomUUID()` and `Instant.now()`, and the optimizers should take a seeded `Random` (or a seed in the run spec). Enforcement needs a tool that can actually ban calls: an ArchUnit `noClasses().should().callMethod(...)` rule, or a PMD, Checkstyle, or ErrorProne rule against `Instant.now()`, `new Random()`, and `UUID.randomUUID()`, extended to the `quant-strategy` and `quant-optimizer` modules, not just the kernel. Dependency and JVM pinning completes the reproducibility contract (Peng, 2011; Sandve et al., 2013).

**3. IC tracking and factor diagnostics (high value, medium effort).** Combining alphas without measuring each factor's realized IC, and the correlation structure among factors, is flying blind. The analytics module computes portfolio metrics (Sharpe, drawdown, Calmar, profit factor) but not per-factor IC or a factor correlation matrix, the two numbers that would tell a researcher whether the combination is adding breadth or redundancy. A small monitor closes the gap:

```java
// Sketch: per-rebalance IC of factor j = Spearman correlation(factor_j, forward return).
record FactorIc(String factor, double ic, int obs, int rebalances) {}

FactorIcMonitor monitor = new FactorIcMonitor();
// after each rebalance:
monitor.record(factorValue, forwardReturn);
// report: average IC, IC t-stat, and the factor correlation matrix
```

The measured ICs then feed the weighting of Section 3, as IC-proportional weights, or more conservatively as shrink targets toward the equal-weight benchmark (DeMiguel, Garlappi, and Uppal, 2009).

**4. Walk-forward discipline for weight optimization (high value, medium effort).** Grid and genetic search are embedded in a backtest, but nothing separates fitting from testing. The standard remedy is walk-forward validation: split history into K overlapping folds; on each fold, fit weights on the training window via grid or GA and evaluate only on the following validation window; report the pooled out-of-sample result. Any final weight vector should carry the multiple-testing haircut of Harvey, Liu, and Zhu (2016), or equivalently the deflated Sharpe of Bailey and López de Prado (2014), which the analytics module could compute from the number of trials the optimizer reports. Two cautions: walk-forward parameters are themselves chosen after seeing results, the same hazard one meta-level up, and pooled folds reuse data, so the design must state its selection rules before inspecting the output. A cheaper single-pass alternative is to apply the deflated Sharpe directly to the observed in-sample optimum.

**5. Factor families beyond momentum and technical (medium value, medium effort).** The taxonomy anticipates momentum, value, volatility, quality, and technical signals, but only momentum and technical factors are implemented. The diversification benefit of Section 2 is realizable across families, not within them. A value factor (book-to-price or earnings yield), a quality factor such as gross profitability (Novy-Marx, 2013), and a volatility factor exploiting the low-volatility anomaly (Ang, Hodrick, Xing, and Zhang, 2006) would each add a genuinely distinct return stream. Each also carries a framing obligation flagged in Section 2: these are factor premia, so the essay's combination logic must be clear whether it is blending exposures or residual forecasts.

**6. Machine-learning combination (lower priority, do not build first).** The weighted-sum form is linear; stacking, gradient-boosted trees over factor exposures, and interaction terms are the modern toolkit. The clean seam is a `Combiner` interface between standardization and scoring, preserving the plugin discipline of the factor layer:

```java
// Sketch: a pluggable combiner so aggregation is no longer hard-wired to a sum.
interface Combiner {
    double combine(String symbol, Map<Factor, Double> standardizedValues);
}
// LinearCombiner  →  Σ_j w_j · rank_j      (the current behavior, made explicit)
// TreeCombiner    →  gradient-boosted regression on the rank exposures
```

`FactorScore` would delegate to a `Combiner`, so switching from linear to nonlinear aggregation becomes a configuration change rather than a rewrite. This is deliberately last: nonlinear combination adds optimizer degrees of freedom, which directly worsens the Section 5 overfitting hazard, and it should not be attempted until the measurement infrastructure of items 1 through 4 exists. Adding complexity that cannot be measured is the one thing this agenda argues against building first.

## 8. Conclusion

Alpha combination is frequently described as a formula, a weighted sum of signals, but the case study in this essay shows it is an architecture. The value of combining alphas comes from adding low-correlation information to a book (Section 2), and it is eroded not by any single dramatic failure but by a chain of quiet ones: raw-scale inputs dominating a weighted average, in-sample weight selection masquerading as skill, a constraint regime that leaches breadth from the book, and a determinism guarantee that stops at a module boundary.

One qualification closes the argument. The essay has called the combination a *chain* of decisions, but the topology is really a closed loop: the optimizer's chosen weights feed back into the very aggregation step they parameterize, and the diagnostics feed back into the weighting. The linear metaphor is used for exposition, and the feedback structure is where the architecture's difficulty actually lives, which is precisely why determinism and measurement are the load-bearing pieces.

The platform under study is unusually well positioned to do this properly. Its factor abstraction makes combination a composition problem; its scoring engine handles missing data with the right semantics; its risk layer is cleanly separated; and, most importantly, its insistence on a deterministic kernel gives weight optimization a fighting chance to be measured rather than wished for. What remains is to close the gap between the architecture the platform already has and the architecture its own data model anticipates. Standardize the factors, measure the ICs, extend determinism to the last line of the signal stream, and honor the multiple-testing hazard with out-of-sample discipline. The formula is trivial to write; the discipline lies in the pipeline, and in the statistics around it.

---

## References

- Ang, A., Hodrick, R. J., Xing, Y., & Zhang, X. (2006). The Cross-Section of Volatility and Expected Returns. *Journal of Finance*, 61(1), 259–299.
- Bailey, D. H., & López de Prado, M. (2014). The Deflated Sharpe Ratio: Correcting for Selection Bias, Backtest Overfitting, and Non-Normality. *Journal of Portfolio Management*, 40(5), 94–107.
- Clarke, R., de Silva, H., & Thorley, S. (2002). Portfolio Constraints and the Fundamental Law of Active Management. *Financial Analysts Journal*, 58(5), 48–66.
- Clarke, R., de Silva, H., & Thorley, S. (2006). The Fundamental Law of Active Portfolio Management. *Journal of Portfolio Management*, 32(3), 26–37.
- DeMiguel, V., Garlappi, L., & Uppal, R. (2009). Optimal Versus Naive Diversification. *Review of Financial Studies*, 22(5), 1915–1953.
- Fama, E. F., & French, K. R. (1993). Common Risk Factors in the Returns on Stocks and Bonds. *Journal of Financial Economics*, 33(1), 3–56.
- Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
- Grinold, R. C. (1989). The Fundamental Law of Active Management. *Journal of Portfolio Management*, 15(3), 30–37.
- Grinold, R. C., & Kahn, R. N. (2000). *Active Portfolio Management* (2nd ed.). McGraw-Hill.
- Harvey, C. R., Liu, Y., & Zhu, H. (2016). …and the Cross-Section of Expected Returns. *Review of Financial Studies*, 29(1), 5–68.
- Jegadeesh, N., & Titman, S. (1993). Returns to Buying Winners and Selling Losers: Implications for Stock Market Efficiency. *Journal of Finance*, 48(1), 65–91.
- Lo, A. W. (2002). The Statistics of Sharpe Ratios. *Financial Analysts Journal*, 58(4), 36–52.
- Markowitz, H. (1952). Portfolio Selection. *Journal of Finance*, 7(1), 77–91.
- Novy-Marx, R. (2013). The Other Side of Value: The Gross Profitability Premium. *Journal of Financial Economics*, 108(1), 1–28.
- Peng, R. D. (2011). Reproducible Research in Computational Science. *Science*, 334(6060), 1226–1227.
- Qian, E., Hua, R., & Sorensen, E. H. (2007). *Quantitative Equity Portfolio Management*. Chapman & Hall/CRC.
- Sandve, G. K., Nekrutenko, A., Taylor, J., & Hovig, E. (2013). Ten Simple Rules for Reproducible Computational Research. *PLoS Computational Biology*, 9(10), e1003285.
- Sullivan, R., Timmermann, A., & White, H. (1999). Data-Snooping, Technical Trading Rule Performance, and the Bootstrap. *Journal of Finance*, 54(5), 1647–1691.
- White, H. (2000). A Reality Check for Data Snooping. *Econometrica*, 68(5), 1097–1126.
