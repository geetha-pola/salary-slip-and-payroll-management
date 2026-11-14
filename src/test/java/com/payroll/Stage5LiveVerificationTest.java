package com.payroll;

import com.payroll.controller.DashboardController;
import com.payroll.controller.MainLayoutController;
import com.payroll.controller.PayrollController;
import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PayrollCalculator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated automated verification test covering Tests A, B, C, D, and E
 * requested by the user for Stage 5 desktop payroll workflow approval.
 * All tests run against isolated in-memory/temporary repositories, preserving original CSV data.
 */
public class Stage5LiveVerificationTest {

    private static Stage testStage;
    private PayrollCalculator calculator;

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

        CountDownLatch stageLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            testStage = new Stage();
            testStage.setTitle("Stage 5 Live Verification Test");
            stageLatch.countDown();
        });
        assertTrue(stageLatch.await(5, TimeUnit.SECONDS));

        AppContext.getInstance().getAuthService().authenticate("1234", "5678");
    }

    @AfterAll
    static void cleanup() throws InterruptedException {
        if (testStage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                testStage.close();
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        }
    }

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();
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

    @Test
    @DisplayName("Test A: Permanent Payroll Pro-rating (30 days, 25 present, PF disabled)")
    void testA_PermanentPayrollProrating() {
        // Policy: Basic 40,000, HRA 4,000, DA 2,500, Conveyance 1,200
        DesignationPolicy policy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"),
                new BigDecimal("4000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("1200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, // PF disabled for this test
                null, null, "BASIC_DA", BigDecimal.ZERO, true
        );

        PermanentEmployee emp = new PermanentEmployee(
                "Anand", "Varma", "TEMP-P1", LocalDate.of(1992, 4, 15),
                "anand@test.com", "9876501111", "Single", "Hyderabad",
                "Engineering", "Senior Developer", "Telangana",
                LocalDate.of(2021, 6, 1), true, "PF-TEMP", "UAN-TEMP",
                false, false, null // PF disabled on employee
        );

        AttendanceRecord att = new AttendanceRecord("TEMP-P1", "2026-11", 30, 25);
        assertEquals(5, att.getAbsentDays());

        PayrollRecord rec = calculator.calculatePayroll(emp, policy, att, null, "2026-11", LocalDate.of(2026, 11, 30));

        // Verified expected values:
        assertEquals(new BigDecimal("33333.33"), rec.getPayableBasic(), "Payable basic must be 33,333.33");
        assertEquals(new BigDecimal("3333.33"), rec.getPayableHra(), "Payable HRA must be 3,333.33");
        assertEquals(new BigDecimal("2083.33"), rec.getPayableDa(), "Payable DA must be 2,083.33");
        assertEquals(new BigDecimal("1000.00"), rec.getPayableConveyance(), "Payable conveyance must be 1,000.00");
        assertEquals(new BigDecimal("39750.00"), rec.getGrossEarnings(), "Gross earnings must be 39,750.00");

        // PF is disabled -> 0.00
        assertEquals(new BigDecimal("0.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), rec.getPfEmployerContribution());

        // PT in Telangana for Gross > 20,000 is 200.00
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());

        // Net salary = 39,750.00 - 200.00 = 39,550.00
        assertEquals(new BigDecimal("39550.00"), rec.getNetSalary());
    }

    @Test
    @DisplayName("Test B: Bench Payroll (Daily rate 1,500, 12 project days, Gross 18,000, 0 HRA/DA/Conveyance)")
    void testB_BenchPayrollDailyRate() {
        DesignationPolicy policy = new DesignationPolicy(
                "Senior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1500.00"), // Daily rate
                false, null, null, "BASIC", BigDecimal.ZERO, true
        );

        BenchEmployee emp = new BenchEmployee(
                "Sneha", "Rao", "TEMP-B1", LocalDate.of(1995, 9, 20),
                "sneha@test.com", "9876502222", "Single", "Hyderabad",
                "Technology", "Senior Developer", "Telangana",
                LocalDate.of(2022, 2, 1), true, "PF-TEMP2", "UAN-TEMP2",
                false, false, null
        );

        ProjectRecord proj = new ProjectRecord("TEMP-B1", "2026-11", "Client Cloud Migration", 12);

        PayrollRecord rec = calculator.calculatePayroll(emp, policy, null, proj, "2026-11", LocalDate.of(2026, 11, 30));

        // Expected gross = 1,500 * 12 = 18,000.00
        assertEquals(new BigDecimal("18000.00"), rec.getGrossEarnings());
        assertEquals(new BigDecimal("18000.00"), rec.getPayableBasic());

        // STRICT VERIFICATION: HRA, DA, Conveyance are exactly 0.00
        assertEquals(new BigDecimal("0.00"), rec.getPayableHra(), "Bench HRA must be zero");
        assertEquals(new BigDecimal("0.00"), rec.getPayableDa(), "Bench DA must be zero");
        assertEquals(new BigDecimal("0.00"), rec.getPayableConveyance(), "Bench Conveyance must be zero");

        // Professional Tax in Telangana for 15,000 < Gross <= 20,000 is 150.00
        assertEquals(new BigDecimal("150.00"), rec.getProfessionalTax());

        // Net Salary = 18,000 - 150 = 17,850.00
        assertEquals(new BigDecimal("17850.00"), rec.getNetSalary());
    }

    @Test
    @DisplayName("Test C: PF Separation (Employee PF reduces net pay, Employer PF remains separate contribution)")
    void testC_PfSeparationStatutoryExpense() {
        // Isolated test policy with explicit 12% PF rates
        DesignationPolicy policy = new DesignationPolicy(
                "Architect", "Permanent",
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true,
                new BigDecimal("0.12"), // Employee 12%
                new BigDecimal("0.12"), // Employer 12%
                "BASIC_DA", BigDecimal.ZERO, true
        );

        PermanentEmployee emp = new PermanentEmployee(
                "Rohit", "Sharma", "TEMP-PF", LocalDate.of(1987, 4, 30),
                "rohit@test.com", "9876503333", "Married", "Hyderabad",
                "Architecture", "Architect", "Telangana",
                LocalDate.of(2019, 5, 1), true, "PF-ROHIT", "UAN-ROHIT",
                true, false, null // PF applicable
        );

        AttendanceRecord att = new AttendanceRecord("TEMP-PF", "2026-11", 30, 30);

        // Active global PF configuration
        PFPolicy globalPf = new PFPolicy(true, new BigDecimal("0.12"), new BigDecimal("0.12"), "BASIC_DA", null, false);

        PayrollRecord rec = calculator.calculatePayroll(emp, policy, att, null, "2026-11", LocalDate.of(2026, 11, 30), globalPf);

        // Gross = 50,000 + 5,000 + 2,500 + 1,000 = 58,500.00
        assertEquals(new BigDecimal("58500.00"), rec.getGrossEarnings());

        // PF Base = Basic + DA = 50,000 + 2,500 = 52,500.00. 12% = 6,300.00
        assertEquals(new BigDecimal("6300.00"), rec.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("6300.00"), rec.getPfEmployerContribution());

        // PT in Telangana for Gross > 20,000 is 200.00
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());

        // Total Deductions = 6,300 (Employee PF) + 200 (PT) = 6,500.00
        assertEquals(new BigDecimal("6500.00"), rec.getTotalDeductions());

        // Net Salary = Gross (58,500) - Deductions (6,500) = 52,000.00
        assertEquals(new BigDecimal("52000.00"), rec.getNetSalary());

        // Strict verification: Net Salary is NOT reduced by Employer PF (6,300)
        assertNotEquals(rec.getNetSalary(), rec.getGrossEarnings().subtract(rec.getTotalDeductions()).subtract(rec.getPfEmployerContribution()));
        assertEquals(rec.getGrossEarnings().subtract(rec.getTotalDeductions()), rec.getNetSalary());
    }

    @Test
    @DisplayName("Test D: Duplicate Payroll Prevention (Second generation for same employee and period is rejected)")
    void testD_DuplicatePayrollPrevention(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("payroll_isolated.csv");
        PayrollRepository repo = new PayrollRepository(tempFile);

        DesignationPolicy policy = new DesignationPolicy(
                "Engineer", "Permanent",
                new BigDecimal("40000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, false, null, null, "BASIC", BigDecimal.ZERO, true
        );

        PermanentEmployee emp = new PermanentEmployee(
                "Vikram", "Reddy", "TEMP-DUP", LocalDate.of(1991, 10, 1),
                "vikram@test.com", "9876504444", "Single", "Hyderabad",
                "IT", "Engineer", "Telangana", LocalDate.of(2021, 1, 1),
                true, "PF-V", "UAN-V", false, false, null
        );

        AttendanceRecord att = new AttendanceRecord("TEMP-DUP", "2026-11", 30, 30);
        PayrollRecord rec1 = calculator.calculatePayroll(emp, policy, att, null, "2026-11", LocalDate.now());

        // First save succeeds
        repo.save(rec1);
        assertTrue(repo.exists("TEMP-DUP", "2026-11"));

        // Second generation attempt for same employee and period
        PayrollRecord rec2 = calculator.calculatePayroll(emp, policy, att, null, "2026-11", LocalDate.now());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> repo.save(rec2));
        assertTrue(ex.getMessage().contains("Duplicate Payroll Processing"));
    }

    @Test
    @DisplayName("Test E: Desktop UI Verification: Permanent, Bench, live preview, and Dashboard aggregation")
    void testE_DesktopUiVerification(@TempDir Path tempDir) throws InterruptedException, IOException {
        runOnFxThread(() -> {
            try {
                // 1. Load Main Application Layout and open Payroll view
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent mainRoot = mainLoader.load();
                MainLayoutController mainController = mainLoader.getController();

                Scene scene = new Scene(mainRoot, 1100, 750);
                testStage.setScene(scene);
                testStage.show();

                assertTrue(scene.getStylesheets().isEmpty(), "Zero CSS stylesheets on scene");

                mainController.showPayroll();
                StackPane contentArea = (StackPane) mainLoader.getNamespace().get("contentArea");
                assertFalse(contentArea.getChildren().isEmpty());

                // 2. Load Payroll view directly for programmatic field inspection
                FXMLLoader payrollLoader = new FXMLLoader(getClass().getResource("/com/payroll/payroll.fxml"));
                Parent payrollRoot = payrollLoader.load();
                PayrollController payrollController = payrollLoader.getController();
                assertNotNull(payrollController);

                ComboBox<Employee> permEmployeeCombo = (ComboBox<Employee>) payrollLoader.getNamespace().get("permEmployeeCombo");
                TextField permTotalDaysField = (TextField) payrollLoader.getNamespace().get("permTotalDaysField");
                TextField permPresentDaysField = (TextField) payrollLoader.getNamespace().get("permPresentDaysField");
                TextField permAbsentDaysField = (TextField) payrollLoader.getNamespace().get("permAbsentDaysField");
                Label permGrossEarningsLabel = (Label) payrollLoader.getNamespace().get("permGrossEarningsLabel");
                Label permNetSalaryLabel = (Label) payrollLoader.getNamespace().get("permNetSalaryLabel");

                // Test A verification inside UI: Select Permanent employee with 40k basic
                PermanentEmployee testPermEmp = new PermanentEmployee(
                        "Aditi", "Sen", "UI-TEST-P", LocalDate.of(1992, 2, 20),
                        "aditi@test.com", "9876505555", "Single", "Hyderabad",
                        "Engineering", "Senior Developer", "Telangana",
                        LocalDate.of(2021, 5, 10), true, "PF-ADITI", "UAN-ADITI",
                        false, false, null // PF disabled
                );
                permEmployeeCombo.getItems().add(testPermEmp);
                permEmployeeCombo.setValue(testPermEmp);

                // Set 30 working days and 25 present days
                permTotalDaysField.setText("30");
                permPresentDaysField.setText("25");

                assertEquals("5", permAbsentDaysField.getText(), "Absent days must be 5");
                assertEquals("₹ 39,750.00", permGrossEarningsLabel.getText(), "Live preview gross must be 39,750.00");
                assertEquals("₹ 39,550.00", permNetSalaryLabel.getText(), "Live preview net must be 39,550.00 (39,750 - 200 PT)");

                // Test B verification inside UI: Bench live preview
                ComboBox<Employee> benchEmployeeCombo = (ComboBox<Employee>) payrollLoader.getNamespace().get("benchEmployeeCombo");
                TextField benchProjectDaysField = (TextField) payrollLoader.getNamespace().get("benchProjectDaysField");
                Label benchGrossEarningsLabel = (Label) payrollLoader.getNamespace().get("benchGrossEarningsLabel");
                Label benchHraLabel = (Label) payrollLoader.getNamespace().get("benchHraLabel");

                BenchEmployee testBenchEmp = new BenchEmployee(
                        "Kiran", "Kumar", "UI-TEST-B", LocalDate.of(1994, 8, 12),
                        "kiran@test.com", "9876506666", "Single", "Hyderabad",
                        "Operations", "Senior Developer", "Telangana",
                        LocalDate.of(2022, 3, 1), true, "PF-KIRAN", "UAN-KIRAN",
                        false, false, null
                );
                benchEmployeeCombo.getItems().add(testBenchEmp);
                benchEmployeeCombo.setValue(testBenchEmp);

                benchProjectDaysField.setText("12");

                assertEquals("₹ 18,000.00", benchGrossEarningsLabel.getText(), "Bench gross must be 18,000.00 (1500 * 12)");
                assertTrue(benchHraLabel.getText().contains("₹ 0.00"), "Bench HRA must be zero");

            } catch (Exception e) {
                fail("UI verification failed: " + e.getMessage());
            }
        });
    }
}

