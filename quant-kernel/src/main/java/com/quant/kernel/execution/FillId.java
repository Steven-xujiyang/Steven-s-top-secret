package com.quant.kernel.execution;

import java.util.Objects;

/**
 * Deterministic fill identifier.
 *
 * <p>Constructed from a stable sequence number within a backtest run,
 * never from a random UUID.
 */
public record FillId(String value) implements Comparable<FillId> {

    public FillId {
        Objects.requireNonNull(value, "FillId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FillId value must not be blank");
        }
    }

    /**
     * Creates a FillId from an integer sequence, zero-padded to 5 digits.
     */
    public static FillId fromSequence(int seq) {
        return new FillId("FILL-" + String.format("%05d", seq));
    }

    @Override
    public int compareTo(FillId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
