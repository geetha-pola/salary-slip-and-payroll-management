package com.payroll;

import com.payroll.model.ProfessionalTaxPolicy;
import com.payroll.service.ProfessionalTaxCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProfessionalTaxCalculatorTest {

    private ProfessionalTaxCalculator ptCalculator;

    @BeforeEach
    void setUp() {
        ptCalculator = new ProfessionalTaxCalculator();
    }

    @ParameterizedTest(name = "State: {0}, Monthly Gross: {1} => Expected Tax: {2}")
    @CsvSource({
            // Andhra Pradesh slabs
            "Andhra Pradesh, 0.00, 0.00",
            "Andhra Pradesh, 10000.00, 0.00",
            "Andhra Pradesh, 15000.00, 0.00",      // Upper bound slab 1
            "Andhra Pradesh, 15000.01, 150.00",    // Lower bound slab 2
            "Andhra Pradesh, 18500.00, 150.00",
            "Andhra Pradesh, 20000.00, 150.00",    // Upper bound slab 2
            "Andhra Pradesh, 20000.01, 200.00",    // Lower bound slab 3
            "Andhra Pradesh, 45000.00, 200.00",

            // Telangana slabs
            "Telangana, 0.00, 0.00",
            "Telangana, 12000.00, 0.00",
            "Telangana, 15000.00, 0.00",
            "Telangana, 15000.01, 150.00",
            "Telangana, 20000.00, 150.00",
            "Telangana, 20000.01, 200.00",
            "Telangana, 65000.00, 200.00"
    })
    @DisplayName("Verify state professional tax slabs with boundary edge cases")
    void testStateProfessionalTaxSlabs(String state, String grossStr, String expectedTaxStr) {
        BigDecimal gross = new BigDecimal(grossStr);
        BigDecimal expectedTax = new BigDecimal(expectedTaxStr);

        BigDecimal actualTax = ptCalculator.calculateTax(state, gross);
        assertEquals(expectedTax, actualTax);
    }

    @Test
    @DisplayName("Dynamic policy configuration: modifying slabs is supported")
    void testCustomConfigurableSlabs() {
        // Create custom policy for Telangana with 4 slabs
        ProfessionalTaxPolicy customPolicy = new ProfessionalTaxPolicy("Telangana", List.of(
                new ProfessionalTaxPolicy.TaxSlab(BigDecimal.ZERO, new BigDecimal("10000.00"), BigDecimal.ZERO),
                new ProfessionalTaxPolicy.TaxSlab(new BigDecimal("10000.01"), new BigDecimal("25000.00"), new BigDecimal("120.00")),
                new ProfessionalTaxPolicy.TaxSlab(new BigDecimal("25000.01"), new BigDecimal("99999999.00"), new BigDecimal("250.00"))
        ));

        ptCalculator.registerPolicy(customPolicy);

        assertEquals(new BigDecimal("0.00"), ptCalculator.calculateTax("Telangana", new BigDecimal("8000.00")));
        assertEquals(new BigDecimal("120.00"), ptCalculator.calculateTax("Telangana", new BigDecimal("18000.00")));
        assertEquals(new BigDecimal("250.00"), ptCalculator.calculateTax("Telangana", new BigDecimal("35000.00")));
    }

    @Test
    @DisplayName("Unsupported or missing state throws IllegalArgumentException")
    void testMissingStatePolicyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ptCalculator.calculateTax("Karnataka", new BigDecimal("25000.00"));
        });
    }
}

