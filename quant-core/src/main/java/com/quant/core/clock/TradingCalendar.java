package com.quant.core.clock;

import com.quant.common.enums.Market;
import com.quant.common.util.DateTimeUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Trading calendar aware of market sessions and holidays.
 * Currently implements basic weekend checks; extend with specific holiday calendars.
 */
public class TradingCalendar {

    private final Market market;
    private final Set<LocalDate> holidays;
    private final LocalTime sessionOpen;
    private final LocalTime sessionClose;

    public TradingCalendar(Market market, Set<LocalDate> holidays) {
        this.market = market;
        this.holidays = holidays;
        switch (market) {
            case ASHARE -> {
                this.sessionOpen = LocalTime.of(9, 30);
                this.sessionClose = LocalTime.of(15, 0);
            }
            case US -> {
                this.sessionOpen = LocalTime.of(9, 30);
                this.sessionClose = LocalTime.of(16, 0);
            }
            case HK -> {
                this.sessionOpen = LocalTime.of(9, 30);
                this.sessionClose = LocalTime.of(16, 0);
            }
            default -> {
                this.sessionOpen = LocalTime.MIDNIGHT;
                this.sessionClose = LocalTime.of(23, 59, 59);
            }
        }
    }

    /**
     * Create a calendar with no specific holidays (weekends only).
     */
    public static TradingCalendar weekendsOnly(Market market) {
        return new TradingCalendar(market, Set.of());
    }

    /**
     * Check if a date is a trading day.
     */
    public boolean isTradingDay(LocalDate date) {
        if (DateTimeUtils.isWeekend(date)) return false;
        return !holidays.contains(date);
    }

    /**
     * Check if a time is during the trading session.
     */
    public boolean isSessionOpen(LocalTime time) {
        return !time.isBefore(sessionOpen) && !time.isAfter(sessionClose);
    }

    /**
     * Get the next trading day on or after the given date.
     */
    public LocalDate nextTradingDay(LocalDate date) {
        LocalDate next = date;
        while (!isTradingDay(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    /**
     * Add trading days to a date.
     */
    public LocalDate addTradingDays(LocalDate date, int days) {
        LocalDate result = date;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (isTradingDay(result)) added++;
        }
        return result;
    }

    // Getters
    public Market getMarket() { return market; }
    public Set<LocalDate> getHolidays() { return holidays; }
    public LocalTime getSessionOpen() { return sessionOpen; }
    public LocalTime getSessionClose() { return sessionClose; }
}
