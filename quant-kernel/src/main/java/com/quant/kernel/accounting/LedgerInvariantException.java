package com.quant.kernel.accounting;

/**
 * Thrown when the fundamental accounting invariant
 * ({@code cash + marketValue == equity}) is violated.
 */
public class LedgerInvariantException extends RuntimeException {

    public LedgerInvariantException(String message) {
        super(message);
    }
}
