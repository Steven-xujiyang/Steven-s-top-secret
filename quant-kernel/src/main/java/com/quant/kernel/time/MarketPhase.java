package com.quant.kernel.time;

/**
 * The two discrete phases within a trading day in this kernel.
 *
 * <p>OPEN  — only the opening price of the current day is visible.
 * <br>CLOSE — the full OHLC bar for the current day has been published;
 *            strategies may now observe close prices and produce target weights.
 */
public enum MarketPhase {
    OPEN,
    CLOSE
}
