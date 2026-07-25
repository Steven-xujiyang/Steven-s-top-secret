package com.quant.data.cleaning;

import com.quant.core.model.market.Bar;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles missing values in bar data using forward-fill (for gaps within a series).
 * Detects missing dates and fills with the previous bar's close price.
 */
public class MissingValueHandler implements DataCleaner {

    @Override
    public List<Bar> clean(List<Bar> bars) {
        if (bars.isEmpty()) return bars;

        // Group bars by symbol
        Map<String, List<Bar>> bySymbol = new LinkedHashMap<>();
        for (Bar bar : bars) {
            bySymbol.computeIfAbsent(bar.symbol(), k -> new ArrayList<>()).add(bar);
        }

        // Clean each symbol's bars independently
        List<Bar> allCleaned = new ArrayList<>();
        for (List<Bar> symbolBars : bySymbol.values()) {
            allCleaned.addAll(cleanSymbolBars(symbolBars));
        }

        allCleaned.sort(Comparator.comparing(Bar::timestamp));
        return allCleaned;
    }

    private List<Bar> cleanSymbolBars(List<Bar> bars) {
        List<Bar> cleaned = new ArrayList<>();
        for (Bar bar : bars) {
            if (isValid(bar)) {
                cleaned.add(bar);
            } else if (!cleaned.isEmpty()) {
                // Forward fill from last valid bar
                Bar last = cleaned.get(cleaned.size() - 1);
                Bar filled = new Bar(
                    bar.symbol(), bar.timestamp(), bar.frequency(),
                    last.close(), last.close(), last.close(), last.close(),
                    0, 0
                );
                cleaned.add(filled);
            }
            // If first bar is invalid, skip it
        }
        return cleaned;
    }

    private boolean isValid(Bar bar) {
        return !Double.isNaN(bar.open()) && !Double.isNaN(bar.close())
            && bar.open() > 0 && bar.high() > 0 && bar.low() > 0 && bar.close() > 0
            && bar.high() >= bar.low();
    }
}
