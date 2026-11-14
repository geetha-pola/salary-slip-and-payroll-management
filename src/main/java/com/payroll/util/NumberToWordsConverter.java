package com.payroll.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for converting monetary amounts into Indian Rupee currency words.
 * Formats: "Rupees <Amount> Only" or "Rupees <Amount> and <Paise> Paise Only".
 * Follows standard Indian numbering conventions (Crores, Lakhs, Thousands, Hundreds).
 */
public class NumberToWordsConverter {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /**
     * Converts a BigDecimal monetary value to words in Indian Rupees.
     * Example: 39750.00 -> "Rupees Thirty-Nine Thousand Seven Hundred Fifty Only"
     * Example: 33333.33 -> "Rupees Thirty-Three Thousand Three Hundred Thirty-Three and Thirty-Three Paise Only"
     */
    public static String convertToIndianCurrencyWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Rupees Zero Only";
        }

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = scaled.longValue();
        int paise = scaled.remainder(BigDecimal.ONE).multiply(new BigDecimal("100")).intValue();

        StringBuilder result = new StringBuilder("Rupees ");

        if (rupees > 0) {
            result.append(convertNumberToWords(rupees));
        } else {
            result.append("Zero");
        }

        if (paise > 0) {
            result.append(" and ").append(convertNumberToWords(paise)).append(" Paise");
        }

        result.append(" Only");
        return result.toString().trim();
    }

    /**
     * Recursive converter for positive integers using Indian numbering system
     * (Crores = 10,000,000; Lakhs = 100,000; Thousands = 1,000; Hundreds = 100).
     */
    public static String convertNumberToWords(long n) {
        if (n == 0) {
            return "Zero";
        }

        if (n < 0) {
            return "Minus " + convertNumberToWords(-n);
        }

        StringBuilder words = new StringBuilder();

        // Crores (1,00,00,000)
        if (n >= 10000000) {
            words.append(convertNumberToWords(n / 10000000)).append(" Crore ");
            n %= 10000000;
        }

        // Lakhs (1,00,000)
        if (n >= 100000) {
            words.append(convertNumberToWords(n / 100000)).append(" Lakh ");
            n %= 100000;
        }

        // Thousands (1,000)
        if (n >= 1000) {
            words.append(convertNumberToWords(n / 1000)).append(" Thousand ");
            n %= 1000;
        }

        // Hundreds (100)
        if (n >= 100) {
            words.append(convertNumberToWords(n / 100)).append(" Hundred ");
            n %= 100;
        }

        if (n > 0) {
            if (n < 20) {
                words.append(UNITS[(int) n]).append(" ");
            } else {
                words.append(TENS[(int) (n / 10)]);
                if (n % 10 > 0) {
                    words.append("-").append(UNITS[(int) (n % 10)]);
                }
                words.append(" ");
            }
        }

        return words.toString().trim();
    }
}
