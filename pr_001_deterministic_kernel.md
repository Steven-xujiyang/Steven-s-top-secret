# PR-001：建立确定性的日频回测内核

> 类型：基础能力 / 高风险金融核心  
> 建议标题：`feat(kernel): add deterministic next-open backtest vertical slice`  
> 建议风险等级：R3  
> 前置条件：新建 Java/Maven 仓库；ADR-001 至 ADR-005 已批准

## Summary

本 PR 建立第一条可运行的纵向切片：读取内存中的两资产日频行情，在某日收盘后由固定策略产生目标，在下一交易日开盘生成成交，用成交更新现金和持仓，并输出每日组合快照。

本 PR 的成功标准不是收益率，而是三个可证伪性质：

1. 收盘后生成的订单不能按同一收盘价成交；
2. 每个账本事件后满足净资产恒等式；
3. 相同输入和 seed 重放得到相同规范化结果。

这是后续数据适配器、策略插件、异步任务和 API 的可信基础。

## Why

如果先做页面、数据库或复杂策略，我们会得到一个“能提交任务、能展示曲线”的系统，却无法确认曲线是否来自可执行交易。第一条切片选择最小数据和最简单策略，使时间顺序、订单、成交和现金变化可以人工计算。

它验证架构中最关键的依赖方向：

```text
MarketData → StrategyDecision → TargetPortfolio
→ Order → Fill → LedgerSnapshot → DailySnapshot
```

## Scope

### In scope

- Java 25、Maven Wrapper 和父 POM；
- `quant-kernel` 纯 Java 模块；
- 日频 `Bar`、`MarketTime` 和内存数据源；
- 固定示例策略，在指定收盘后返回目标权重；
- 目标权重转换为整数股订单；
- `NextBarOpenExecutionModel`；
- 固定成交额费率佣金；
- 现金、持仓和净资产账本；
- 两资产三交易日黄金样本；
- 决定性、未来隔离、时点执行和账本不变量测试；
- 最小 CLI/demo 仅用于打印规范化结果；
- 架构测试确保 kernel 无 Spring/JPA/HTTP 依赖。

### Non-goals

- Spring Boot、REST API、数据库、Parquet；
- 实际行情供应商；
- 因子计算、优化器和参数搜索；
- 部分成交、成交量限制和复杂滑点；
- 分红、拆股、停牌和退市；
- 做空、融资、借券费、多币种；
- Web UI；
- 用户上传策略代码；
- 性能优化和并行事件处理。

如果实现过程中发现必须引入上述内容，停止并提出新 PR，不扩大本 PR。

## Architecture

### Maven modules

```text
quant-platform/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
├── quant-kernel/
│   ├── pom.xml
│   └── src/
└── quant-architecture-tests/
    ├── pom.xml
    └── src/test/
```

### Kernel packages

```text
com.example.quant.kernel
├── time/           # MarketTime、TradingCalendar
├── market/         # Bar、MarketSlice、MarketDataFeed
├── strategy/       # Strategy、StrategyDecision、TargetPortfolio
├── portfolio/      # OrderPlanner、Order
├── execution/      # ExecutionModel、Fill、CommissionModel
├── accounting/     # LedgerSnapshot、Position、Ledger
└── backtest/       # RunSpec、BacktestEngine、BacktestResult
```

包名中的 `com.example` 在仓库初始化时替换为组织域名；替换动作必须一次完成，不允许并存两个根包。

### Dependency rules

- `backtest` 可以编排各领域公开接口；
- `strategy` 不依赖 `execution` 或 `accounting`；
- `portfolio` 可读取账本公开快照，但不能修改账本；
- `execution` 不依赖具体策略；
- `accounting` 只消费成交事实和估值输入；
- 任何 kernel 包都不能依赖 `org.springframework..`、`jakarta.persistence..`、HTTP 客户端或数据库驱动；
- 不创建通用 `util` 或 `common` 垃圾桶包。

