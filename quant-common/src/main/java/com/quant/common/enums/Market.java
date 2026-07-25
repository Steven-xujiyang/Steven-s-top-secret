package com.quant.common.enums;

/**
 * Supported trading markets.
 */
public enum Market {
    ASHARE("A-Share", "CNY"),
    US("US Stock", "USD"),
    HK("Hong Kong", "HKD"),
    CRYPTO("Cryptocurrency", "USDT");

    private final String displayName;
    private final String currency;

    Market(String displayName, String currency) {
        this.displayName = displayName;
        this.currency = currency;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCurrency() {
        return currency;
    }
}
