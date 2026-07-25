package com.quant.engine.loop;

import com.quant.core.clock.SimulationClock;
import com.quant.core.event.*;
import com.quant.core.model.market.Bar;
import com.quant.core.model.order.Fill;
import com.quant.core.model.order.Order;
import com.quant.core.model.portfolio.PortfolioSnapshot;
import com.quant.core.model.signal.Signal;
import com.quant.data.provider.MarketDataProvider;
import com.quant.engine.bus.InMemoryEventBus;
import com.quant.engine.execution.ExecutionSimulator;
import com.quant.engine.execution.OrderManager;
import com.quant.engine.portfolio.PortfolioManager;
import com.quant.engine.recorder.BacktestRecorder;
import com.quant.engine.recorder.MetricsCollector;
import com.quant.engine.strategy.StrategyRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Core event-driven backtesting loop.
 *
 * Processes events in strict time-priority order:
 * MARKET_DATA → SIGNAL → ORDER → FILL → PORTFOLIO
 *
 * Each bar cycle:
 * 1. Emit MarketEvent with new bars
 * 2. Strategy generates signals → emit SignalEvent
 * 3. Convert signals to orders → emit OrderEvent
 * 4. Simulate order execution → emit FillEvent
 * 5. Update portfolio → emit PortfolioEvent
 */
public class EventLoop {

    private static final Logger log = LoggerFactory.getLogger(EventLoop.class);

    private final SimulationClock clock;
    private final MarketDataProvider dataProvider;
    private final InMemoryEventBus eventBus;
    private final StrategyRunner strategyRunner;
    private final OrderManager orderManager;
    private final ExecutionSimulator executionSimulator;
    private final PortfolioManager portfolioManager;
    private final BacktestRecorder recorder;
    private final MetricsCollector metrics;
    private final EventLoopConfig config;

    public EventLoop(EventLoopConfig config,
                     MarketDataProvider dataProvider,
                     StrategyRunner strategyRunner,
                     OrderManager orderManager,
                     ExecutionSimulator executionSimulator,
                     PortfolioManager portfolioManager) {
        this.config = config;
        this.clock = new SimulationClock(config.getStartTime());
        this.dataProvider = dataProvider;
        this.eventBus = new InMemoryEventBus();
        this.strategyRunner = strategyRunner;
        this.orderManager = orderManager;
        this.executionSimulator = executionSimulator;
        this.portfolioManager = portfolioManager;
        this.recorder = new BacktestRecorder();
        this.metrics = new MetricsCollector();

        this.recorder.attach(eventBus);
    }

    /**
     * Run the event loop from start to end.
     */
    public void run(List<Bar[]> barData) {
        metrics.start();

        // Emit system start event
        eventBus.publish(new SystemEvent(clock.now(), SystemEvent.SystemEventType.START));

        int barIndex = 0;
        int totalBars = barData.size();

        while (barIndex < totalBars) {
            Bar[] bars = barData.get(barIndex);
            barIndex++;

            if (bars.length == 0) continue;

            Instant barTime = bars[0].timestamp();
            clock.tickTo(barTime);
            metrics.incrementBar();

            // Step 1: Market data arrives
            MarketEvent marketEvent = new MarketEvent(barTime, bars);
            eventBus.publish(marketEvent);
            metrics.incrementEvent();

            // Step 2: Update position mark-to-market
            Map<String, Double> prices = portfolioManager.priceMapFromBars(Arrays.asList(bars));
            portfolioManager.markToMarket(prices, barTime);

            // Step 3: Strategy evaluates and generates signals
            PortfolioSnapshot currentPortfolio = portfolioManager.snapshot();
            List<Signal> signals = strategyRunner.onBars(Arrays.asList(bars), currentPortfolio);

            if (!signals.isEmpty()) {
                SignalEvent signalEvent = new SignalEvent(barTime, strategyRunner.strategyName(), signals);
                eventBus.publish(signalEvent);
                metrics.incrementEvent();
                metrics.incrementSignal(signals.size());

                // Step 4: Convert signals to orders
                List<Order> orders = orderManager.generateOrders(signals,
                    portfolioManager.getPortfolio(), prices);

                for (Order order : orders) {
                    OrderEvent orderEvent = new OrderEvent(barTime, order, order.getStatus());
                    eventBus.publish(orderEvent);
                    metrics.incrementEvent();
                }
            }

            // Step 5: Execute active orders against current bar
            List<Order> activeOrders = orderManager.getActiveOrders();
            List<Fill> fills = executionSimulator.execute(activeOrders, Arrays.asList(bars));

            for (Fill fill : fills) {
                FillEvent fillEvent = new FillEvent(barTime, fill);
                eventBus.publish(fillEvent);
                metrics.incrementEvent();
                metrics.incrementTrade();

                // Apply fill to portfolio
                portfolioManager.applyFill(fill, barTime);
            }

            // Step 6: Emit portfolio snapshot
            PortfolioSnapshot snapshot = portfolioManager.snapshot();
            PortfolioEvent portfolioEvent = new PortfolioEvent(barTime, snapshot);
            eventBus.publish(portfolioEvent);
            metrics.incrementEvent();
        }

        // Call strategy completion
        strategyRunner.complete();

        // Emit system stop event
        eventBus.publish(new SystemEvent(clock.now(), SystemEvent.SystemEventType.STOP));

        metrics.stop();
        log.info("Event loop complete — {}", metrics);
    }

    // Getters
    public InMemoryEventBus getEventBus() { return eventBus; }
    public BacktestRecorder getRecorder() { return recorder; }
    public MetricsCollector getMetrics() { return metrics; }
    public PortfolioManager getPortfolioManager() { return portfolioManager; }
    public SimulationClock getClock() { return clock; }
}
