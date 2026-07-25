package com.quant.common.exception;

/**
 * Thrown when requested market data is not available.
 */
public class DataNotFoundException extends BacktestException {

    public DataNotFoundException(String symbol, String start, String end) {
        super(String.format("Data not found for %s from %s to %s", symbol, start, end));
    }

    public DataNotFoundException(String message) {
        super(message);
    }
}