## Domain contract

### Value objects

```java
public record InstrumentId(String value) {}

public record MarketTime(LocalDate tradingDate, MarketPhase phase) {}

public enum MarketPhase {
    OPEN, CLOSE
}
```

值对象构造时拒绝 null、空标识和非法数值。所有集合进入 record 时做防御性复制，并返回不可变集合。

### Bar

```java
public record DailyBar(
    InstrumentId instrument,
    LocalDate tradingDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    long volume,
    Instant availableAt) {}
```

约束：

- 所有价格严格大于 0；
- `low <= open/close <= high`；
- volume 非负；
- 本 PR 的 close 在收盘事件后才可用；
- 下一开盘执行只能读取下一日 open，不能读取该日 high/low/close。

### Strategy

```java
public interface Strategy {
    StrategyDecision decide(StrategyContext context, MarketSlice slice);
}
```

本 PR 提供测试策略 `ScheduledTargetStrategy`：在指定日期收盘后返回固定目标权重。生产代码不硬编码具体证券或日期。

### Target portfolio

```java
public record TargetPortfolio(
    MarketTime decidedAt,
    Map<InstrumentId, BigDecimal> weights) {}
```

本 PR 只支持 long-only：每个权重 `[0, 1]`，总和不超过 1。权重使用 `BigDecimal`，比较时使用数值比较而不是依赖 scale-sensitive equality。

### Order

```java
public record Order(
    OrderId id,
    InstrumentId instrument,
    Side side,
    long quantity,
    MarketTime createdAt,
    ExecutionPolicy executionPolicy) {}
```

本 PR 只允许正整数数量和 `NEXT_BAR_OPEN`。

### Fill

```java
public record Fill(
    FillId id,
    OrderId orderId,
    InstrumentId instrument,
    Side side,
    long quantity,
    BigDecimal price,
    BigDecimal commission,
    MarketTime filledAt) {}
```

`Fill` 是不可变事实。`FillId` 必须从稳定输入派生或由 run 内决定性序列产生，不能用随机 UUID 破坏黄金结果。

### Ledger snapshot

```java
public record LedgerSnapshot(
    MarketTime time,
    BigDecimal cash,
    Map<InstrumentId, Position> positions,
    BigDecimal marketValue,
    BigDecimal equity,
    BigDecimal cumulativeCommission) {}
```

本 PR 单币种、无负现金。金额统一使用文档化的 scale 和 `RoundingMode.HALF_EVEN`；如选择不同规则，必须先更新 ADR。

## Time model

### Event ordering

每个交易日固定顺序：

```text
OPEN
1. 发布 open-only MarketSlice
2. 执行之前已创建且 policy=NEXT_BAR_OPEN 的订单
3. 将 Fill 应用到账本
4. 以 open 估值并记录可选快照

CLOSE
5. 发布包含当日 close 的 MarketSlice
6. 以 close 估值
7. 调用策略
8. 把目标权重转成订单，订单等待下一 OPEN
9. 记录日终快照
```

若采用“收盘后信号、下一开盘成交”，禁止在步骤 7 后回到步骤 2。

### Stable ordering

同一事件阶段内按 `(instrumentId.value, orderSequence)` 稳定排序。不能依赖 `HashMap` 遍历顺序。

## Order planning

为了让黄金样本可人工计算，本 PR 使用简单算法：

1. 取收盘估值后的 equity；
2. `targetValue = equity × targetWeight`；
3. 使用当前已知 close 仅计算期望数量；
4. `targetQuantity = floor(targetValue / close)`；
5. `delta = targetQuantity - currentQuantity`；
6. 生成订单；
7. 下一开盘按真实 open 成交；
8. 如果开盘跳空导致现金不足，执行模型拒绝该买单并给出明确原因。

这里故意不在订单规划时预知下一开盘。后续 PR 可增加现金缓冲和缩量成交。

## Commission

```text
commission = abs(quantity × fillPrice) × commissionRate
```

