# Review Panel — Round 1

**Manuscript:** `docs/essay-alpha-combination.md` — "Combining Alphas: Theory, Architecture, and Critique of a Composite-Score Pipeline"
**Skill:** `academic-research-skills:academic-paper-reviewer` (full mode)
**Date:** 2026-08-03

---

## 1. Editorial Decision Letter

**Decision: Major Revision.**

To the authors,

Your essay was reviewed by a five-member panel: a Journal-Fit reviewer, a methodology reviewer, a domain reviewer, a cross-disciplinary perspective reviewer, and a devil's advocate. The panel is unanimous that the manuscript's technical core is sound — the code evidence is faithful to the source (verbatim quotes verified), the theoretical anchors are correct, and three findings were singled out by multiple reviewers as genuine contributions:

1. **The standardization "wiring omission"** (Section 4.2): the data model (`FactorValue.zScore`/`percentile`) anticipates normalization that the aggregation path never applies.
2. **Hidden effective-weight asymmetry under missing data** (Section 4.3): renormalization over present factors makes realized weights symbol- and date-dependent.
3. **Determinism as a whole-pipeline property** (Section 5): the discipline is enforced for a subset of the pipeline only.

The manuscript must nonetheless undergo major revision before it can be considered suitable for submission. The panel's concerns cluster into four groups:

- **Factual corrections (required, non-negotiable).** The strategy is mischaracterized as long-short: `Signal.sell` produces `FLAT`, which *closes* positions; the book is long top-N / exit bottom-N. The determinism section names the *least* consequential leak (`Instant.now()` on a dead NaN value) while omitting two live ones — the genetic optimizer's unseeded `Random` and `Signal`'s embedded `UUID.randomUUID()`/`Instant.now()`. The claim that the "test infrastructure enforces" the call-level bans overstates the tooling, which bans dependencies and packages but not source calls. The optimizer objective is not a standard Sharpe ratio.
- **Thesis discipline (DA-CRITICAL).** The severity claims ("consequential," "severe") are asserted, not measured; the decisive experiment — composite rank divergence under raw vs. rank-standardized scoring — is cheap and absent. The essay should hedge its empirical claims and name the experiment.
- **Conceptual precision.** The essay conflates Grinold–Kahn *alpha* (unpriced residual forecasts) with Fama–French *factor risk premia* (priced systematic exposure), and presents the fundamental law as a bound when it is an idealization.
- **Citational completeness.** Three references (Fama & French 1993; Jegadeesh & Titman 1993; Qian, Hua & Sorensen 2007) dangle unused; the data-snooping lineage omits White (2000) and Sullivan, Timmermann & White (1999); several recommendations lack canonical citations (Ang et al. 2006; Novy-Marx 2013); the engineering thesis engages no software-architecture or reproducibility literature.

None of these forces a rethink of the thesis. All are addressable in revision.

Sincerely,
The Editorial Synthesizer

---

## 2. Reviewer Verdicts

| Reviewer | Verdict | Signature findings |
|---|---|---|
| Journal-Fit | **Major Revision** | No empirical content; case study unverifiable (private codebase); thin originality in §§2–3; 3 orphan references; no abstract/keywords |
| Methodology | **Major Revision** | Determinism critique selectively evidenced (GA unseeded `Random`, `Signal` UUID/`Instant.now` omitted); enforcement claim overstates tooling; "long-short" mischaracterizes semantics; objective is not standard Sharpe |
| Domain | **Major Revision** | 3 dangling references; alpha-vs-factor-premia conflation; data-snooping lineage incomplete (White 2000; STW 1999) |
| Perspective | **Minor Revision** | Determinism as unqualified good (costs/levels undefined); no software-architecture literature engaged; ranking discards magnitude; no benchmark/cost model |
| Devil's Advocate | — (challenges; no decision authority) | 2 CRITICAL + 6 MAJOR; see adjudication below |

---

## 3. Devil's Advocate CRITICAL Adjudication

**DA-CRITICAL-1 — Internal inconsistency between §4.2 and §5 ("the optimizer can absorb scale").**
*Claim:* the "unit of RSI weight overwhelms momentum" objection assumes hand-set weights, but the platform's weights are optimizer outputs on the raw scale, so the optimizer can absorb native-scale differences; the §4.2 critique operates on a model §5 contradicts.
*Adjudication:* **Partially validated.** The non-invariance of composite rank under scale holds for any fixed weight vector, and the *interpretability* claim is unaffected. But "consequential" is contingent on usage — for optimizer-chosen weights the failure is representationally equivalent. The essay must hedge this claim and acknowledge the optimizer-absorbs-scale path.
*Resolution:* Reword §4.2 to distinguish "weights are opaque" (true) from "this is always consequential" (contingent); add the optimizer-absorbs-scale caveat; name the empirical test that would settle it.

**DA-CRITICAL-2 — The central empirical claim ("consequential," "highest-priority fix") is never tested.**
*Claim:* the essay asserts consequence without measurement; the raw-vs-standardized rank-divergence experiment is cheap and absent.
*Adjudication:* **Validated.** This is the panel's strongest methodological criticism and it coheres with the Journal-Fit reviewer's "no empirical content" finding.
*Resolution:* Hedge severity language throughout; explicitly present the decisive experiment as an open empirical question; frame the essay's contribution as architectural analysis, not measured effects.

---

## 4. Consolidated Revision Roadmap

Prioritized. **P0 = factual corrections (blocking); P1 = thesis discipline; P2 = conceptual precision; P3 = citations; P4 = polish.**

### P0 — Factual corrections (verified against source)

