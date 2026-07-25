package com.quant.engine.portfolio;

import com.quant.core.model.order.Fill;
import com.quant.core.model.portfolio.Portfolio;
import com.quant.core.model.portfolio.PortfolioSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages portfolio state: applies fills, marks to market, and emits snapshots.
 */
public class PortfolioManager {

    private static final Logger log = LoggerFactory.getLogger(PortfolioManager.class);

    private final Portfolio portfolio;

    public PortfolioManager(double initialCapital) {
        this.portfolio = new Portfolio(initialCapital);
    }

    /**
     * Apply a fill to the portfolio, updating cash and positions.
     */
    public void applyFill(Fill fill, Instant eventTime) {
        portfolio.applyFill(fill, eventTime);
    }

    /**
     * Update all positions to current market prices.
     */
    public void markToMarket(Map<String, Double> prices, Instant eventTime) {
        portfolio.markToMarket(prices, eventTime);
    }

    /**
     * Build a price map from bar data for mark-to-market.
     */
    public Map<String, Double> priceMapFromBars(java.util.List<com.quant.core.model.market.Bar> bars) {
        Map<String, Double> prices = new HashMap<>();
        for (var bar : bars) {
            prices.put(bar.symbol(), bar.close());
        }
        return prices;
    }

    /**
     * Take a snapshot of current portfolio state.
     */
    public PortfolioSnapshot snapshot() {
        return portfolio.snapshot();
    }

    /**
     * Record the current snapshot in history.
     */
    public void recordSnapshot(PortfolioSnapshot snapshot) {
        portfolio.recordSnapshot(snapshot);
    }

    /**
     * Get the underlying portfolio.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Current NAV.
     */
    public double nav() {
        return portfolio.nav();
    }

    /**
     * Available cash.
     */
    public double availableCash() {
        return portfolio.availableCash();
    }
}
