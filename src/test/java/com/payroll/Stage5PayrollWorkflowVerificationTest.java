package com.payroll;

import com.payroll.controller.MainLayoutController;
import com.payroll.controller.PayrollController;
import com.payroll.model.BenchEmployee;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.PermanentEmployee;
import com.payroll.service.AppContext;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end Desktop UI Workflow Verification for Stage 5: Monthly Payroll Processing.
 * Validates the UI controls, live breakdown updates, pro-rating, zero allowances for bench,
 * and zero CSS compliance directly on the JavaFX Application Thread.
 */
public class Stage5PayrollWorkflowVerificationTest {

    private static Stage testStage;

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
            testStage.setTitle("Stage 5 Desktop UI Workflow Verification");
            stageLatch.countDown();
        });
        assertTrue(stageLatch.await(5, TimeUnit.SECONDS));

        // Ensure clean authenticated administrator session
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
    @DisplayName("Verify Stage 5 Monthly Payroll UI Workflow: Permanent Pro-rating, Bench Daily-rate, and Batch controls")
    void testStage5PayrollUiWorkflow() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                // 1. Load Main Application Layout
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent mainRoot = mainLoader.load();
                assertNotNull(mainRoot);
                MainLayoutController mainController = mainLoader.getController();
                assertNotNull(mainController);

                Scene scene = new Scene(mainRoot, 1100, 750);
                testStage.setScene(scene);
                testStage.show();

                // Strict Hackathon Check: Zero CSS stylesheets on Scene
                assertTrue(scene.getStylesheets().isEmpty(), "Strict Rule: Zero CSS stylesheets allowed on scene");

                // 2. Navigate to Monthly Payroll Processing screen
                Button btnPayroll = (Button) mainLoader.getNamespace().get("btnPayroll");
                assertNotNull(btnPayroll);
                assertFalse(btnPayroll.isDisabled(), "Payroll button must be enabled in Stage 5");

                mainController.showPayroll();

                StackPane contentArea = (StackPane) mainLoader.getNamespace().get("contentArea");
                assertNotNull(contentArea);
                assertFalse(contentArea.getChildren().isEmpty(), "Payroll view must be loaded into content area");

                // 3. Inspect Payroll Screen Controls
                ScrollPane scrollPane = (ScrollPane) contentArea.getChildren().get(0);
                assertNotNull(scrollPane);
                assertTrue(scrollPane.getStylesheets().isEmpty(), "Zero CSS stylesheets on scrollPane");

                // Load payroll view directly to access namespace controls
                FXMLLoader payrollLoader = new FXMLLoader(getClass().getResource("/com/payroll/payroll.fxml"));
                Parent payrollRoot = payrollLoader.load();
                PayrollController payrollController = payrollLoader.getController();
                assertNotNull(payrollController);

                TabPane tabPane = (TabPane) payrollLoader.getNamespace().get("tabPane");
                assertNotNull(tabPane);
                assertEquals(3, tabPane.getTabs().size(), "Must have exactly 3 tabs: Permanent, Bench, and Batch");
                assertEquals("Permanent Employee Payroll", tabPane.getTabs().get(0).getText());
                assertEquals("Bench Employee Payroll", tabPane.getTabs().get(1).getText());
                assertEquals("Batch Payroll Run", tabPane.getTabs().get(2).getText());

                // 4. Verify Permanent Tab Controls
                ComboBox<com.payroll.model.Employee> permEmployeeCombo = (ComboBox<com.payroll.model.Employee>) payrollLoader.getNamespace().get("permEmployeeCombo");
                TextField permPayPeriodField = (TextField) payrollLoader.getNamespace().get("permPayPeriodField");
                DatePicker permPayDatePicker = (DatePicker) payrollLoader.getNamespace().get("permPayDatePicker");
                TextField permLocationField = (TextField) payrollLoader.getNamespace().get("permLocationField");
                TextField permDeptField = (TextField) payrollLoader.getNamespace().get("permDeptField");
                TextField permDojField = (TextField) payrollLoader.getNamespace().get("permDojField");
                TextField permPfUanField = (TextField) payrollLoader.getNamespace().get("permPfUanField");
                TextField permPtConfigField = (TextField) payrollLoader.getNamespace().get("permPtConfigField");

                TextField permTotalDaysField = (TextField) payrollLoader.getNamespace().get("permTotalDaysField");
                TextField permPresentDaysField = (TextField) payrollLoader.getNamespace().get("permPresentDaysField");
                TextField permAbsentDaysField = (TextField) payrollLoader.getNamespace().get("permAbsentDaysField");

                Label permPayableBasicLabel = (Label) payrollLoader.getNamespace().get("permPayableBasicLabel");
                Label permGrossEarningsLabel = (Label) payrollLoader.getNamespace().get("permGrossEarningsLabel");
                Label permNetSalaryLabel = (Label) payrollLoader.getNamespace().get("permNetSalaryLabel");
                Label permEmployerPfLabel = (Label) payrollLoader.getNamespace().get("permEmployerPfLabel");

                assertNotNull(permEmployeeCombo);
                assertNotNull(permPayPeriodField);
                assertNotNull(permPayDatePicker);
                assertNotNull(permLocationField);
                assertNotNull(permDeptField);
                assertNotNull(permDojField);
                assertNotNull(permPfUanField);
                assertNotNull(permPtConfigField);

                // Initial default values
                assertEquals("30", permTotalDaysField.getText());
                assertEquals("30", permPresentDaysField.getText());
                assertEquals("0", permAbsentDaysField.getText());

                // Test Live Permanent Pro-rating using a test Permanent employee
                PermanentEmployee testPermEmp = new PermanentEmployee(
                        "Ramesh", "Vemula", "EMP-TEST-P", LocalDate.of(1988, 7, 10),
                        "ramesh@example.com", "9876500001", "Married", "Hyderabad",
                        "Engineering", "Senior Developer", "Telangana",
                        LocalDate.of(2021, 3, 1), true, "PF-9999", "UAN-9999",
                        true, false, null
                );

                permEmployeeCombo.getItems().add(testPermEmp);
                permEmployeeCombo.setValue(testPermEmp);

                // Verify Auto-retrieved fields
                assertEquals("Telangana", permLocationField.getText());
                assertEquals("Engineering", permDeptField.getText());
                assertEquals("2021-03-01", permDojField.getText());
                assertTrue(permPfUanField.getText().contains("PF-9999"));
                assertTrue(permPtConfigField.getText().contains("Telangana"));

                // Test Live Absent Days and Pro-rating calculation
                permTotalDaysField.setText("30");
                permPresentDaysField.setText("24"); // 6 LOP days
                assertEquals("6", permAbsentDaysField.getText());

                // Senior Developer Permanent Policy: Basic=40,000, HRA=4,000, DA=2,500, Conveyance=1,200 (Total Fixed = 47,700)
                // Pro-rated factor = 24/30 = 0.80
                // Payable Basic = 40,000 * 0.8 = 32,000.00
                // Payable Gross = 47,700 * 0.8 = 38,160.00
                assertEquals("₹ 32,000.00", permPayableBasicLabel.getText());
                assertEquals("₹ 38,160.00", permGrossEarningsLabel.getText());

                // 5. Verify Bench Tab Controls
                ComboBox<com.payroll.model.Employee> benchEmployeeCombo = (ComboBox<com.payroll.model.Employee>) payrollLoader.getNamespace().get("benchEmployeeCombo");
                ComboBox<String> benchProjectCombo = (ComboBox<String>) payrollLoader.getNamespace().get("benchProjectCombo");
                TextField benchProjectDaysField = (TextField) payrollLoader.getNamespace().get("benchProjectDaysField");
                TextField benchDailyRateField = (TextField) payrollLoader.getNamespace().get("benchDailyRateField");

                Label benchPayableBasicLabel = (Label) payrollLoader.getNamespace().get("benchPayableBasicLabel");
                Label benchHraLabel = (Label) payrollLoader.getNamespace().get("benchHraLabel");
                Label benchDaLabel = (Label) payrollLoader.getNamespace().get("benchDaLabel");
                Label benchConveyanceLabel = (Label) payrollLoader.getNamespace().get("benchConveyanceLabel");
                Label benchGrossEarningsLabel = (Label) payrollLoader.getNamespace().get("benchGrossEarningsLabel");

                assertNotNull(benchEmployeeCombo);
                assertNotNull(benchProjectCombo);
                assertNotNull(benchProjectDaysField);
                assertNotNull(benchDailyRateField);

                BenchEmployee testBenchEmp = new BenchEmployee(
                        "Kavitha", "Nair", "EMP-TEST-B", LocalDate.of(1996, 11, 4),
                        "kavitha@example.com", "9876500002", "Single", "Visakhapatnam",
                        "Operations", "Senior Developer", "Andhra Pradesh",
                        LocalDate.of(2022, 6, 15), true, "PF-8888", "UAN-8888",
                        true, false, null
                );

                benchEmployeeCombo.getItems().add(testBenchEmp);
                benchEmployeeCombo.setValue(testBenchEmp);

                // Configured daily rate for Senior Developer is ₹ 1,500.00
                assertTrue(benchDailyRateField.getText().contains("1,500.00"));

                // Set eligible project days to 10
                benchProjectDaysField.setText("10");

                // Bench Gross = 10 * 1,500.00 = 15,000.00
                assertEquals("₹ 15,000.00", benchPayableBasicLabel.getText());
                assertEquals("₹ 15,000.00", benchGrossEarningsLabel.getText());

                // Strict Rule: Bench employees receive NO HRA, NO DA, NO Conveyance
                assertTrue(benchHraLabel.getText().contains("₹ 0.00"));
                assertTrue(benchDaLabel.getText().contains("₹ 0.00"));
                assertTrue(benchConveyanceLabel.getText().contains("₹ 0.00"));

                // 6. Verify Batch Tab Controls
                ComboBox<String> batchCategoryCombo = (ComboBox<String>) payrollLoader.getNamespace().get("batchCategoryCombo");
                TextField batchPeriodField = (TextField) payrollLoader.getNamespace().get("batchPeriodField");
                TextField batchDaysField = (TextField) payrollLoader.getNamespace().get("batchDaysField");
                Button btnRunBatch = (Button) payrollLoader.getNamespace().get("btnRunBatch");
                TableView<?> batchResultTable = (TableView<?>) payrollLoader.getNamespace().get("batchResultTable");

                assertNotNull(batchCategoryCombo);
                assertEquals(2, batchCategoryCombo.getItems().size());
                assertTrue(batchCategoryCombo.getItems().contains("Permanent"));
                assertTrue(batchCategoryCombo.getItems().contains("Bench"));

                assertNotNull(batchPeriodField);
                assertNotNull(batchDaysField);
                assertNotNull(btnRunBatch);
                assertNotNull(batchResultTable);

            } catch (Exception e) {
                fail("Stage 5 UI verification failed: " + e.getMessage());
            }
        });
    }
}

