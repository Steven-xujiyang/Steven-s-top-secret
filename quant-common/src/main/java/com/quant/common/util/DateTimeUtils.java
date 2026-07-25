package com.quant.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Date and time utilities for trading calendar operations.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    public static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York");

    private DateTimeUtils() {
    }

    /**
     * Parse a date string in yyyy-MM-dd format.
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * Parse a datetime string in yyyy-MM-dd HH:mm:ss format.
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * Format a LocalDate to yyyy-MM-dd string.
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Get the number of trading days between two dates (approximate: excludes weekends).
     */
    public static long tradingDaysBetween(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end);
        long weeks = days / 7;
        long remainder = days % 7;
        long weekends = weeks * 2;

        DayOfWeek startDow = start.getDayOfWeek();
        for (int i = 0; i < remainder; i++) {
            DayOfWeek dow = startDow.plus(i);
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekends++;
            }
        }
        return days - weekends;
    }

    /**
     * Check if a date is a weekend.
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * Convert a LocalDateTime to epoch milliseconds.
     */
    public static long toEpochMilli(LocalDateTime dateTime, ZoneId zone) {
        return dateTime.atZone(zone).toInstant().toEpochMilli();
    }
}
