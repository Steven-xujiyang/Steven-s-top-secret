package com.quant.data.provider;

import com.quant.common.enums.BarFrequency;
import com.quant.common.enums.Market;
import com.quant.core.model.market.Bar;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Interface for market data providers (CSV, database, API).
 * Each implementation handles a specific data source.
 */
public interface MarketDataProvider {

    /**
     * Fetch bars for a single symbol.
     *
     * @param symbol    the instrument symbol
     * @param start     start date (inclusive)
     * @param end       end date (inclusive)
     * @param frequency bar frequency
     * @return list of bars in chronological order
     */
    List<Bar> fetchBars(String symbol, LocalDate start, LocalDate end, BarFrequency frequency);

    /**
     * Fetch bars for multiple symbols at once.
     * Each symbol's bars are independently fetched but aligned by timestamp.
     */
    List<List<Bar>> fetchBars(Set<String> symbols, LocalDate start, LocalDate end, BarFrequency frequency);

    /**
     * Get all available symbols from this provider.
     */
    Set<String> getAvailableSymbols();

    /**
     * Get the date range for which data is available for a symbol.
     *
     * @param symbol the instrument symbol
     * @return array of [earliestDate, latestDate], or null if no data
     */
    LocalDate[] getDateRange(String symbol);

    /**
     * Check if data is available for a given symbol and date range.
     */
    boolean hasData(String symbol, LocalDate start, LocalDate end, BarFrequency frequency);

    /**
     * Get the market this provider serves.
     */
    Market getMarket();
}
