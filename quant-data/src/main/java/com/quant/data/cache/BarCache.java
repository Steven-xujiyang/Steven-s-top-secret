package com.quant.data.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.quant.common.enums.BarFrequency;
import com.quant.core.model.market.Bar;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache for bar data using Caffeine (high-performance LRU).
 * Caches by symbol+frequency key for fast repeated lookups during backtesting.
 */
public class BarCache {

    private final com.github.benmanes.caffeine.cache.Cache<String, List<Bar>> cache;

    private static final long DEFAULT_MAX_SIZE = 10_000;
    private static final long DEFAULT_TTL_MINUTES = 30;

    public BarCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_MINUTES);
    }

    public BarCache(long maxEntries, long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxEntries)
            .expireAfterAccess(ttlMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    private static String cacheKey(String symbol, BarFrequency frequency) {
        return symbol + ":" + frequency.getAlias();
    }

    /**
     * Get bars from cache, or null if not present.
     */
    public List<Bar> get(String symbol, BarFrequency frequency) {
        return cache.getIfPresent(cacheKey(symbol, frequency));
    }

    /**
     * Get bars for a specific date range, using cached data when possible.
     * Returns null if the cache doesn't cover the range.
     */
    public List<Bar> get(String symbol, BarFrequency frequency, LocalDate start, LocalDate end) {
        List<Bar> allBars = get(symbol, frequency);
        if (allBars == null) return null;

        // Check if cache covers the requested range
        if (allBars.isEmpty()) return List.of();
        LocalDate cacheStart = allBars.get(0).timestamp().toLocalDate();
        LocalDate cacheEnd = allBars.get(allBars.size() - 1).timestamp().toLocalDate();
        if (start.isBefore(cacheStart) || end.isAfter(cacheEnd)) return null;

        return allBars.stream()
            .filter(b -> !b.timestamp().toLocalDate().isBefore(start))
            .filter(b -> !b.timestamp().toLocalDate().isAfter(end))
            .toList();
    }

    /**
     * Put bars into cache.
     */
    public void put(String symbol, BarFrequency frequency, List<Bar> bars) {
        cache.put(cacheKey(symbol, frequency), bars);
    }

    /**
     * Invalidate a specific symbol+frequency entry.
     */
    public void invalidate(String symbol, BarFrequency frequency) {
        cache.invalidate(cacheKey(symbol, frequency));
    }

    /**
     * Clear the entire cache.
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * Get cache statistics for monitoring.
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }

    /**
     * Estimated size of the cache.
     */
    public long estimatedSize() {
        return cache.estimatedSize();
    }
}
