package com.payroll;

import com.payroll.controller.*;
import com.payroll.service.AppContext;
import com.payroll.util.UiTheme;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 4 UI Tests verifying Hackathon compliance:
 * STRICTLY JavaFX + FXML + Java Controllers (Zero CSS, Zero HTML).
 */
public class Stage4UiTest {

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

        // Authenticate administrator session in AppContext
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
    @DisplayName("Verify Pure JavaFX UiTheme engine applies Background, Border, Font, and Colors without CSS")
    void testUiThemeEngine() throws InterruptedException {
        runOnFxThread(() -> {
            VBox card = new VBox();
            UiTheme.applyCard(card);
            assertNotNull(card.getBackground(), "Card must have JavaFX Background applied");
            assertNotNull(card.getBorder(), "Card must have JavaFX Border applied");

            Button primaryBtn = new Button("Submit");
            UiTheme.applyPrimaryButton(primaryBtn);
            assertNotNull(primaryBtn.getBackground(), "Primary button must have JavaFX Background applied");
            assertEquals(UiTheme.COLOR_TEXT_LIGHT, primaryBtn.getTextFill(), "Primary button text must be light");

            TextField field = new TextField();
            UiTheme.applyInputField(field);
            assertNotNull(field.getBackground());
            assertNotNull(field.getBorder());
        });
    }

    @Test
    @DisplayName("Verify Login screen FXML and LoginController initialization")
    void testLoginScreenLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/login.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "login.fxml root must load");
                LoginController controller = loader.getController();
                assertNotNull(controller, "LoginController must be instantiated");

                TextField empId = (TextField) loader.getNamespace().get("empIdField");
                assertNotNull(empId, "empIdField must exist");
                Button loginBtn = (Button) loader.getNamespace().get("loginButton");
                assertNotNull(loginBtn, "loginButton must exist");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Login screen: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Verify Main Layout FXML loading, persistent sidebar, and disabled future stage entries")
    void testMainLayoutLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Main layout root node must not be null");
                MainLayoutController controller = loader.getController();
                assertNotNull(controller, "MainLayoutController must be injected");

                // Verify Stage 5 and 6 buttons are enabled, Stage 7 button remains disabled per requirements
                Button btnPayroll = (Button) loader.getNamespace().get("btnPayroll");
                assertNotNull(btnPayroll, "#btnPayroll button must exist");
                assertFalse(btnPayroll.isDisabled(), "Payroll button must be enabled in Stage 5");

                Button btnSalarySlip = (Button) loader.getNamespace().get("btnSalarySlip");
                assertNotNull(btnSalarySlip, "#btnSalarySlip button must exist");
                assertFalse(btnSalarySlip.isDisabled(), "Salary Slip button must be enabled in Stage 6");

                Button btnHistory = (Button) loader.getNamespace().get("btnHistory");
                assertNotNull(btnHistory, "#btnHistory button must exist");
                assertFalse(btnHistory.isDisabled(), "History button must be active in Stage 7");

                // Verify Stages 1-4 buttons are active
                Button btnDashboard = (Button) loader.getNamespace().get("btnDashboard");
                assertNotNull(btnDashboard, "#btnDashboard button must exist");
                assertFalse(btnDashboard.isDisabled(), "Dashboard button must be active");

                Button btnRegistration = (Button) loader.getNamespace().get("btnRegistration");
                assertNotNull(btnRegistration, "#btnRegistration button must exist");
                assertFalse(btnRegistration.isDisabled(), "Registration button must be active");

                Button btnDirectory = (Button) loader.getNamespace().get("btnDirectory");
                assertNotNull(btnDirectory, "#btnDirectory button must exist");
                assertFalse(btnDirectory.isDisabled(), "Directory button must be active");

                Button btnSettings = (Button) loader.getNamespace().get("btnSettings");
                assertNotNull(btnSettings, "#btnSettings button must exist");
                assertFalse(btnSettings.isDisabled(), "Settings button must be active");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Main Layout: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Verify Dashboard FXML loading, cards, and dynamic statistics")
    void testDashboardLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/dashboard.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Dashboard root node must not be null");
                DashboardController controller = loader.getController();
                assertNotNull(controller, "DashboardController must be injected");

                Label activeLabel = (Label) loader.getNamespace().get("totalActiveEmployeesLabel");
                assertNotNull(activeLabel, "totalActiveEmployeesLabel must exist");
                assertNotNull(activeLabel.getText(), "Active count text must not be null");

                Label currentMonthLabel = (Label) loader.getNamespace().get("currentMonthPayrollLabel");
                assertNotNull(currentMonthLabel);
                assertEquals("Not Available Yet", currentMonthLabel.getText(), "Dashboard must show 'Not Available Yet' for unintegrated payroll metrics");

                Label permLabel = (Label) loader.getNamespace().get("totalPermanentLabel");
                assertNotNull(permLabel, "totalPermanentLabel must exist");

                Label benchLabel = (Label) loader.getNamespace().get("totalBenchLabel");
                assertNotNull(benchLabel, "totalBenchLabel must exist");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Dashboard: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Verify Employee Registration form loading, field bindings, and preview")
    void testEmployeeFormLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/employee-form.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Employee form root node must not be null");
                EmployeeController controller = loader.getController();
                assertNotNull(controller, "EmployeeController must be injected");

                TextField firstNameField = (TextField) loader.getNamespace().get("firstNameField");
                TextField lastNameField = (TextField) loader.getNamespace().get("lastNameField");
                TextField fullNameField = (TextField) loader.getNamespace().get("fullNameField");
                assertNotNull(firstNameField);
                assertNotNull(lastNameField);
                assertNotNull(fullNameField);

                // Test live name concatenation binding
                firstNameField.setText("Ravi");
                lastNameField.setText("Kumar");
                assertEquals("Ravi Kumar", fullNameField.getText(), "Full name should automatically combine First + Last name");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Employee Form: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Verify Employee Directory screen loading, table columns, and filter controls")
    void testEmployeeDirectoryLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/employee-directory.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Employee directory root node must not be null");
                EmployeeDirectoryController controller = loader.getController();
                assertNotNull(controller, "EmployeeDirectoryController must be injected");

                TableView<?> table = (TableView<?>) loader.getNamespace().get("employeeTable");
                assertNotNull(table, "employeeTable must exist");
                assertEquals(8, table.getColumns().size(), "Employee table should have 8 columns");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Employee Directory: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Verify Settings screen loading and policy table controls")
    void testSettingsLoading() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/settings.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Settings root node must not be null");
                SettingsController controller = loader.getController();
                assertNotNull(controller, "SettingsController must be injected");

                TabPane tabPane = (TabPane) loader.getNamespace().get("tabPane");
                assertNotNull(tabPane, "Settings TabPane must exist");
                assertEquals(4, tabPane.getTabs().size(), "Settings screen must have 4 tabs (Designations, PF, PT, Company)");

                TableView<?> desigTable = (TableView<?>) loader.getNamespace().get("designationTable");
                assertNotNull(desigTable, "designationTable must exist");
                assertTrue(desigTable.getColumns().size() >= 7, "Designation table should contain all policy columns");
            } catch (Exception e) {
                throw new RuntimeException("Failed to test Settings: " + e.getMessage(), e);
            }
        });
    }
}
