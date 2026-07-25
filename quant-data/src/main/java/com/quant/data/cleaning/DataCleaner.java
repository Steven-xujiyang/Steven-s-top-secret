package com.quant.data.cleaning;

import com.quant.core.model.market.Bar;

import java.util.List;

/**
 * Interface for data cleaning pipeline stages.
 */
public interface DataCleaner {

    /**
     * Clean a list of bars and return the cleaned result.
     *
     * @param bars input bars (chronologically ordered)
     * @return cleaned bars
     */
    List<Bar> clean(List<Bar> bars);

    /**
     * Chain this cleaner with another.
     */
    default DataCleaner andThen(DataCleaner next) {
        return bars -> next.clean(this.clean(bars));
    }

    /**
     * Create a standard cleaning pipeline:
     * outlier detection → missing value handling → corporate action adjustment.
     */
    static DataCleaner standardPipeline() {
        return new OutlierDetector(3.0)
            .andThen(new MissingValueHandler())
            .andThen(new CorporateActionAdjuster());
    }
}
