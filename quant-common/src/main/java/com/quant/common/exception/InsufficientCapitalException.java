package com.quant.common.exception;

/**
 * Thrown when an order cannot be placed due to insufficient capital.
 */
public class InsufficientCapitalException extends BacktestException {

    private final double available;
    private final double required;

    public InsufficientCapitalException(double available, double required) {
        super(String.format("Insufficient capital: available %.2f, required %.2f", available, required));
        this.available = available;
        this.required = required;
    }

    public double getAvailable() {
        return available;
    }

    public double getRequired() {
        return required;
    }
}
