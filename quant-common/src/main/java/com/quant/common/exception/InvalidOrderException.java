package com.quant.common.exception;

/**
 * Thrown when an order fails validation checks.
 */
public class InvalidOrderException extends BacktestException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String orderId, String reason) {
        super(String.format("Invalid order %s: %s", orderId, reason));
    }
}