- 佣金只由 Fill 驱动；
- 没有 Fill 则佣金为 0；
- 本 PR 不设置最低佣金；
- 佣金按 ADR 中的金额 scale 舍入一次；
- 不额外计算滑点。

## Ledger transitions

买入：

```text
cash' = cash - quantity × price - commission
positionQty' = positionQty + quantity
```

卖出：

```text
cash' = cash + quantity × price - commission
positionQty' = positionQty - quantity
```

本 PR long-only，不允许卖出超过持仓。

估值：

```text
marketValue = Σ(positionQty × currentMark)
equity = cash + marketValue
```

每次成交后及每个日终都调用 `LedgerInvariantChecker`。失败时抛出包含 runId、eventTime 和相关 FillId 的领域异常。

## Golden fixture

使用固定、人工可算的说明性数据，不代表真实市场：

| 日期 | 资产 A 开/收 | 资产 B 开/收 | 事件 |
|---|---:|---:|---|
| D1 | 100 / 100 | 50 / 50 | D1 收盘目标 A=50%、B=0% |
| D2 | 110 / 108 | 48 / 49 | D2 开盘执行；D2 收盘目标 A=0%、B=50% |
| D3 | 107 / 109 | 52 / 51 | D3 开盘执行；结束 |

初始现金、佣金率和金额 scale 在 fixture 中显式指定。Expected 必须人工计算并写在测试注释或 `golden-fixture.md`，不能通过调用生产佣金/账本实现生成 expected。

黄金测试至少断言：

- D1 没有 Fill；
- 第一笔 Fill 在 D2 OPEN 且价格为 110，不是 D1 close=100；
- D2 订单在 D3 OPEN 执行；
- 每日 cash、position quantity、commission、market value、equity 与人工表一致；
- 最终订单和成交顺序稳定。

## Required tests

### Unit tests

- `DailyBarTest.rejectsInvalidOhlcAndNegativeVolume`
- `TargetPortfolioTest.rejectsNegativeOrOverAllocatedWeights`
- `CommissionModelTest.chargesOnlyOnFillAndRoundsOnce`
- `LedgerTest.appliesBuyAndSellFillWithoutBreakingEquityIdentity`
- `OrderPlannerTest.neverSellsMoreThanLongPosition`

### Scientific/invariant tests

- `BacktestTimingTest.closeDecisionCannotFillAtSameClose`
- `BacktestCausalityTest.changingFutureBarsDoesNotChangePastArtifacts`
- `BacktestDeterminismTest.sameRunSpecProducesSameCanonicalArtifacts`
- `LedgerPropertyTest.equityAlwaysEqualsCashPlusMarketValue`
- `ExecutionPropertyTest.fillNeverPrecedesOrderAndNeverExceedsQuantity`

### Architecture tests

- `KernelDependencyTest.kernelHasNoSpringJpaOrHttpDependencies`
- `KernelDependencyTest.strategyDoesNotDependOnExecutionOrAccounting`
- `KernelDependencyTest.executionDoesNotDependOnConcreteStrategy`

### Golden test

- `TwoAssetThreeDayGoldenBacktestTest.matchesHandCalculatedLedger`

### Tests must prove failure

实现前先加入 `BacktestTimingTest`，让当前缺失实现以预期方式失败。实现完成后，人工临时将执行价改为 D1 close 或允许 CLOSE 即时成交，确认该测试会失败；临时 fault 不进入提交。

## Canonical result

决定性测试不能直接比较含对象地址、当前时间或随机 UUID 的日志。定义规范化结果：

```text
runSpec
ordered orders
ordered fills
ordered daily snapshots
total commission
warnings
```

以稳定字段顺序、ISO 时间和标准化 decimal 输出。规范化函数属于测试/产物契约，不包含运行耗时或机器信息。

## Expected file changes

以下是建议，不要求私有类逐字匹配；公共边界变化必须在实现前说明。

