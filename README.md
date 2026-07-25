# Quantitative Backtesting Platform

事件驱动的量化回测平台，支持股票多头/多空策略，内置因子库、参数优化引擎和 Web 可视化面板。

## 项目结构

```
quant-backtest-platform/
├── quant-common/       # 公共枚举、工具类、异常
├── quant-core/         # 核心领域模型（事件系统、订单、持仓、组合）
├── quant-data/         # 行情数据管理（CSV/DB/API 接入、清洗、缓存）
├── quant-engine/       # 事件驱动回测引擎（撮合、风控、记录）
├── quant-strategy/     # 策略接口 + 因子库（动量/价值/波动/质量/技术）
├── quant-optimizer/    # 参数优化（网格搜索、遗传算法、步进优化）
├── quant-risk/         # 风控限制 + 仓位管理
├── quant-analytics/    # 绩效分析（夏普/回撤/因子归因/报告）
├── quant-server/       # Spring Boot Web 服务（REST API + WebSocket）
├── quant-ui/           # React 前端可视化面板
└── quant-benchmark/    # 性能基准测试
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Java 17 |
| 构建 | Maven 多模块 |
| Web 框架 | Spring Boot 3.2 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | PostgreSQL 15 |
| 缓存 | Redis 7 + Caffeine |
| 数学库 | Apache Commons Math + EJML |
| 前端 | React 18 + TypeScript + Ant Design 5 + ECharts 5 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 18+ (前端开发)

### 启动基础设施

```bash
docker-compose up -d
```

### 构建项目

```bash
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 启动 Web 服务

```bash
cd quant-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 启动前端

```bash
cd quant-ui
npm install
npm run dev
```

## 核心架构

事件驱动回测引擎，按时间戳顺序处理事件：

```
MARKET_DATA → SIGNAL → ORDER → FILL → PORTFOLIO
```

1. **MarketEvent** — 新 Bar 到达
2. **SignalEvent** — 策略根据 Bar 产生交易信号
3. **OrderEvent** — 信号转换为订单
4. **FillEvent** — 订单撮合成交（含滑点、佣金）
5. **PortfolioEvent** — 投资组合状态更新

## 策略开发示例

```java
public class DualMovingAverageStrategy implements Strategy {
    private int shortPeriod = 5;
    private int longPeriod = 20;

    @Override
    public void init(StrategyConfig config) {
        this.shortPeriod = config.getInt("shortPeriod", 5);
        this.longPeriod = config.getInt("longPeriod", 20);
    }

    @Override
    public List<Signal> onBar(Bar bar, PortfolioSnapshot portfolio) {
        double shortMA = calculateMA(bar.symbol(), shortPeriod);
        double longMA = calculateMA(bar.symbol(), longPeriod);
        if (shortMA > longMA) {
            return List.of(Signal.buy(bar.symbol(), 0.8));
        } else {
            return List.of(Signal.sell(bar.symbol()));
        }
    }
}
```

## License

MIT
