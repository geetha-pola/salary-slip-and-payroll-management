package com.payroll.controller;

import com.payroll.MainApp;
import com.payroll.service.AppContext;
import com.payroll.service.AuthService;
import com.payroll.util.UiTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller for the Main Persistent Sidebar Layout.
 * Manages view switching into the central content area and handles user logout.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class MainLayoutController {

    @FXML
    private BorderPane mainRoot;

    @FXML
    private VBox sidebarBox;

    @FXML
    private Label brandTitleLabel;

    @FXML
    private Label brandSubtitleLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label userLabel;

    @FXML
    private Button btnLogout;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnRegistration;

    @FXML
    private Button btnDirectory;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnPayroll;

    @FXML
    private Button btnSalarySlip;

    @FXML
    private Button btnHistory;

    private final AuthService authService;
    private Button activeNavButton;

    public MainLayoutController() {
        this.authService = AppContext.getInstance().getAuthService();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();

        if (!authService.isLoggedIn()) {
            handleLogout();
            return;
        }

        userLabel.setText("Admin: " + authService.getCurrentUser());

        // Stage 5 & Stage 6 are fully implemented and enabled
        if (btnPayroll != null) {
            btnPayroll.setDisable(false);
            btnPayroll.setText("💰  Monthly Payroll Processing");
            UiTheme.applySidebarNavButton(btnPayroll, false);
        }
        if (btnSalarySlip != null) {
            btnSalarySlip.setDisable(false);
            btnSalarySlip.setText("📄  Salary Slip Generation");
            UiTheme.applySidebarNavButton(btnSalarySlip, false);
        }
        if (btnHistory != null) {
            btnHistory.setDisable(false);
            btnHistory.setText("📜  Payroll History");
            UiTheme.applySidebarNavButton(btnHistory, false);
        }

        showDashboard();
    }

    private void applyPureJavaFxStyles() {
        if (sidebarBox != null) {
            UiTheme.applySidebar(sidebarBox);
        }

        if (contentArea != null) {
            UiTheme.applyContentArea(contentArea);
        }

        if (brandTitleLabel != null) {
            brandTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 16));
            brandTitleLabel.setTextFill(Color.WHITE);
        }

        if (brandSubtitleLabel != null) {
            brandSubtitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            brandSubtitleLabel.setTextFill(Color.rgb(148, 163, 184));
        }

        if (userLabel != null) {
            UiTheme.applyBadge(userLabel, Color.rgb(51, 65, 85), Color.rgb(248, 250, 252));
        }

        if (btnLogout != null) {
            UiTheme.applyDangerButton(btnLogout);
        }

        // Apply initial nav button styles
        Button[] navButtons = {btnDashboard, btnRegistration, btnDirectory, btnSettings, btnPayroll, btnSalarySlip, btnHistory};
        for (Button btn : navButtons) {
            if (btn != null) {
                UiTheme.applySidebarNavButton(btn, false);
            }
        }
    }

    private void setActiveButton(Button button) {
        if (activeNavButton != null) {
            UiTheme.applySidebarNavButton(activeNavButton, false);
        }
        activeNavButton = button;
        if (activeNavButton != null) {
            UiTheme.applySidebarNavButton(activeNavButton, true);
        }
    }

    private void loadView(String fxmlPath, Button navButton) {
        setActiveButton(navButton);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Error loading view " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to load view");
            alert.setContentText("Could not load " + fxmlPath + ":\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void showDashboard() {
        loadView("/com/payroll/dashboard.fxml", btnDashboard);
    }

    @FXML
    public void showRegistration() {
        loadView("/com/payroll/employee-form.fxml", btnRegistration);
    }

    @FXML
    public void showDirectory() {
        loadView("/com/payroll/employee-directory.fxml", btnDirectory);
    }

    @FXML
    public void showSettings() {
        loadView("/com/payroll/settings.fxml", btnSettings);
    }

    @FXML
    public void showPayroll() {
        loadView("/com/payroll/payroll.fxml", btnPayroll);
    }

    @FXML
    public void showSalarySlip() {
        loadView("/com/payroll/salary-slip.fxml", btnSalarySlip);
    }

    @FXML
    public void showHistory() {
        loadView("/com/payroll/history.fxml", btnHistory);
    }

    @FXML
    public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out Confirmation");
        alert.setHeaderText("Are you sure you want to sign out?");
        alert.setContentText("Your current administrative session will be terminated.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performLogout();
        }
    }

    public void performLogout() {
        authService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/login.fxml"));
            Parent root = loader.load();

            Stage stage = MainApp.getPrimaryStage();
            if (stage != null) {
                Scene currentScene = stage.getScene();
                if (currentScene == null) {
                    currentScene = new Scene(root, 900, 600);
                    stage.setScene(currentScene);
                } else {
                    currentScene.setRoot(root);
                }
                stage.setTitle("Employee Payroll Management System - Login");
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("Failed to return to Login view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
