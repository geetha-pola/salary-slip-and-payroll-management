package com.payroll;

import com.payroll.controller.DashboardController;
import com.payroll.controller.PayrollController;
import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PayrollCalculator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.FileHandler;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused verification test strictly executing the user's 10-step payroll persistence requirement:
 * 1. Isolated temporary employee and payroll data.
 * 2. Open Permanent Payroll screen.
 * 3. Complete valid calculation.
 * 4. Process & Save Payroll.
 * 5. Confirm record is saved.
 * 6. Exactly one complete record written to temporary payroll.csv.
 * 7. Simulate restart by recreating the repository from CSV.
 * 8. Retrieve record and verify values and policy snapshot match.
 * 9. Verify Dashboard aggregates the saved payroll record correctly.
 * 10. Attempt duplicate and verify rejection.
 */
public class PayrollPersistenceVerificationTest {

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
    @DisplayName("Focused 10-Step Payroll Persistence & Restart Verification")
    void testCompletePayrollPersistenceLifecycle(@TempDir Path tempDir) throws Exception {
        // Step 1: Use isolated temporary employee and payroll data
        Path tempEmployeeCsv = tempDir.resolve("employees.csv");
        Path tempPayrollCsv = tempDir.resolve("payroll.csv");
        Path tempPoliciesDir = tempDir.resolve("policies");
        Files.createDirectories(tempPoliciesDir);

        EmployeeRepository tempEmployeeRepo = new EmployeeRepository(tempEmployeeCsv);
        PayrollRepository tempPayrollRepo = new PayrollRepository(tempPayrollCsv);
        PolicyRepository tempPolicyRepo = new PolicyRepository(tempPoliciesDir);
        PayrollCalculator calculator = new PayrollCalculator();

        // Seed designation policy: Senior Developer Permanent (Basic 40,000, HRA 4,000, DA 2,500, Conveyance 1,200)
        DesignationPolicy policy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"),
                new BigDecimal("4000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("1200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, null, null, "BASIC_DA", BigDecimal.ZERO, true
        );
        tempPolicyRepo.saveDesignationPolicy(policy);

        // Seed 1 active Permanent employee in isolated temporary repository
        PermanentEmployee emp = new PermanentEmployee(
                "Rohan", "Kapoor", "EMP-PERM-01", LocalDate.of(1990, 8, 14),
                "rohan@test.com", "9876543210", "Married", "Hyderabad",
                "Engineering", "Senior Developer", "Telangana",
                LocalDate.of(2021, 4, 1), true, "PF-ROHAN", "UAN-ROHAN",
                false, false, null
        );
        tempEmployeeRepo.save(emp);
        assertEquals(1, tempEmployeeRepo.loadAll().size());

        String currentPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        AtomicReference<String> savedRecordId = new AtomicReference<>();

        runOnFxThread(() -> {
            try {
                // Step 2: Open the Permanent Payroll screen
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/payroll.fxml"));
                loader.setControllerFactory(param -> new PayrollController(tempEmployeeRepo, tempPolicyRepo, tempPayrollRepo, calculator));
                Parent root = loader.load();
                assertNotNull(root);

                PayrollController controller = loader.getController();
                assertNotNull(controller);
                controller.setShowDialogs(false); // Suppress modal blocking during automated execution

                ComboBox<Employee> permEmployeeCombo = (ComboBox<Employee>) loader.getNamespace().get("permEmployeeCombo");
                TextField permPayPeriodField = (TextField) loader.getNamespace().get("permPayPeriodField");
                TextField permTotalDaysField = (TextField) loader.getNamespace().get("permTotalDaysField");
                TextField permPresentDaysField = (TextField) loader.getNamespace().get("permPresentDaysField");
                TextField permAbsentDaysField = (TextField) loader.getNamespace().get("permAbsentDaysField");
                Label permPayableBasicLabel = (Label) loader.getNamespace().get("permPayableBasicLabel");
                Label permGrossEarningsLabel = (Label) loader.getNamespace().get("permGrossEarningsLabel");
                Label permNetSalaryLabel = (Label) loader.getNamespace().get("permNetSalaryLabel");

                // Step 3: Complete a valid payroll calculation
                permEmployeeCombo.getSelectionModel().select(emp);
                permPayPeriodField.setText(currentPeriod);
                permTotalDaysField.setText("30");
                permPresentDaysField.setText("25");

                assertEquals("5", permAbsentDaysField.getText());
                assertEquals("₹ 33,333.33", permPayableBasicLabel.getText());
                assertEquals("₹ 39,750.00", permGrossEarningsLabel.getText());
                assertEquals("₹ 39,550.00", permNetSalaryLabel.getText()); // 39,750 - 200 (PT in Telangana)

                // Step 4: Click Process & Save Payroll
                controller.handlePermProcess();

            } catch (Exception e) {
                fail("UI processing failed: " + e.getMessage());
            }
        });

        // Step 5: Confirm the final action actually saves the record
        List<PayrollRecord> recordsAfterSave = tempPayrollRepo.loadAll();
        assertEquals(1, recordsAfterSave.size(), "Repository must contain exactly 1 saved payroll record");

        PayrollRecord saved = recordsAfterSave.get(0);
        savedRecordId.set(saved.getRecordId());
        assertEquals("EMP-PERM-01", saved.getEmployeeId());
        assertEquals(currentPeriod, saved.getPayPeriod());
        assertEquals(new BigDecimal("33333.33"), saved.getPayableBasic());
        assertEquals(new BigDecimal("39750.00"), saved.getGrossEarnings());
        assertEquals(new BigDecimal("200.00"), saved.getProfessionalTax());
        assertEquals(new BigDecimal("39550.00"), saved.getNetSalary());

        // Step 6: Verify that exactly one complete record is written to temporary payroll.csv on disk
        assertTrue(Files.exists(tempPayrollCsv), "temporary payroll.csv must exist on disk");
        List<List<String>> csvRows = FileHandler.readCsv(tempPayrollCsv);
        // Header + exactly 1 data row = 2 lines
        assertEquals(2, csvRows.size(), "payroll.csv must have header plus exactly 1 record row");
        List<String> recordRow = csvRows.get(1);
        assertEquals(saved.getRecordId(), recordRow.get(0));
        assertEquals("EMP-PERM-01", recordRow.get(1));
        assertEquals("Rohan Kapoor", recordRow.get(2));
        assertEquals("Senior Developer", recordRow.get(3));
        assertEquals("33333.33", recordRow.get(18)); // payable_basic
        assertEquals("39750.00", recordRow.get(23)); // gross_earnings
        assertEquals("39550.00", recordRow.get(29)); // net_salary

        // Step 7: Close and recreate the repository to simulate an application restart
        PayrollRepository restartedRepo = new PayrollRepository(tempPayrollCsv);

        // Step 8: Verify that the payroll record can still be retrieved with the same amounts and policy snapshot
        Optional<PayrollRecord> reloadedOpt = restartedRepo.findById(savedRecordId.get());
        assertTrue(reloadedOpt.isPresent(), "Record must be retrievable after application restart simulation");

        PayrollRecord reloaded = reloadedOpt.get();
        assertEquals(saved.getRecordId(), reloaded.getRecordId());
        assertEquals("EMP-PERM-01", reloaded.getEmployeeId());
        assertEquals(new BigDecimal("33333.33"), reloaded.getPayableBasic());
        assertEquals(new BigDecimal("3333.33"), reloaded.getPayableHra());
        assertEquals(new BigDecimal("2083.33"), reloaded.getPayableDa());
        assertEquals(new BigDecimal("1000.00"), reloaded.getPayableConveyance());
        assertEquals(new BigDecimal("39750.00"), reloaded.getGrossEarnings());
        assertEquals(new BigDecimal("200.00"), reloaded.getProfessionalTax());
        assertEquals(new BigDecimal("39550.00"), reloaded.getNetSalary());
        assertEquals("Senior Developer", reloaded.getDesignationSnapshot());
        assertNotNull(reloaded.getPolicySnapshot(), "Policy snapshot must be preserved");
        assertTrue(reloaded.getPolicySnapshot().contains("Senior Developer"));
        assertTrue(reloaded.getPolicySnapshot().contains("BaseRate=40000.00"));

        // Step 9: Verify that the dashboard aggregates the saved payroll record correctly
        runOnFxThread(() -> {
            try {
                FXMLLoader dashLoader = new FXMLLoader(getClass().getResource("/com/payroll/dashboard.fxml"));
                dashLoader.setControllerFactory(param -> new DashboardController(tempEmployeeRepo, restartedRepo));
                Parent dashRoot = dashLoader.load();
                assertNotNull(dashRoot);

                DashboardController dashController = dashLoader.getController();
                assertNotNull(dashController);

                Label totalActive = (Label) dashLoader.getNamespace().get("totalActiveEmployeesLabel");
                Label totalMonthlyPay = (Label) dashLoader.getNamespace().get("currentMonthPayrollLabel");
                Label totalRecords = (Label) dashLoader.getNamespace().get("totalPayrollRecordsLabel");

                assertEquals("1", totalActive.getText(), "Dashboard must show 1 active employee");
                assertEquals("₹ 39,550.00", totalMonthlyPay.getText(), "Dashboard must aggregate current month disbursement to ₹ 39,550.00");
                assertEquals("1", totalRecords.getText(), "Dashboard must display 1 total historical payroll record");

            } catch (Exception e) {
                fail("Dashboard aggregation verification failed: " + e.getMessage());
            }
        });

        // Step 10: Attempt the same payroll again and verify duplicate prevention
        assertTrue(restartedRepo.exists("EMP-PERM-01", currentPeriod));

        // Direct repository attempt must throw IllegalStateException
        IllegalStateException dupEx = assertThrows(IllegalStateException.class, () -> restartedRepo.save(saved));
        assertTrue(dupEx.getMessage().contains("Duplicate Payroll Processing"));

        // Controller duplicate attempt
        runOnFxThread(() -> {
            try {
                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/com/payroll/payroll.fxml"));
                loader2.setControllerFactory(param -> new PayrollController(tempEmployeeRepo, tempPolicyRepo, restartedRepo, calculator));
                Parent root2 = loader2.load();
                PayrollController controller2 = loader2.getController();
                controller2.setShowDialogs(false);

                ComboBox<Employee> permCombo2 = (ComboBox<Employee>) loader2.getNamespace().get("permEmployeeCombo");
                TextField payPeriodField2 = (TextField) loader2.getNamespace().get("permPayPeriodField");

                permCombo2.getSelectionModel().select(emp);
                payPeriodField2.setText(currentPeriod);

                // Attempt to process duplicate
                controller2.handlePermProcess();

                // Record count in CSV must remain strictly 1
                assertEquals(1, restartedRepo.loadAll().size(), "Duplicate attempt must not insert a second record");

            } catch (Exception e) {
                fail("Controller duplicate attempt failed: " + e.getMessage());
            }
        });
    }
}