- **R1. Long-short → long/exit.** Replace "long-short" with "long top-N, exit bottom-N." `Signal.sell` emits `FLAT` (`Signal.java:38-41`); `OrderManager` treats `FLAT` as closing a position (`OrderManager.java:78-89`); `SHORT` is only reached via `shortSell`, which `FactorRankingStrategy` never calls. Fix in §1, §4.4, and the asymmetric-strength discussion.
- **R2. Determinism leaks: add the live ones.** Add the unseeded `new Random()` in `GeneticOptimizer` (line 46) and `ParameterSpace.sampleRandom` (line 55), and `UUID.randomUUID()` + `Instant.now()` embedded in every `Signal` (`Signal.java:24,40,54`). Reframe the `Instant.now()` NaN line as illustrative, not the (or only) leak. This *strengthens* the essay's whole-pipeline thesis.
- **R3. Enforcement claim.** Distinguish ADR *policy* from automated *enforcement*: Maven Enforcer bans dependency artifacts only (`quant-kernel/pom.xml:63-78`); ArchUnit enforces package-dependency rules only (`KernelDependencyTest.java`). Call-level bans are enforced by review + the behavioral `BacktestDeterminismTest`. Propose a tool that can actually ban calls (ArchUnit `noClasses().should().callMethod(...)`, or PMD/Checkstyle/ErrorProne).
- **R4. Optimizer objective.** `GridSearchOptimizer` requires an explicit `ObjectiveFunction`; only the GA builder defaults to `maximizeSharpe()`, and its implementation is `totalReturn / per-period std` — non-annualized, contradicting its own javadoc (`ObjectiveFunction.java:16-37`). State this rather than calling it "the standard target."
- **R5. Scale argument completeness.** Add `MAFactor` to the enumeration — it returns `(close − ma)/ma`, the same ratio scale as momentum — and note momentum is "typically on the order of tenths, unbounded in extremes."

### P1 — Thesis discipline (DA-CRITICAL resolutions)

- **R6.** Hedge "consequential"/"severe"; add the optimizer-absorbs-scale caveat to §4.2; name the decisive raw-vs-standardized rank-divergence experiment; frame findings as architectural analysis.
- **R7.** Add a determinism-limits paragraph: determinism is necessary, not sufficient (in-sample maximization over a deterministic kernel is still selection-biased); define levels (run/build/environment); acknowledge float non-associativity and the parallel-throughput trade-off; add dependency/JVM pinning as the complement.

### P2 — Conceptual precision

- **R8.** Add a short passage distinguishing Grinold–Kahn *alpha* (unpriced residual forecasts, IC-weighted combination) from Fama–French *factor risk premia* (priced systematic exposure); state that the platform's long/exit book is closer to a factor portfolio and say which sense of "alpha combination" the essay uses.
- **R9.** Fundamental law: "bounded by" → "is approximated by" (up to the transfer coefficient); state the idealizing assumptions (independent bets, no costs).
- **R10.** IR-combination formula: declare the equal-volatility (equal-variance) assumption; attribute the formula (Qian, Hua & Sorensen 2007 or the signal-combination literature).
- **R11.** Add the transfer coefficient `TC` to the §6 discussion (Clarke, de Silva & Thorley 2006).
- **R12.** Sharpen Lo (2002): the asymptotic distribution of Sharpe depends on serial correlation and higher moments, not merely non-normality/short samples.
- **R13.** Distinguish cross-sectional vs. time-series IC; confirm the monitor reports cross-sectional IC per rebalance.

### P3 — Citations

- **R14.** Weave in the dangling references: Jegadeesh & Titman (1993) for momentum; Fama & French (1993) for the factor-premia distinction; Qian, Hua & Sorensen (2007) for IC weighting, standardization, and the combined-IR formula.
- **R15.** Data-snooping lineage: add White (2000, *Econometrica*) and Sullivan, Timmermann & White (1999, *Journal of Finance*); note deflated Sharpe continues that program.
- **R16.** Add Ang, Hodrick, Xing & Zhang (2006) for the low-volatility anomaly; Novy-Marx (2013) for gross profitability.
- **R17.** Add engineering references for the architecture thesis: Gamma et al. (1994) plugin/registry pattern; Peng (2011) reproducible research; Sandve et al. (2013) ten simple rules for reproducible computational research.
- **R18.** Soften "the academic literature is unambiguous" (§5) to "the weight of the evidence."

### P4 — Polish

- **R19.** Ranking trade-off: acknowledge ranking discards magnitude/conviction; note turnover/cost implications; justify or repair the long/exit strength asymmetry.
- **R20.** Benchmark/cost context: state what the kernel/engine charges (commission; slippage model; no shorting), name the missing benchmark, note raw-Sharpe fitness is the most gameable statistic.
- **R21.** §4.3: sketch surfacing *effective* weights per rebalance (or note per-cross-section standardization largely absorbs the divergence).
- **R22.** §7: rank the six recommendations by value/effort; note what *not* to build first; add a minimum-cross-section / fallback-to-equal-weights caveat for the rank-transform sketch.
- **R23.** §8: fix the aphorism ("the formula is trivial to write; the discipline lies in the pipeline *and in the statistics around it*"); acknowledge the closed-loop (weights feed back into aggregation) topology.
- **R24.** Add an abstract + keywords (Journal-Fit MINOR-5).

---

## 5. Files

- Review package: this file.
- Revised manuscript: `docs/essay-alpha-combination.md` (to be rewritten per this roadmap).
- Revision verification: `docs/essay-review-round-2.md` (after re-review).
