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
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Entry Login Screen.
 * Enforces PS-10 demo credentials (ID: 1234, Password: 5678) via AuthService.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class LoginController {

    @FXML
    private StackPane rootPane;

    @FXML
    private VBox cardBox;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label empIdLabel;

    @FXML
    private TextField empIdField;

    @FXML
    private Label passwordLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private VBox hintBox;

    @FXML
    private Label hintTitleLabel;

    @FXML
    private Label hintValueLabel;

    private final AuthService authService;

    public LoginController() {
        this.authService = AppContext.getInstance().getAuthService();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();

        // Sync password field and visible text field
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheckBox.selectedProperty().addListener((obs, wasShown, isNowShown) -> {
            if (isNowShown) {
                passwordVisibleField.setManaged(true);
                passwordVisibleField.setVisible(true);
                passwordField.setManaged(false);
                passwordField.setVisible(false);
            } else {
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                passwordVisibleField.setManaged(false);
                passwordVisibleField.setVisible(false);
            }
        });
    }

    private void applyPureJavaFxStyles() {
        if (rootPane != null) {
            rootPane.setBackground(new Background(new BackgroundFill(UiTheme.COLOR_PRIMARY_NAVY, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        if (cardBox != null) {
            UiTheme.applyCard(cardBox);
        }

        if (titleLabel != null) {
            titleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 17));
            titleLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }

        if (subtitleLabel != null) {
            subtitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            subtitleLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }

        if (empIdLabel != null) UiTheme.applyFormLabel(empIdLabel);
        if (empIdField != null) UiTheme.applyInputField(empIdField);

        if (passwordLabel != null) UiTheme.applyFormLabel(passwordLabel);
        if (passwordField != null) UiTheme.applyInputField(passwordField);
        if (passwordVisibleField != null) UiTheme.applyInputField(passwordVisibleField);

        if (showPasswordCheckBox != null) {
            showPasswordCheckBox.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            showPasswordCheckBox.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }

        if (messageLabel != null) {
            UiTheme.applyErrorLabel(messageLabel);
        }

        if (loginButton != null) {
            UiTheme.applyPrimaryButton(loginButton);
        }

        if (hintBox != null) {
            hintBox.setBackground(new Background(new BackgroundFill(UiTheme.COLOR_BG_LIGHT, new CornerRadii(6), Insets.EMPTY)));
            hintBox.setBorder(new Border(new BorderStroke(UiTheme.COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        }

        if (hintTitleLabel != null) {
            hintTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
            hintTitleLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }

        if (hintValueLabel != null) {
            hintValueLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            hintValueLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
    }

    @FXML
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    @FXML
    public void handleLogin() {
        String empId = empIdField.getText();
        String password = passwordField.getText();

        AuthService.AuthResult result = authService.authenticate(empId, password);

        if (result.isSuccess()) {
            messageLabel.setVisible(false);
            navigateToMainLayout();
        } else {
            messageLabel.setText(result.getMessage());
            messageLabel.setVisible(true);
        }
    }

    private void navigateToMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
            Parent root = loader.load();

            Stage stage = MainApp.getPrimaryStage();
            Scene currentScene = stage != null ? stage.getScene() : null;

            if (currentScene == null) {
                currentScene = new Scene(root, 1150, 720);
                if (stage != null) {
                    stage.setScene(currentScene);
                }
            } else {
                currentScene.setRoot(root);
            }

            if (stage != null) {
                stage.setTitle("Employee Payroll Management System - Corporate Portal");
                stage.setMinWidth(1000);
                stage.setMinHeight(650);
                stage.show();
            }
        } catch (IOException e) {
            messageLabel.setText("Failed to load application layout: " + e.getMessage());
            messageLabel.setVisible(true);
            e.printStackTrace();
        }
    }
}
