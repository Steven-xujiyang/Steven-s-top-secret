package com.quant.kernel.portfolio;

import java.util.Objects;

/**
 * Deterministic order identifier.
 *
 * <p>Constructed from a stable sequence number, not a random UUID.
 */
public record OrderId(String value) implements Comparable<OrderId> {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("OrderId value must not be blank");
        }
    }

    /**
     * Creates an OrderId from an integer sequence, zero-padded to 5 digits.
     */
    public static OrderId fromSequence(int seq) {
        return new OrderId("ORD-" + String.format("%05d", seq));
    }

    @Override
    public int compareTo(OrderId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
