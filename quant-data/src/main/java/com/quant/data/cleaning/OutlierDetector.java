package com.quant.data.cleaning;

import com.quant.core.model.market.Bar;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects and corrects outliers using Z-score method on daily returns.
 * Bars with |z-score| > threshold are replaced with the previous bar's close.
 */
public class OutlierDetector implements DataCleaner {

    private final double zScoreThreshold;

    public OutlierDetector(double zScoreThreshold) {
        this.zScoreThreshold = zScoreThreshold;
    }

    @Override
    public List<Bar> clean(List<Bar> bars) {
        if (bars.size() < 2) return new ArrayList<>(bars);

        // Compute log returns
        double[] returns = new double[bars.size() - 1];
        for (int i = 1; i < bars.size(); i++) {
            double prevClose = bars.get(i - 1).close();
            double currClose = bars.get(i).close();
            returns[i - 1] = prevClose > 0 ? Math.log(currClose / prevClose) : 0;
        }

        // Compute mean and std of returns
        double mean = 0, std = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;
        for (double r : returns) std += (r - mean) * (r - mean);
        std = Math.sqrt(std / returns.length);

        if (std < 1e-10) return new ArrayList<>(bars); // no variation, skip

        // Replace outliers
        List<Bar> cleaned = new ArrayList<>();
        cleaned.add(bars.get(0)); // first bar kept as-is

        for (int i = 1; i < bars.size(); i++) {
            double zScore = Math.abs((returns[i - 1] - mean) / std);
            if (zScore > zScoreThreshold) {
                // Replace outlier bar with previous bar's close prices
                Bar prev = cleaned.get(cleaned.size() - 1);
                Bar corrected = new Bar(
                    bars.get(i).symbol(),
                    bars.get(i).timestamp(),
                    bars.get(i).frequency(),
                    prev.close(), prev.close(), prev.close(), prev.close(),
                    bars.get(i).volume(), bars.get(i).amount()
                );
                cleaned.add(corrected);
            } else {
                cleaned.add(bars.get(i));
            }
        }
        return cleaned;
    }
}
