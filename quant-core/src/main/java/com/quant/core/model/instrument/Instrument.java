package com.quant.core.model.instrument;

import com.quant.common.enums.AssetClass;
import com.quant.common.enums.Market;
import com.quant.common.enums.Currency;

/**
 * Trading instrument (stock, ETF, future, etc.).
 */
public record Instrument(
    String symbol,
    String name,
    Market market,
    AssetClass assetClass,
    Currency currency,
    int lotSize,
    double tickSize,
    double multiplier
) {
    /**
     * Create a standard A-share stock instrument.
     */
    public static Instrument aShare(String symbol, String name) {
        return new Instrument(symbol, name, Market.ASHARE, AssetClass.STOCK, Currency.CNY, 100, 0.01, 1.0);
    }

    /**
     * Create a standard US stock instrument.
     */
    public static Instrument usStock(String symbol, String name) {
        return new Instrument(symbol, name, Market.US, AssetClass.STOCK, Currency.USD, 1, 0.01, 1.0);
    }

    /**
     * Full qualified key: e.g. "600036.SH" or "AAPL.US"
     */
    public String qualifiedSymbol() {
        String suffix = switch (market) {
            case ASHARE -> symbol.startsWith("6") ? "SH" : "SZ";
            case US -> "US";
            case HK -> "HK";
            default -> market.name();
        };
        return symbol + "." + suffix;
    }
}
