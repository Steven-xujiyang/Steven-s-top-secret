package com.quant.common.exception;

/**
 * Base exception for all backtest-related errors.
 */
public class BacktestException extends RuntimeException {

    public BacktestException(String message) {
        super(message);
    }

    public BacktestException(String message, Throwable cause) {
        super(message, cause);
    }
}
