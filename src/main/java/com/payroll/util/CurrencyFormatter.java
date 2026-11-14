package com.payroll.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for Indian Rupee currency formatting and Number-to-Words conversion.
 * Formats according to the Indian numbering system (Lakhs, Crores, Thousands).
 */
public final class CurrencyFormatter {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private CurrencyFormatter() {
    }

    /**
     * Formats monetary BigDecimal to Indian Rupee representation (e.g. ₹ 1,50,000.50).
     */
    public static String formatRupees(BigDecimal amount) {
        if (amount == null) return "₹ 0.00";
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long integerPart = scaled.longValue();
        int decimalPart = scaled.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        String formattedInteger = formatIndianGrouping(integerPart);
        return String.format("₹ %s.%02d", formattedInteger, decimalPart);
    }

    /**
     * Formats monetary BigDecimal to Indian grouping string without currency symbol (e.g. 1,50,000.50).
     */
    public static String format(BigDecimal amount) {
        if (amount == null) return "0.00";
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long integerPart = scaled.longValue();
        int decimalPart = scaled.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        String formattedInteger = formatIndianGrouping(integerPart);
        return String.format("%s.%02d", formattedInteger, decimalPart);
    }

    private static String formatIndianGrouping(long n) {
        if (n < 0) return "-" + formatIndianGrouping(-n);
        if (n < 1000) return String.valueOf(n);

        String s = String.valueOf(n);
        String lastThree = s.substring(s.length() - 3);
        String remaining = s.substring(0, s.length() - 3);

        StringBuilder sb = new StringBuilder();
        while (remaining.length() > 2) {
            String chunk = remaining.substring(remaining.length() - 2);
            sb.insert(0, "," + chunk);
            remaining = remaining.substring(0, remaining.length() - 2);
        }
        sb.insert(0, remaining);
        sb.append(",").append(lastThree);
        return sb.toString();
    }

    /**
     * Converts a monetary BigDecimal amount into English words using the Indian numbering system.
     * Example: 45000.00 -> "Rupees Forty-Five Thousand Only"
     */
    public static String amountToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Rupees Zero Only";
        }

        BigDecimal safeAmount = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = safeAmount.longValue();
        int paise = safeAmount.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        StringBuilder sb = new StringBuilder("Rupees ");
        if (rupees == 0) {
            sb.append("Zero");
        } else {
            sb.append(convertIndianNumber(rupees));
        }

        if (paise > 0) {
            sb.append(" and ").append(convertLessThanOneThousand(paise)).append(" Paise");
        }
        sb.append(" Only");

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String convertIndianNumber(long n) {
        if (n == 0) return "";

        long crores = n / 10000000L;
        long lakhs = (n / 100000L) % 100L;
        long thousands = (n / 1000L) % 100L;
        long hundreds = n % 1000L;

        StringBuilder result = new StringBuilder();

        if (crores > 0) {
            result.append(convertLessThanOneThousand(crores)).append(" Crore ");
        }
        if (lakhs > 0) {
            result.append(convertLessThanOneThousand(lakhs)).append(" Lakh ");
        }
        if (thousands > 0) {
            result.append(convertLessThanOneThousand(thousands)).append(" Thousand ");
        }
        if (hundreds > 0) {
            result.append(convertLessThanOneThousand(hundreds)).append(" ");
        }

        return result.toString().trim();
    }

    private static String convertLessThanOneThousand(long n) {
        StringBuilder result = new StringBuilder();
        if (n >= 100) {
            result.append(ONES[(int) (n / 100)]).append(" Hundred ");
            n %= 100;
        }
        if (n >= 20) {
            int tensIndex = (int) (n / 10);
            int onesIndex = (int) (n % 10);
            if (onesIndex > 0) {
                result.append(TENS[tensIndex]).append("-").append(ONES[onesIndex]).append(" ");
            } else {
                result.append(TENS[tensIndex]).append(" ");
            }
        } else if (n > 0) {
            result.append(ONES[(int) n]).append(" ");
        }
        return result.toString().trim();
    }
}
