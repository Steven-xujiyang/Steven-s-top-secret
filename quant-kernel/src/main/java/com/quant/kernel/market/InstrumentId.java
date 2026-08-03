package com.quant.kernel.market;

import java.util.Objects;

/**
 * Strongly-typed instrument identifier.
 *
 * <p>Must be non-null and non-blank. Two {@code InstrumentId} instances with the same
 * {@code value} are equal regardless of case; comparison uses {@link String#compareTo}
 * on the lower-cased value for stable ordering.
 */
public record InstrumentId(String value) implements Comparable<InstrumentId> {

    public InstrumentId {
        Objects.requireNonNull(value, "InstrumentId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("InstrumentId value must not be blank");
        }
    }

    @Override
    public int compareTo(InstrumentId other) {
        return this.value.toLowerCase().compareTo(other.value.toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
