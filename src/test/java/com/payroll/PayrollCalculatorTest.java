package com.payroll;

import com.payroll.model.*;
import com.payroll.service.PayrollCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PayrollCalculatorTest {

    private PayrollCalculator calculator;
    private DesignationPolicy permanentSeniorDevPolicy;
    private DesignationPolicy benchSeniorDevPolicy;

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();

        // Permanent Senior Dev Policy (Sample demo values from Section 4)
        permanentSeniorDevPolicy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"), // Basic
                new BigDecimal("4000.00"),  // HRA
                new BigDecimal("2500.00"),  // DA
                new BigDecimal("1200.00"),  // Conveyance
                BigDecimal.ZERO,            // Other earnings
                BigDecimal.ZERO,            // Daily rate
                true,                       // PF applicable
                new BigDecimal("0.12"),     // Employee PF rate: 12%
                new BigDecimal("0.12"),     // Employer PF rate: 12%
                "BASIC_DA",                 // PF Wage Base: Basic + DA
                BigDecimal.ZERO,            // Other deductions
                true
        );

        // Bench Senior Dev Policy (Sample demo values from Section 4)
        benchSeniorDevPolicy = new DesignationPolicy(
                "Senior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1500.00"),  // Daily Rate
                true,                       // PF applicable
                new BigDecimal("0.12"),
                new BigDecimal("0.12"),
                "BASIC",
                BigDecimal.ZERO,
                true
        );
    }

    private PermanentEmployee createPermanentEmployee(String id, String location, boolean pfApplicable) {
        return new PermanentEmployee(
                "Rahul", "Sharma", id, LocalDate.of(1995, 5, 10),
                "rahul@example.com", "9876543210", "Married", "Hyderabad",
                "Engineering", "Senior Developer", location,
                LocalDate.of(2021, 6, 1), true, "PF12345", "UAN12345",
                pfApplicable, false, null
        );
    }

    private BenchEmployee createBenchEmployee(String id, String location, boolean pfApplicable) {
        return new BenchEmployee(
                "Ananya", "Reddy", id, LocalDate.of(1998, 8, 20),
                "ananya@example.com", "9876501234", "Single", "Visakhapatnam",
                "Engineering", "Senior Developer", location,
                LocalDate.of(2022, 1, 15), true, "PF67890", "UAN67890",
                pfApplicable, false, null
        );
    }

    @Test
    @DisplayName("Permanent employee with 100% attendance in Telangana")
    void testPermanentEmployeeFullAttendance() {
        Employee emp = createPermanentEmployee("EMP001", "Telangana", true);
        AttendanceRecord att = new AttendanceRecord("EMP001", "10-2023", 20, 20);

        PayrollRecord record = calculator.calculatePayroll(
                emp, permanentSeniorDevPolicy, att, null, "10-2023", LocalDate.of(2023, 10, 31)
        );

        // Gross = Basic (40000) + HRA (4000) + DA (2500) + Conveyance (1200) = 47,700.00
        assertEquals(new BigDecimal("40000.00"), record.getPayableBasic());
        assertEquals(new BigDecimal("4000.00"), record.getPayableHra());
        assertEquals(new BigDecimal("2500.00"), record.getPayableDa());
        assertEquals(new BigDecimal("1200.00"), record.getPayableConveyance());
        assertEquals(new BigDecimal("47700.00"), record.getGrossEarnings());

        // PF Base = Basic + DA = 40000 + 2500 = 42,500.00
        // Employee PF = 42,500 * 12% = 5,100.00
        // Employer PF = 42,500 * 12% = 5,100.00
        assertEquals(new BigDecimal("5100.00"), record.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("5100.00"), record.getPfEmployerContribution());

        // Professional Tax in Telangana for Gross > 20,000 is 200.00
        assertEquals(new BigDecimal("200.00"), record.getProfessionalTax());

        // Total Deductions = 5100 (PF) + 200 (PT) = 5,300.00
        assertEquals(new BigDecimal("5300.00"), record.getTotalDeductions());

        // Net Salary = Gross (47700) - Deductions (5300) = 42,400.00
        assertEquals(new BigDecimal("42400.00"), record.getNetSalary());

        // Verify Employer PF does NOT reduce Net Salary
        assertTrue(record.getGrossEarnings().subtract(record.getTotalDeductions()).compareTo(record.getNetSalary()) == 0);
    }

    @Test
    @DisplayName("Permanent employee with partial attendance (prorated calculation)")
    void testPermanentEmployeePartialAttendance() {
        Employee emp = createPermanentEmployee("EMP002", "Andhra Pradesh", true);
        // 20 present out of 25 working days -> attendance factor = 0.8
        AttendanceRecord att = new AttendanceRecord("EMP002", "10-2023", 25, 20);

        PayrollRecord record = calculator.calculatePayroll(
                emp, permanentSeniorDevPolicy, att, null, "10-2023", LocalDate.of(2023, 10, 31)
        );

        // Attendance factor = 0.8
        // Payable Basic = 40,000 * 0.8 = 32,000.00
        // Payable HRA = 4,000 * 0.8 = 3,200.00
        // Payable DA = 2,500 * 0.8 = 2,000.00
        // Payable Conveyance = 1,200 * 0.8 = 960.00
        // Gross = 32000 + 3200 + 2000 + 960 = 38,160.00
        assertEquals(new BigDecimal("32000.00"), record.getPayableBasic());
        assertEquals(new BigDecimal("3200.00"), record.getPayableHra());
        assertEquals(new BigDecimal("2000.00"), record.getPayableDa());
        assertEquals(new BigDecimal("960.00"), record.getPayableConveyance());
        assertEquals(new BigDecimal("38160.00"), record.getGrossEarnings());

        // PF Base = 32000 + 2000 = 34,000.00
        // Employee PF = 34000 * 0.12 = 4,080.00
        assertEquals(new BigDecimal("4080.00"), record.getPfEmployeeDeduction());

        // PT in AP for > 20000 is 200.00
        assertEquals(new BigDecimal("200.00"), record.getProfessionalTax());

        // Net Salary = 38160 - (4080 + 200) = 33,880.00
        assertEquals(new BigDecimal("33880.00"), record.getNetSalary());
    }

    @Test
    @DisplayName("Bench employee calculation for eligible project days")
    void testBenchEmployeeSalaryCalculation() {
        Employee emp = createBenchEmployee("BENCH001", "Telangana", true);
        // 8 project-working days @ ₹1,500 per day
        ProjectRecord proj = new ProjectRecord("BENCH001", "10-2023", "Internal ERP", 8);

        PayrollRecord record = calculator.calculatePayroll(
                emp, benchSeniorDevPolicy, null, proj, "10-2023", LocalDate.of(2023, 10, 31)
        );

        // Bench Gross = 1500 * 8 = 12,000.00
        assertEquals(new BigDecimal("12000.00"), record.getGrossEarnings());
        assertEquals(new BigDecimal("12000.00"), record.getPayableBasic());

        // STRICT REQUIREMENT: Bench employees receive NO HRA, NO DA, NO Conveyance
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getPayableHra());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getPayableDa());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getPayableConveyance());

        // PF on 12,000 @ 12% = 1,440.00
        assertEquals(new BigDecimal("1440.00"), record.getPfEmployeeDeduction());

        // Professional tax for <= 15,000 is ₹0.00
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getProfessionalTax());

        // Net Salary = 12000 - 1440 = 10,560.00
        assertEquals(new BigDecimal("10560.00"), record.getNetSalary());
    }

    @Test
    @DisplayName("Bench employee with ZERO eligible project days")
    void testBenchEmployeeZeroProjectDays() {
        Employee emp = createBenchEmployee("BENCH002", "Andhra Pradesh", true);
        ProjectRecord proj = new ProjectRecord("BENCH002", "10-2023", "Unassigned", 0);

        PayrollRecord record = calculator.calculatePayroll(
                emp, benchSeniorDevPolicy, null, proj, "10-2023", LocalDate.of(2023, 10, 31)
        );

        // Gross = ₹0.00
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getGrossEarnings());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getPfEmployeeDeduction());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getProfessionalTax());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), record.getNetSalary());
    }

    @Test
    @DisplayName("Attendance validation: present days cannot exceed total working days")
    void testInvalidAttendancePresentExceedsWorking() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AttendanceRecord("EMP001", "10-2023", 20, 22);
        });
    }

    @Test
    @DisplayName("Attendance validation: working days must be greater than zero")
    void testInvalidAttendanceZeroWorkingDays() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AttendanceRecord("EMP001", "10-2023", 0, 0);
        });
    }

    @Test
    @DisplayName("Bench validation: negative project days are rejected")
    void testInvalidBenchProjectDaysNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ProjectRecord("BENCH001", "10-2023", "Project A", -2);
        });
    }

    @Test
    @DisplayName("Permanent employee missing attendance throws IllegalArgumentException")
    void testPermanentMissingAttendanceThrows() {
        Employee emp = createPermanentEmployee("EMP003", "Telangana", true);
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculatePayroll(emp, permanentSeniorDevPolicy, null, null, "10-2023", LocalDate.now());
        });
    }

    @Test
    @DisplayName("Bench employee missing project record throws IllegalArgumentException")
    void testBenchMissingProjectThrows() {
        Employee emp = createBenchEmployee("BENCH003", "Telangana", true);
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculatePayroll(emp, benchSeniorDevPolicy, null, null, "10-2023", LocalDate.now());
        });
    }
}

