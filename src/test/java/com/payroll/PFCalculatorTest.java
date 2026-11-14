package com.payroll;

import com.payroll.model.DesignationPolicy;
import com.payroll.model.Employee;
import com.payroll.model.PFPolicy;
import com.payroll.model.PermanentEmployee;
import com.payroll.service.PFCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PFCalculatorTest {

    private PFCalculator pfCalculator;

    @BeforeEach
    void setUp() {
        pfCalculator = new PFCalculator();
    }

    private Employee createEmployee(boolean pfApplicable) {
        return new PermanentEmployee(
                "Suresh", "Kumar", "EMP101", LocalDate.of(1990, 1, 1),
                "suresh@example.com", "9876543210", "Married", "Hyderabad",
                "IT", "Lead Engineer", "Telangana", LocalDate.of(2018, 5, 1),
                true, "PF999", "UAN999", pfApplicable, false, null
        );
    }

    @Test
    @DisplayName("PF calculation requires administrator configuration when PF is applicable")
    void testPfCalculationRequiresExplicitAdminRatesWhenApplicable() {
        Employee emp = createEmployee(true);
        // Policy where PF is marked applicable but administrator has NOT configured rates (rates are null)
        DesignationPolicy unconfiguredPolicy = new DesignationPolicy(
                "Lead Engineer", "Permanent",
                new BigDecimal("50000.00"), new BigDecimal("5000.00"),
                new BigDecimal("3000.00"), new BigDecimal("1500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, // Applicable
                null, null, // UNCONFIGURED RATES
                "BASIC_DA", BigDecimal.ZERO, true
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            pfCalculator.calculate(emp, unconfiguredPolicy,
                    new BigDecimal("50000.00"), new BigDecimal("3000.00"), new BigDecimal("59500.00"));
        });

        assertTrue(ex.getMessage().contains("administrator PF contribution rates have not been configured"));
    }

    @Test
    @DisplayName("PF calculation when PF is applicable and explicitly configured with separate rates")
    void testPfApplicableCalculationWithSeparateRates() {
        Employee emp = createEmployee(true);
        // Administrator explicitly configured asymmetric rates: Employee 10%, Employer 12%
        DesignationPolicy policy = new DesignationPolicy(
                "Lead Engineer", "Permanent",
                new BigDecimal("50000.00"), new BigDecimal("5000.00"),
                new BigDecimal("3000.00"), new BigDecimal("1500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true,
                new BigDecimal("0.10"), // Explicit employee rate: 10%
                new BigDecimal("0.12"), // Explicit employer rate: 12%
                "BASIC_DA",
                BigDecimal.ZERO, true
        );

        BigDecimal payableBasic = new BigDecimal("50000.00");
        BigDecimal payableDa = new BigDecimal("3000.00");
        BigDecimal gross = new BigDecimal("59500.00");

        PFCalculator.PFResult result = pfCalculator.calculate(emp, policy, payableBasic, payableDa, gross);

        assertTrue(result.isApplicable());
        // Wage base = 50,000 + 3,000 = 53,000.00
        assertEquals(new BigDecimal("53000.00"), result.getWageBase());
        // Employee PF = 53,000 * 0.10 = 5,300.00
        assertEquals(new BigDecimal("5300.00"), result.getEmployeeDeduction());
        // Employer PF = 53,000 * 0.12 = 6,360.00 (Kept separate!)
        assertEquals(new BigDecimal("6360.00"), result.getEmployerContribution());
    }

    @Test
    @DisplayName("PF deduction is ZERO when employee-level PF is not applicable")
    void testPfNotApplicableEmployeeLevel() {
        Employee emp = createEmployee(false); // Employee opted out
        DesignationPolicy policy = new DesignationPolicy(
                "Lead Engineer", "Permanent",
                new BigDecimal("50000.00"), new BigDecimal("5000.00"),
                new BigDecimal("3000.00"), new BigDecimal("1500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        );

        PFCalculator.PFResult result = pfCalculator.calculate(emp, policy,
                new BigDecimal("50000.00"), new BigDecimal("3000.00"), new BigDecimal("59500.00"));

        assertFalse(result.isApplicable());
        assertEquals(new BigDecimal("0.00"), result.getEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), result.getEmployerContribution());
    }

    @Test
    @DisplayName("PF deduction is ZERO when policy-level PF is not applicable")
    void testPfNotApplicablePolicyLevel() {
        Employee emp = createEmployee(true);
        DesignationPolicy policy = new DesignationPolicy(
                "Consultant", "Permanent",
                new BigDecimal("50000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, // Policy PF disabled
                null, null, // Rates need not be configured if PF is disabled
                "BASIC", BigDecimal.ZERO, true
        );

        PFCalculator.PFResult result = pfCalculator.calculate(emp, policy,
                new BigDecimal("50000.00"), BigDecimal.ZERO, new BigDecimal("50000.00"));

        assertFalse(result.isApplicable());
        assertEquals(new BigDecimal("0.00"), result.getEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), result.getEmployerContribution());
    }

    @Test
    @DisplayName("Global PF policy throws if marked applicable but unconfigured")
    void testGlobalPfPolicyUnconfiguredThrows() {
        Employee emp = createEmployee(true);
        PFPolicy unconfiguredGlobal = new PFPolicy(true, null, null, "BASIC", null, false);

        assertThrows(IllegalStateException.class, () -> {
            pfCalculator.calculate(emp, unconfiguredGlobal, new BigDecimal("30000.00"), BigDecimal.ZERO, new BigDecimal("30000.00"));
        });
    }

    @Test
    @DisplayName("PF wage base selection: BASIC vs BASIC_DA vs GROSS")
    void testWageBaseSelection() {
        BigDecimal basic = new BigDecimal("40000.00");
        BigDecimal da = new BigDecimal("4000.00");
        BigDecimal gross = new BigDecimal("50000.00");

        assertEquals(new BigDecimal("40000.00"), pfCalculator.determineWageBase("BASIC", basic, da, gross));
        assertEquals(new BigDecimal("44000.00"), pfCalculator.determineWageBase("BASIC_DA", basic, da, gross));
        assertEquals(new BigDecimal("50000.00"), pfCalculator.determineWageBase("GROSS", basic, da, gross));
    }
}
