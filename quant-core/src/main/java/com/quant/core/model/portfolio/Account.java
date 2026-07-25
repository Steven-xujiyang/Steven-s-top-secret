package com.quant.core.model.portfolio;

/**
 * Account managing cash, buying power, and margin.
 */
public class Account {

    private final double initialCapital;
    private double cash;
    private double totalCommission;
    private double marginLoan;

    public Account(double initialCapital) {
        this.initialCapital = initialCapital;
        this.cash = initialCapital;
        this.totalCommission = 0;
        this.marginLoan = 0;
    }

    /**
     * Withdraw cash (e.g., for a buy order).
     */
    public void withdraw(double amount) {
        this.cash -= amount;
    }

    /**
     * Deposit cash (e.g., from a sell order).
     */
    public void deposit(double amount) {
        this.cash += amount;
    }

    /**
     * Add commission to tracking.
     */
    public void addCommission(double commission) {
        this.totalCommission += commission;
    }

    /**
     * Buying power: cash minus margin requirements.
     */
    public double buyingPower() {
        return cash - marginLoan;
    }

    // Getters and setters
    public double getInitialCapital() { return initialCapital; }
    public double getCash() { return cash; }
    public double getTotalCommission() { return totalCommission; }
    public double getMarginLoan() { return marginLoan; }

    public void setCash(double cash) { this.cash = cash; }
    public void setMarginLoan(double marginLoan) { this.marginLoan = marginLoan; }
}
