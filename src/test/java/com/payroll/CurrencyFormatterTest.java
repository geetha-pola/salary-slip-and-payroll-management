package com.payroll;

import com.payroll.util.CurrencyFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyFormatterTest {

    @Test
    @DisplayName("Format Indian Rupee currency amounts")
    void testFormatRupees() {
        assertEquals("₹ 45,000.00", CurrencyFormatter.formatRupees(new BigDecimal("45000.00")));
        assertEquals("₹ 1,50,000.50", CurrencyFormatter.formatRupees(new BigDecimal("150000.50")));
        assertEquals("₹ 0.00", CurrencyFormatter.formatRupees(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Convert amounts to words using Indian numbering system")
    void testAmountToWords() {
        assertEquals("Rupees Forty-Five Thousand Only",
                CurrencyFormatter.amountToWords(new BigDecimal("45000.00")));

        assertEquals("Rupees Twelve Thousand Only",
                CurrencyFormatter.amountToWords(new BigDecimal("12000.00")));

        assertEquals("Rupees One Lakh Twenty-Five Thousand Only",
                CurrencyFormatter.amountToWords(new BigDecimal("125000.00")));

        assertEquals("Rupees Ten Thousand Five Hundred and Fifty Paise Only",
                CurrencyFormatter.amountToWords(new BigDecimal("10500.50")));

        assertEquals("Rupees Zero Only",
                CurrencyFormatter.amountToWords(BigDecimal.ZERO));
    }
}

