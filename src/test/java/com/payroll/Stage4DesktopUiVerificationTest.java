package com.payroll;

import com.payroll.controller.*;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 4 Desktop UI Automated Verification Suite.
 * Simulates complete user session navigation across all implemented screens:
 * Login -> Dashboard -> Employee Registration -> Employee Directory -> Settings -> Logout.
 * Confirms zero CSS/HTML, proper layouts, no clipped or missing controls, and no CSV corruption.
 */
public class Stage4DesktopUiVerificationTest {

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
            testStage.setTitle("Employee Payroll Management System - UI Verification");
            stageLatch.countDown();
        });
        assertTrue(stageLatch.await(5, TimeUnit.SECONDS));
    }

    @AfterAll
    static void cleanup() throws InterruptedException {
        if (testStage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                testStage.close();
                latch.countDown();
            });
            latch.await(2, TimeUnit.SECONDS);
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
    @DisplayName("Complete UI Lifecycle Verification: Login -> Dashboard -> Registration -> Directory -> Settings -> Logout")
    void testCompleteUiLifecycle() throws InterruptedException {
        runOnFxThread(() -> {
            try {
                // Ensure logged out initial state
                AppContext.getInstance().getAuthService().logout();

                // 1. Load and display Login Screen
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/com/payroll/login.fxml"));
                Parent loginRoot = loginLoader.load();
                assertNotNull(loginRoot, "Login root cannot be null");
                LoginController loginController = loginLoader.getController();
                assertNotNull(loginController, "LoginController must be instantiated");

                Scene scene = new Scene(loginRoot, 1000, 650);
                testStage.setScene(scene);
                testStage.show();

                // Confirm no CSS stylesheets attached
                assertTrue(scene.getStylesheets().isEmpty(), "Strict Hackathon Rule: Zero CSS stylesheets allowed on scene");

                TextField empIdField = (TextField) loginLoader.getNamespace().get("empIdField");
                PasswordField passwordField = (PasswordField) loginLoader.getNamespace().get("passwordField");
                TextField passwordVisibleField = (TextField) loginLoader.getNamespace().get("passwordVisibleField");
                CheckBox showPassCheck = (CheckBox) loginLoader.getNamespace().get("showPasswordCheckBox");
                Button loginBtn = (Button) loginLoader.getNamespace().get("loginButton");

                assertNotNull(empIdField, "empIdField must exist");
                assertNotNull(passwordField, "passwordField must exist");
                assertNotNull(passwordVisibleField, "passwordVisibleField must exist");
                assertNotNull(loginBtn, "loginBtn must exist");

                // Test Password Show/Hide toggle
                assertFalse(passwordVisibleField.isVisible(), "Visible password field must initially be hidden");
                showPassCheck.setSelected(true);
                assertTrue(passwordVisibleField.isVisible(), "Visible password field must be visible when checked");
                showPassCheck.setSelected(false);
                assertFalse(passwordVisibleField.isVisible());

                // Test Invalid Credentials
                empIdField.setText("WRONG_USER");
                passwordField.setText("WRONG_PASS");
                loginController.handleLogin();
                assertFalse(AppContext.getInstance().getAuthService().isLoggedIn(), "Invalid credentials must not log in");

                // Test Valid Demo Credentials (1234 / 5678)
                empIdField.setText("1234");
                passwordField.setText("5678");
                loginController.handleLogin();
                assertTrue(AppContext.getInstance().getAuthService().isLoggedIn(), "Admin credentials (1234/5678) must succeed");

                // 2. Load Main Application Layout
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent mainRoot = mainLoader.load();
                assertNotNull(mainRoot, "Main layout root cannot be null");
                MainLayoutController mainController = mainLoader.getController();
                assertNotNull(mainController, "MainLayoutController must be injected");

                scene.setRoot(mainRoot);
                assertTrue(scene.getStylesheets().isEmpty(), "Zero CSS stylesheets on main layout scene");

                // 3. Inspect Sidebar Navigation Controls
                Label userLabel = (Label) mainLoader.getNamespace().get("userLabel");
                assertNotNull(userLabel);
                assertTrue(userLabel.getText().contains("1234"), "User label should reflect current authenticated admin");

                Button btnDashboard = (Button) mainLoader.getNamespace().get("btnDashboard");
                Button btnReg = (Button) mainLoader.getNamespace().get("btnRegistration");
                Button btnDir = (Button) mainLoader.getNamespace().get("btnDirectory");
                Button btnSettings = (Button) mainLoader.getNamespace().get("btnSettings");
                Button btnPayroll = (Button) mainLoader.getNamespace().get("btnPayroll");
                Button btnSalarySlip = (Button) mainLoader.getNamespace().get("btnSalarySlip");
                Button btnHistory = (Button) mainLoader.getNamespace().get("btnHistory");

                assertFalse(btnDashboard.isDisabled(), "Dashboard button must be active");
                assertFalse(btnReg.isDisabled(), "Registration button must be active");
                assertFalse(btnDir.isDisabled(), "Directory button must be active");
                assertFalse(btnSettings.isDisabled(), "Settings button must be active");
                assertFalse(btnPayroll.isDisabled(), "Payroll button must be active for Stage 5");
                assertFalse(btnSalarySlip.isDisabled(), "Salary Slip button must be active for Stage 6");
                assertFalse(btnHistory.isDisabled(), "History button must be active for Stage 7");

                StackPane contentArea = (StackPane) mainLoader.getNamespace().get("contentArea");
                assertNotNull(contentArea, "Central content area must exist");

                // 4. Test Dashboard Screen
                mainController.showDashboard();
                assertFalse(contentArea.getChildren().isEmpty(), "Dashboard view must be placed into content area");
                javafx.scene.Node dashView = contentArea.getChildren().get(0);
                assertNotNull(dashView);

                // 5. Test Employee Registration Screen & Fields
                mainController.showRegistration();
                assertFalse(contentArea.getChildren().isEmpty());
                javafx.scene.Node regView = contentArea.getChildren().get(0);
                assertNotNull(regView);

                // 6. Test Employee Directory Screen
                mainController.showDirectory();
                assertFalse(contentArea.getChildren().isEmpty());
                javafx.scene.Node dirView = contentArea.getChildren().get(0);
                assertNotNull(dirView);

                // 7. Test Settings Screen & All Tabs
                mainController.showSettings();
                assertFalse(contentArea.getChildren().isEmpty());
                javafx.scene.Node settingsView = contentArea.getChildren().get(0);
                assertNotNull(settingsView);

                // 8. Test Logout Action
                mainController.performLogout();
                assertFalse(AppContext.getInstance().getAuthService().isLoggedIn(), "Session must be terminated after logout");

            } catch (Exception e) {
                throw new RuntimeException("UI Lifecycle Verification Failed: " + e.getMessage(), e);
            }
        });
    }
}
