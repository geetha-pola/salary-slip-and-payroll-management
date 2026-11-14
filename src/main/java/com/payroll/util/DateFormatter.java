package com.payroll.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Standard corporate date formatting routines.
 */
public final class DateFormatter {

    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter PAY_PERIOD_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateFormatter() {
    }

    public static String formatDisplay(LocalDate date) {
        if (date == null) return "-";
        return date.format(DISPLAY_DATE);
    }

    public static String formatIso(LocalDate date) {
        if (date == null) return "";
        return date.format(ISO_DATE);
    }

    public static LocalDate parseIso(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return LocalDate.parse(text.trim(), ISO_DATE);
    }
}

