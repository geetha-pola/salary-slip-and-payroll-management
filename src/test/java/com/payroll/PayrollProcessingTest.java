package com.payroll;

import com.payroll.controller.MainLayoutController;
import com.payroll.controller.PayrollController;
import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PFCalculator;
import com.payroll.service.PayrollCalculator;
import com.payroll.service.ProfessionalTaxCalculator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for Stage 5: Monthly Payroll Processing.
 * Covers:
 * 1. Permanent employee payroll calculations (full attendance and LOP pro-rating).
 * 2. Bench employee payroll calculations (daily rate * project days; strictly ₹0 HRA/DA/Conveyance).
 * 3. PF configuration interactions (global inactive, unconfigured rates validation, active calculation).
 * 4. Professional Tax calculation across AP and Telangana.
 * 5. Separation of Employer PF from employee Net Salary.
 * 6. Duplicate payroll prevention.
 * 7. All Section 8 validation edge cases.
 * 8. JavaFX FXML loading and Controller verification on FX Application Thread.
 */
public class PayrollProcessingTest {

    private PayrollCalculator calculator;
    private DesignationPolicy permPolicy;
    private DesignationPolicy benchPolicy;

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
        } catch (IllegalStateException e) {
            Platform.setImplicitExit(false);
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform startup timed out");
        AppContext.getInstance().getAuthService().authenticate("1234", "5678");
    }

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();

        permPolicy = new DesignationPolicy(
                "Lead Architect", "Permanent",
                new BigDecimal("60000.00"), // Basic
                new BigDecimal("6000.00"),  // HRA
                new BigDecimal("3000.00"),  // DA
                new BigDecimal("2000.00"),  // Conveyance
                BigDecimal.ZERO,            // Other earnings
                BigDecimal.ZERO,            // Daily rate (Permanent N/A)
                true,                       // PF eligible
                new BigDecimal("0.12"),     // Employee PF rate
                new BigDecimal("0.12"),     // Employer PF rate
                "BASIC_DA",                 // PF wage base
                BigDecimal.ZERO,            // Other deductions
                true
        );

        benchPolicy = new DesignationPolicy(
                "Consultant", "Bench",
                BigDecimal.ZERO,            // Monthly basic (Bench N/A)
                BigDecimal.ZERO,            // HRA
                BigDecimal.ZERO,            // DA
                BigDecimal.ZERO,            // Conveyance
                BigDecimal.ZERO,            // Other earnings
                new BigDecimal("2500.00"),  // Daily rate
                true,                       // PF eligible
                new BigDecimal("0.12"),     // Employee PF rate
                new BigDecimal("0.12"),     // Employer PF rate
                "BASIC",                    // PF wage base
                BigDecimal.ZERO,            // Other deductions
                true
        );
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX Thread action timed out");
        if (error.get() != null) {
            if (error.get() instanceof RuntimeException) {
                throw (RuntimeException) error.get();
            }
            throw new RuntimeException(error.get());
        }
    }

    private PermanentEmployee createPermanentEmployee(String id, String location, boolean active, boolean pfApplicable) {
        return new PermanentEmployee(
                "Suresh", "Kumar", id, LocalDate.of(1990, 5, 12),
                "suresh@example.com", "9876543210", "Married", "Hyderabad",
                "Technology", "Lead Architect", location,
                LocalDate.of(2020, 1, 10), active, "PF-1001", "UAN-1001",
                pfApplicable, false, null
        );
    }

    private BenchEmployee createBenchEmployee(String id, String location, boolean active, boolean pfApplicable) {
        return new BenchEmployee(
                "Pooja", "Sharma", id, LocalDate.of(1995, 3, 22),
                "pooja@example.com", "9876543222", "Single", "Vijayawada",
                "Consulting", "Consultant", location,
                LocalDate.of(2021, 8, 1), active, "PF-2002", "UAN-2002",
                pfApplicable, false, null
        );
    }

    @Test
    @DisplayName("1. Permanent Employee Payroll: 100% attendance (30/30 days)")
    void testPermanentPayrollFullAttendance() {
        PermanentEmployee emp = createPermanentEmployee("EMP-P1", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-P1", "2026-11", 30, 30);

        PayrollRecord rec = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.of(2026, 11, 30));

        // Earnings
        assertEquals(new BigDecimal("60000.00"), rec.getPayableBasic());
        assertEquals(new BigDecimal("6000.00"), rec.getPayableHra());
        assertEquals(new BigDecimal("3000.00"), rec.getPayableDa());
        assertEquals(new BigDecimal("2000.00"), rec.getPayableConveyance());
        assertEquals(new BigDecimal("71000.00"), rec.getGrossEarnings());

        // Deductions
        // PF Base = Basic + DA = 60000 + 3000 = 63,000.00. 12% = 7,560.00
        assertEquals(new BigDecimal("7560.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("7560.00"), rec.getPfEmployerContribution());

        // PT in Telangana for Gross > 20,000 = 200.00
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());
        assertEquals(new BigDecimal("7760.00"), rec.getTotalDeductions());

        // Net Salary = 71000 - 7760 = 63,240.00
        assertEquals(new BigDecimal("63240.00"), rec.getNetSalary());

        // Verify Employer PF did NOT reduce Net Salary
        assertEquals(rec.getGrossEarnings().subtract(rec.getTotalDeductions()), rec.getNetSalary());
    }

    @Test
    @DisplayName("2. Permanent Employee Payroll: Pro-rated partial attendance (24/30 days, 6 LOP)")
    void testPermanentPayrollPartialAttendanceLop() {
        PermanentEmployee emp = createPermanentEmployee("EMP-P2", "Andhra Pradesh", true, true);
        // Factor = 24 / 30 = 0.80
        AttendanceRecord att = new AttendanceRecord("EMP-P2", "2026-11", 30, 24);

        PayrollRecord rec = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.of(2026, 11, 30));

        // Pro-rated earnings (each * 0.8)
        assertEquals(new BigDecimal("48000.00"), rec.getPayableBasic());
        assertEquals(new BigDecimal("4800.00"), rec.getPayableHra());
        assertEquals(new BigDecimal("2400.00"), rec.getPayableDa());
        assertEquals(new BigDecimal("1600.00"), rec.getPayableConveyance());
        assertEquals(new BigDecimal("56800.00"), rec.getGrossEarnings());

        // Pro-rated PF: Base = 48000 + 2400 = 50,400.00. 12% = 6,048.00
        assertEquals(new BigDecimal("6048.00"), rec.getPfEmployeeDeduction());

        // PT in AP for > 20000 is 200.00
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());
        assertEquals(new BigDecimal("6248.00"), rec.getTotalDeductions());

        // Net = 56800 - 6248 = 50,552.00
        assertEquals(new BigDecimal("50552.00"), rec.getNetSalary());
    }

    @Test
    @DisplayName("3. Bench Employee Payroll: Daily rate * eligible project days (Strictly ₹0 HRA/DA/Conveyance)")
    void testBenchPayrollEligibleProjectDays() {
        BenchEmployee emp = createBenchEmployee("BENCH-B1", "Telangana", true, true);
        ProjectRecord proj = new ProjectRecord("BENCH-B1", "2026-11", "FinTech Core", 12);

        PayrollRecord rec = calculator.calculatePayroll(emp, benchPolicy, null, proj, "2026-11", LocalDate.of(2026, 11, 30));

        // Gross = 2500 * 12 = 30,000.00
        assertEquals(new BigDecimal("30000.00"), rec.getPayableBasic());
        assertEquals(new BigDecimal("30000.00"), rec.getGrossEarnings());

        // Strict Rule: Bench employees receive NO HRA, NO DA, NO Conveyance
        assertEquals(new BigDecimal("0.00"), rec.getPayableHra());
        assertEquals(new BigDecimal("0.00"), rec.getPayableDa());
        assertEquals(new BigDecimal("0.00"), rec.getPayableConveyance());

        // PF on 30,000 @ 12% = 3,600.00
        assertEquals(new BigDecimal("3600.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("3600.00"), rec.getPfEmployerContribution());

        // PT in Telangana for 30,000 is 200.00
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());

        // Net = 30000 - (3600 + 200) = 26,200.00
        assertEquals(new BigDecimal("26200.00"), rec.getNetSalary());

        // Project snapshots stored
        assertEquals("FinTech Core", rec.getProjectName());
        assertEquals(12, rec.getEligibleProjectDays());
    }

    @Test
    @DisplayName("4. Bench Employee Payroll: ZERO eligible project days results in ₹0 gross and net")
    void testBenchPayrollZeroProjectDays() {
        BenchEmployee emp = createBenchEmployee("BENCH-B2", "Andhra Pradesh", true, true);
        ProjectRecord proj = new ProjectRecord("BENCH-B2", "2026-11", "Bench Pool", 0);

        PayrollRecord rec = calculator.calculatePayroll(emp, benchPolicy, null, proj, "2026-11", LocalDate.of(2026, 11, 30));

        assertEquals(new BigDecimal("0.00"), rec.getGrossEarnings());
        assertEquals(new BigDecimal("0.00"), rec.getPayableBasic());
        assertEquals(new BigDecimal("0.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), rec.getProfessionalTax());
        assertEquals(new BigDecimal("0.00"), rec.getNetSalary());
    }

    @Test
    @DisplayName("5. Global PF Policy: When inactive, PF deduction is ₹0.00 even if employee and designation are eligible")
    void testGlobalPfInactiveYieldsZeroDeduction() {
        PermanentEmployee emp = createPermanentEmployee("EMP-PF-OFF", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-PF-OFF", "2026-11", 30, 30);

        PFPolicy globalInactive = new PFPolicy(false, new BigDecimal("0.12"), new BigDecimal("0.12"), "BASIC_DA", null, false);

        PayrollRecord rec = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.of(2026, 11, 30), globalInactive);

        assertEquals(new BigDecimal("0.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), rec.getPfEmployerContribution());
    }

    @Test
    @DisplayName("6. Global PF Policy: When active but rates unconfigured, throws IllegalStateException")
    void testGlobalPfActiveUnconfiguredThrows() {
        PermanentEmployee emp = createPermanentEmployee("EMP-PF-UNCONF", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-PF-UNCONF", "2026-11", 30, 30);

        PFPolicy globalUnconfigured = new PFPolicy(true, null, null, "BASIC_DA", null, false);

        assertThrows(IllegalStateException.class, () -> {
            calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.of(2026, 11, 30), globalUnconfigured);
        });
    }

    @Test
    @DisplayName("7. Global PF Policy: When active with configured rates, correctly calculates deductions")
    void testGlobalPfActiveConfiguredCalculates() {
        PermanentEmployee emp = createPermanentEmployee("EMP-PF-OK", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-PF-OK", "2026-11", 30, 30);

        PFPolicy globalActive = new PFPolicy(true, new BigDecimal("0.10"), new BigDecimal("0.10"), "BASIC_DA", null, false);

        PayrollRecord rec = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.of(2026, 11, 30), globalActive);

        // 63,000 * 10% = 6,300.00
        assertEquals(new BigDecimal("6300.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("6300.00"), rec.getPfEmployerContribution());
    }

    @Test
    @DisplayName("8. Validation: Inactive employee throws IllegalStateException")
    void testValidationInactiveEmployeeThrows() {
        PermanentEmployee inactiveEmp = createPermanentEmployee("EMP-INACTIVE", "Telangana", false, true);
        AttendanceRecord att = new AttendanceRecord("EMP-INACTIVE", "2026-11", 30, 30);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            calculator.calculatePayroll(inactiveEmp, permPolicy, att, null, "2026-11", LocalDate.now());
        });
        assertTrue(ex.getMessage().contains("Cannot calculate payroll for inactive employee"));
    }

    @Test
    @DisplayName("9. Validation: Null employee or empty ID throws IllegalArgumentException")
    void testValidationNullOrEmptyEmployeeIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculatePayroll(null, permPolicy, null, null, "2026-11", LocalDate.now());
        });

        PermanentEmployee blankIdEmp = new PermanentEmployee(
                "Test", "User", "   ", LocalDate.of(1990, 1, 1),
                "t@test.com", "9876543210", "Single", "Hyderabad",
                "IT", "Dev", "Telangana", LocalDate.now(), true,
                "PF1", "UAN1", false, false, null
        );
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculatePayroll(blankIdEmp, permPolicy, null, null, "2026-11", LocalDate.now());
        });
    }

    @Test
    @DisplayName("10. Validation: Zero working days and present exceeding total throws IllegalArgumentException")
    void testValidationInvalidAttendanceThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AttendanceRecord("EMP-1", "2026-11", 0, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AttendanceRecord("EMP-1", "2026-11", 30, 31);
        });
    }

    @Test
    @DisplayName("11. Validation: Negative or excessive bench project days throws IllegalArgumentException")
    void testValidationInvalidBenchProjectDaysThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ProjectRecord("BENCH-1", "2026-11", "Proj", -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ProjectRecord("BENCH-1", "2026-11", "Proj", 32);
        });
    }

    @Test
    @DisplayName("12. Validation: Null designation policy throws IllegalArgumentException")
    void testValidationNullDesignationPolicyThrows() {
        PermanentEmployee emp = createPermanentEmployee("EMP-P1", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-P1", "2026-11", 30, 30);

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculatePayroll(emp, null, att, null, "2026-11", LocalDate.now());
        });
    }

    @Test
    @DisplayName("13. Validation: Duplicate payroll for same employee and period throws IllegalStateException")
    void testDuplicatePayrollRejection(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("payroll_test.csv");
        PayrollRepository testRepo = new PayrollRepository(csvFile);

        PermanentEmployee emp = createPermanentEmployee("EMP-DUP", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-DUP", "2026-11", 30, 30);
        PayrollRecord rec1 = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.now());

        testRepo.save(rec1);
        assertTrue(testRepo.exists("EMP-DUP", "2026-11"));

        // Attempting to save a second record for the same employee and period must be rejected
        PayrollRecord rec2 = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-11", LocalDate.now());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            testRepo.save(rec2);
        });
        assertTrue(ex.getMessage().contains("Duplicate Payroll Processing"));
    }

    @Test
    @DisplayName("14. Historical Integrity: Updating employee later does not alter previously saved payroll record")
    void testPayrollSnapshotImmutability(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("payroll_snap.csv");
        PayrollRepository testRepo = new PayrollRepository(csvFile);

        PermanentEmployee emp = createPermanentEmployee("EMP-SNAP", "Telangana", true, true);
        AttendanceRecord att = new AttendanceRecord("EMP-SNAP", "2026-10", 30, 30);
        PayrollRecord rec = calculator.calculatePayroll(emp, permPolicy, att, null, "2026-10", LocalDate.of(2026, 10, 31));

        testRepo.save(rec);

        // Later in November, employee designation changes to Manager with higher salary
        emp.setDesignation("Engineering VP");
        permPolicy.setMonthlyBasicSalary(new BigDecimal("150000.00"));

        // Load record from repo
        PayrollRecord loaded = testRepo.findById(rec.getRecordId()).orElseThrow();
        assertEquals("Lead Architect", loaded.getDesignationSnapshot());
        assertEquals(new BigDecimal("60000.00"), loaded.getPayableBasic());
        assertEquals(new BigDecimal("71000.00"), loaded.getGrossEarnings());
    }

    @Test
    @DisplayName("15. JavaFX: Load payroll.fxml and verify controller initialization and pure UI controls")
    void testPayrollFxmlAndControllerLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/payroll.fxml"));
                Parent view = loader.load();
                assertNotNull(view, "payroll.fxml root view must not be null");

                PayrollController controller = loader.getController();
                assertNotNull(controller, "PayrollController must be instantiated");
            } catch (Exception e) {
                fail("Failed to load payroll.fxml: " + e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("16. JavaFX: Verify MainLayoutController enables Monthly Payroll Processing sidebar button")
    void testMainLayoutPayrollNavigationEnabled() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent view = loader.load();
                assertNotNull(view);

                Button btnPayroll = (Button) loader.getNamespace().get("btnPayroll");
                assertNotNull(btnPayroll, "btnPayroll should exist in namespace");
                assertFalse(btnPayroll.isDisable(), "btnPayroll should be ENABLED for Stage 5");
                assertTrue(btnPayroll.getText().contains("Monthly Payroll Processing"), "btnPayroll text must reflect Stage 5 activation");
            } catch (Exception e) {
                fail("Failed to verify MainLayoutController: " + e.getMessage());
            }
        });
    }
}

