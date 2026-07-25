package com.quant.data.repository;

import com.quant.common.enums.BarFrequency;
import com.quant.core.model.market.Bar;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for bar data persistence (database-backed).
 * Implementations use MyBatis-Plus or raw JDBC.
 */
public interface BarRepository {

    /**
     * Find bars for a symbol within a date range.
     */
    List<Bar> findBySymbol(String symbol, LocalDate start, LocalDate end, BarFrequency frequency);

    /**
     * Find the most recent bar for a symbol.
     */
    Optional<Bar> findLatest(String symbol, BarFrequency frequency);

    /**
     * Batch insert bars (upsert by symbol+timestamp+frequency).
     */
    int batchInsert(List<Bar> bars);

    /**
     * Delete bars for a symbol in a date range.
     */
    int deleteBySymbol(String symbol, LocalDate start, LocalDate end, BarFrequency frequency);

    /**
     * Count bars for a symbol.
     */
    long count(String symbol, BarFrequency frequency);

    /**
     * Get distinct symbols with bar data.
     */
    List<String> findDistinctSymbols();
}