```text
pom.xml
quant-kernel/pom.xml
quant-kernel/src/main/java/.../time/*
quant-kernel/src/main/java/.../market/*
quant-kernel/src/main/java/.../strategy/*
quant-kernel/src/main/java/.../portfolio/*
quant-kernel/src/main/java/.../execution/*
quant-kernel/src/main/java/.../accounting/*
quant-kernel/src/main/java/.../backtest/*
quant-kernel/src/test/java/.../*
quant-kernel/src/test/resources/golden/*
quant-architecture-tests/pom.xml
quant-architecture-tests/src/test/java/.../KernelDependencyTest.java
docs/adr/ADR-001-modular-monolith.md
docs/adr/ADR-002-pure-java-kernel.md
docs/adr/ADR-003-time-semantics.md
docs/adr/ADR-004-next-open-execution.md
docs/adr/ADR-005-money-rounding.md
```

## Validation commands

命令以生成后的 Maven module 名为准，但 PR 必须提供等价命令：

```bash
./mvnw -pl quant-kernel -Dtest=BacktestTimingTest test
./mvnw -pl quant-kernel test
./mvnw -pl quant-architecture-tests test
./mvnw verify
```

如项目启用格式和静态分析 profile，还需：

```bash
./mvnw -Pquality verify
```

## Acceptance criteria

- [ ] `quant-kernel` 编译依赖中不存在 Spring/JPA/HTTP/数据库；
- [ ] 两资产三日 fixture 可从单一命令运行；
- [ ] D1 收盘订单最早 D2 开盘成交；
- [ ] Fill 使用 D2 open，不使用 D1 close；
- [ ] 每个事件后净资产恒等式成立；
- [ ] 手算黄金账本逐字段匹配；
- [ ] 修改 D3 行情不改变 D2 CLOSE 及之前所有产物；
- [ ] 相同 RunSpec 重放的规范化产物完全一致；
- [ ] 无成交不收佣金，佣金只扣一次；
- [ ] 订单/成交排序不依赖 HashMap；
- [ ] 架构测试与全量测试通过；
- [ ] 没有删除、禁用或放宽测试；
- [ ] PR 说明测试证明什么和未证明什么。

## What this PR does not prove

即使全部测试通过，本 PR 仍然没有证明：

- 真实数据没有幸存者偏差或修订偏差；
- 分红、拆股、停牌和退市处理正确；
- 滑点和佣金假设接近真实市场；
- 策略未来会盈利；
- 大数据量下性能满足生产要求；
- 多 worker 重试和结果发布已经安全；
- 做空、融资或多币种账本正确。

这些限制必须保留在 PR，不得用“基础测试通过”替换。

## Reviewer guide

审查者按顺序检查：

1. 确认 non-goals 没有膨胀；
2. 对照事件顺序看是否可能同日收盘成交；
3. 用纸笔复算黄金 fixture；
4. 搜索 `Instant.now`、`LocalDate.now`、`UUID.randomUUID`、`new Random`、`HashMap` 结果遍历；
5. 检查佣金是否由 Fill 触发且只扣一次；
6. 检查 BigDecimal scale 和 rounding 是否集中；
7. 检查测试 expected 是否独立于生产实现；
8. 检查未来隔离测试是否比较了中间产物，而非只看最终值；
9. 运行聚焦、架构和全量验证；
10. 确认 PR 的“未证明什么”没有被删除。

## Rollback

本 PR 不包含数据库和公共线上 API。若发现设计错误，回滚整个 feature commit 即可。合并后若要改变时间语义或金额舍入，必须新增 ADR，不允许静默修改黄金样本 expected。

## Follow-ups

- PR-002：DatasetManifest、availableAt 与 Parquet 只读适配器；
- PR-003：停牌、成交量限制、线性滑点和部分成交；
- PR-004：公司行动与历史股票池；
- PR-005：RunSpec、异步 worker、幂等和原子结果发布；
- PR-006：REST API 与结果查询。

