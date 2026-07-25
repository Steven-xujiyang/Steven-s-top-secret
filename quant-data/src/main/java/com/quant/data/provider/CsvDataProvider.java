package com.quant.data.provider;

import com.quant.common.enums.BarFrequency;
import com.quant.common.enums.Market;
import com.quant.common.exception.DataNotFoundException;
import com.quant.common.util.DateTimeUtils;
import com.quant.core.model.market.Bar;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Market data provider that reads from CSV files.
 *
 * Expected CSV format: date,open,high,low,close,volume,amount
 * File naming: {symbol}_{frequency}.csv (e.g., "600036_D.csv")
 */
public class CsvDataProvider implements MarketDataProvider {

    private final Path dataDirectory;
    private final Market market;
    private final Map<String, List<Bar>> cache;

    public CsvDataProvider(Path dataDirectory, Market market) {
        this.dataDirectory = dataDirectory;
        this.market = market;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public List<Bar> fetchBars(String symbol, LocalDate start, LocalDate end, BarFrequency frequency) {
        String cacheKey = symbol + "_" + frequency.getAlias();
        List<Bar> allBars = cache.computeIfAbsent(cacheKey, k -> loadBars(symbol, frequency));

        return allBars.stream()
            .filter(b -> !b.timestamp().toLocalDate().isBefore(start))
            .filter(b -> !b.timestamp().toLocalDate().isAfter(end))
            .collect(Collectors.toList());
    }

    @Override
    public List<List<Bar>> fetchBars(Set<String> symbols, LocalDate start, LocalDate end, BarFrequency frequency) {
        List<List<Bar>> results = new ArrayList<>();
        for (String symbol : symbols) {
            results.add(fetchBars(symbol, start, end, frequency));
        }
        return results;
    }

    @Override
    public Set<String> getAvailableSymbols() {
        Set<String> symbols = new HashSet<>();
        try (var stream = Files.list(dataDirectory)) {
            stream.filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith(".csv"))
                .map(name -> name.split("_")[0])
                .forEach(symbols::add);
        } catch (IOException e) {
            throw new RuntimeException("Failed to list data directory: " + dataDirectory, e);
        }
        return symbols;
    }

    @Override
    public LocalDate[] getDateRange(String symbol) {
        List<Bar> bars = fetchBars(symbol, LocalDate.of(1990, 1, 1),
            LocalDate.of(2099, 12, 31), BarFrequency.D);
        if (bars.isEmpty()) return null;
        return new LocalDate[]{
            bars.get(0).timestamp().toLocalDate(),
            bars.get(bars.size() - 1).timestamp().toLocalDate()
        };
    }

    @Override
    public boolean hasData(String symbol, LocalDate start, LocalDate end, BarFrequency frequency) {
        List<Bar> bars = fetchBars(symbol, start, end, frequency);
        return !bars.isEmpty();
    }

    @Override
    public Market getMarket() {
        return market;
    }

    /**
     * Clear the internal cache to free memory or force re-read.
     */
    public void clearCache() {
        cache.clear();
    }

    private List<Bar> loadBars(String symbol, BarFrequency frequency) {
        String fileName = symbol + "_" + frequency.getAlias() + ".csv";
        Path filePath = dataDirectory.resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new DataNotFoundException(
                "CSV file not found: " + filePath.toAbsolutePath());
        }

        List<Bar> bars = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                Bar bar = parseCsvLine(symbol, frequency, line);
                if (bar != null) {
                    bars.add(bar);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV: " + filePath, e);
        }
        bars.sort(Comparator.comparing(Bar::timestamp));
        return bars;
    }

    private Bar parseCsvLine(String symbol, BarFrequency frequency, String line) {
        String[] parts = line.split(",");
        if (parts.length < 7) return null;
        try {
            LocalDate date = DateTimeUtils.parseDate(parts[0].trim());
            double open = Double.parseDouble(parts[1].trim());
            double high = Double.parseDouble(parts[2].trim());
            double low = Double.parseDouble(parts[3].trim());
            double close = Double.parseDouble(parts[4].trim());
            long volume = Long.parseLong(parts[5].trim());
            double amount = Double.parseDouble(parts[6].trim());
            return new Bar(symbol, date.atStartOfDay(), frequency, open, high, low, close, volume, amount);
        } catch (Exception e) {
            // Skip malformed lines
            return null;
        }
    }
}
