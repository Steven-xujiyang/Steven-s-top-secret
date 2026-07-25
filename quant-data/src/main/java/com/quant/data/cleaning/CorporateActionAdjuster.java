package com.quant.data.cleaning;

import com.quant.core.model.market.Bar;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjusts historical prices for corporate actions (splits, dividends).
 * Currently a stub — in production this would read adjustment factors from a database.
 */
public class CorporateActionAdjuster implements DataCleaner {

    @Override
    public List<Bar> clean(List<Bar> bars) {
        // Stub implementation: passes through unchanged.
        // Production: apply adjustment factors (forward/backward) for:
        // - Stock splits (e.g., 2:1 split → multiply pre-split prices by 0.5)
        // - Dividends (subtract dividend amount from pre-ex-date prices)
        // - Rights issues
        return new ArrayList<>(bars);
    }

    /**
     * Apply a stock split adjustment to historical bars.
     *
     * @param bars        bars to adjust
     * @param splitRatio  split ratio (e.g., 2.0 for a 2-for-1 split)
     * @param splitDate   date of the split
     * @return adjusted bars (pre-split prices divided by splitRatio)
     */
    public static List<Bar> adjustForSplit(List<Bar> bars, double splitRatio, java.time.LocalDate splitDate) {
        List<Bar> adjusted = new ArrayList<>();
        for (Bar bar : bars) {
            if (bar.timestamp().toLocalDate().isBefore(splitDate)) {
                adjusted.add(bar.adjusted(1.0 / splitRatio));
            } else {
                adjusted.add(bar);
            }
        }
        return adjusted;
    }
}
