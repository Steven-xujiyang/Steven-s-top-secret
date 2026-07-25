package com.quant.core.model.portfolio;

import com.quant.core.model.order.Position;
import com.quant.core.model.order.Fill;

import java.time.Instant;
import java.util.*;

/**
 * Portfolio tracking cash, positions, and NAV over time.
 */
public class Portfolio {

    private final Account account;
    private final Map<String, Position> positions;
    private final List<PortfolioSnapshot> history;
    private Instant lastUpdateTime;

    public Portfolio(double initialCapital) {
        this.account = new Account(initialCapital);
        this.positions = new LinkedHashMap<>();
        this.history = new ArrayList<>();
        this.lastUpdateTime = Instant.now();
    }

    /**
     * Apply a fill to the portfolio.
     */
    public void applyFill(Fill fill, Instant eventTime) {
        Position pos = positions.computeIfAbsent(fill.symbol(), Position::new);
        pos.applyFill(fill.price(), fill.quantity(), fill.commission());
        account.withdraw(fill.totalCost());
        account.addCommission(fill.commission());
        this.lastUpdateTime = eventTime;

        if (pos.isFlat()) {
            // Keep zero positions for tracking but could also remove
        }
    }

    /**
     * Update all positions to current market prices.
     */
    public void markToMarket(Map<String, Double> prices, Instant eventTime) {
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            Position pos = positions.get(entry.getKey());
            if (pos != null) {
                pos.markToMarket(entry.getValue());
            }
        }
        this.lastUpdateTime = eventTime;
    }

    /**
     * Take a snapshot of the current portfolio state.
     */
    public PortfolioSnapshot snapshot() {
        double positionValue = positions.values().stream()
            .mapToDouble(Position::marketValue)
            .sum();
        double unrealizedPnl = positions.values().stream()
            .mapToDouble(Position::unrealizedPnl)
            .sum();
        double realizedPnl = positions.values().stream()
            .mapToDouble(Position::getRealizedPnl)
            .sum();

        double nav = account.getCash() + positionValue;
        double totalCommission = positions.values().stream()
            .mapToDouble(Position::getCommission)
            .sum();

        return new PortfolioSnapshot(
            lastUpdateTime,
            account.getCash(),
            new HashMap<>(positions),
            positionValue,
            nav,
            unrealizedPnl,
            realizedPnl,
            totalCommission,
            account.getInitialCapital()
        );
    }

    /**
     * Record a snapshot in history.
     */
    public void recordSnapshot(PortfolioSnapshot snapshot) {
        history.add(snapshot);
    }

    /**
     * Get cash available for trading.
     */
    public double availableCash() {
        return account.getCash();
    }

    /**
     * Get total NAV (cash + position market value).
     */
    public double nav() {
        return account.getCash() + positions.values().stream()
            .mapToDouble(Position::marketValue)
            .sum();
    }

    /**
     * Get position for a symbol, creating if absent.
     */
    public Position getPosition(String symbol) {
        return positions.get(symbol);
    }

    // Getters
    public Account getAccount() { return account; }
    public Map<String, Position> getPositions() { return Collections.unmodifiableMap(positions); }
    public List<PortfolioSnapshot> getHistory() { return Collections.unmodifiableList(history); }
    public Instant getLastUpdateTime() { return lastUpdateTime; }
}
