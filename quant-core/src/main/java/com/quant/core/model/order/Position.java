package com.quant.core.model.order;

/**
 * Position held for a single instrument.
 */
public class Position {

    private final String symbol;
    private double quantity;
    private double avgCost;
    private double marketPrice;
    private double commission;
    private double realizedPnl;

    public Position(String symbol) {
        this.symbol = symbol;
        this.quantity = 0;
        this.avgCost = 0;
        this.marketPrice = 0;
        this.commission = 0;
        this.realizedPnl = 0;
    }

    /**
     * Apply a fill to this position.
     */
    public void applyFill(double fillPrice, double fillQty, double fillCommission) {
        double oldQty = this.quantity;
        double newQty = oldQty + fillQty;
        this.commission += fillCommission;

        if (Math.signum(oldQty) != Math.signum(newQty) && oldQty != 0) {
            // Position flipped or closed: realize PnL on the closed portion
            double closedQty = Math.min(Math.abs(oldQty), Math.abs(fillQty));
            double pnlPerShare = Math.signum(oldQty) > 0 ? fillPrice - avgCost : avgCost - fillPrice;
            this.realizedPnl += closedQty * pnlPerShare - fillCommission;
        }

        if (newQty == 0) {
            this.avgCost = 0;
        } else if (Math.abs(newQty) > Math.abs(oldQty)) {
            // Adding to position
            double addedQty = Math.abs(newQty) - Math.abs(oldQty);
            this.avgCost = (Math.abs(oldQty) * avgCost + addedQty * fillPrice) / Math.abs(newQty);
        }

        this.quantity = newQty;
        this.marketPrice = fillPrice;
    }

    /**
     * Update mark-to-market price.
     */
    public void markToMarket(double price) {
        this.marketPrice = price;
    }

    /**
     * Market value of this position.
     */
    public double marketValue() {
        return quantity * marketPrice;
    }

    /**
     * Unrealized PnL at current market price.
     */
    public double unrealizedPnl() {
        if (quantity == 0) return 0;
        double pnlPerShare = quantity > 0 ? marketPrice - avgCost : avgCost - marketPrice;
        return Math.abs(quantity) * pnlPerShare;
    }

    /**
     * Total PnL (realized + unrealized).
     */
    public double totalPnl() {
        return realizedPnl + unrealizedPnl();
    }

    /**
     * Whether this position is flat (zero).
     */
    public boolean isFlat() {
        return quantity == 0;
    }

    /**
     * Direction: 1 for long, -1 for short, 0 for flat.
     */
    public int direction() {
        return (int) Math.signum(quantity);
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getQuantity() { return quantity; }
    public double getAvgCost() { return avgCost; }
    public double getMarketPrice() { return marketPrice; }
    public double getCommission() { return commission; }
    public double getRealizedPnl() { return realizedPnl; }

    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setAvgCost(double avgCost) { this.avgCost = avgCost; }
    public void setMarketPrice(double marketPrice) { this.marketPrice = marketPrice; }
    public void setCommission(double commission) { this.commission = commission; }
    public void setRealizedPnl(double realizedPnl) { this.realizedPnl = realizedPnl; }
}
