package com.payroll.controller;

import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.EmployeeManager;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PolicyRepository;
import com.payroll.util.CurrencyFormatter;
import com.payroll.util.UiTheme;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Employee Registration module.
 * Enforces all 10 registration business rules from Section 5.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class EmployeeController {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox viewContainer;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private VBox formCard;

    @FXML
    private Label sec1Label;

    @FXML
    private Label sec2Label;

    @FXML
    private Label sec3Label;

    @FXML
    private Label sec4Label;

    // Field labels
    @FXML private Label lblFirstName;
    @FXML private Label lblLastName;
    @FXML private Label lblFullName;
    @FXML private Label lblEmpId;
    @FXML private Label lblDob;
    @FXML private Label lblEmail;
    @FXML private Label lblMobile;
    @FXML private Label lblMarital;
    @FXML private Label lblAddress;
    @FXML private Label lblDept;
    @FXML private Label lblCat;
    @FXML private Label lblDesig;
    @FXML private Label lblLoc;
    @FXML private Label lblDoj;
    @FXML private Label lblStatus;
    @FXML private Label lblPfAcc;
    @FXML private Label lblUan;
    @FXML private Label lblPfNote;
    @FXML private Label lblMedical;
    @FXML private Label lblMedicalDesc;

    // Personal Information
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField fullNameField;
    @FXML private TextField employeeIdField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField emailField;
    @FXML private TextField mobileField;
    @FXML private ComboBox<String> maritalStatusCombo;
    @FXML private TextArea addressArea;

    // Employment Information
    @FXML private ComboBox<String> departmentCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> designationCombo;
    @FXML private ComboBox<String> workLocationCombo;
    @FXML private DatePicker dojPicker;
    @FXML private CheckBox activeCheckBox;

    // Payroll Identification
    @FXML private TextField pfAccountField;
    @FXML private TextField uanField;
    @FXML private CheckBox pfApplicableCheckBox;

    // Sensitive Additional Information
    @FXML private RadioButton medicalYesRadio;
    @FXML private RadioButton medicalNoRadio;
    @FXML private ToggleGroup medicalToggleGroup;
    @FXML private TextArea medicalDescArea;
    @FXML private VBox medicalDescBox;

    // Buttons
    @FXML private Button btnClear;
    @FXML private Button btnRegister;

    // Policy Salary Structure Preview
    @FXML private VBox previewCard;
    @FXML private Label policyPreviewTitle;
    @FXML private Label previewNoticeLabel;
    @FXML private Label previewBasicLabel;
    @FXML private Label previewHraLabel;
    @FXML private Label previewDaLabel;
    @FXML private Label previewConveyanceLabel;
    @FXML private Label previewPfHeaderLabel;
    @FXML private Label previewPfRatesLabel;
    @FXML private VBox previewInfoBox;
    @FXML private Label previewNoteTitle;
    @FXML private Label previewNoteText;

    private final EmployeeManager employeeManager;
    private final EmployeeRepository employeeRepository;
    private final PolicyRepository policyRepository;

    public EmployeeController() {
        this.employeeManager = AppContext.getInstance().getEmployeeManager();
        this.employeeRepository = AppContext.getInstance().getEmployeeRepository();
        this.policyRepository = AppContext.getInstance().getPolicyRepository();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupFormControls();
        setupLiveListeners();
        updateDesignationOptions();
    }

    private void applyPureJavaFxStyles() {
        if (scrollPane != null) {
            scrollPane.setBackground(new Background(new BackgroundFill(UiTheme.COLOR_BG_LIGHT, CornerRadii.EMPTY, Insets.EMPTY)));
            scrollPane.setBorder(Border.EMPTY);
        }
        if (viewContainer != null) {
            viewContainer.setBackground(new Background(new BackgroundFill(UiTheme.COLOR_BG_LIGHT, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        if (pageTitleLabel != null) UiTheme.applyPageTitle(pageTitleLabel);
        if (pageSubtitleLabel != null) UiTheme.applyPageSubtitle(pageSubtitleLabel);

        if (formCard != null) UiTheme.applyCard(formCard);
        if (previewCard != null) UiTheme.applyCard(previewCard);

        if (sec1Label != null) UiTheme.applySectionHeader(sec1Label);
        if (sec2Label != null) UiTheme.applySectionHeader(sec2Label);
        if (sec3Label != null) UiTheme.applySectionHeader(sec3Label);
        if (sec4Label != null) UiTheme.applySectionHeader(sec4Label);

        // Labels
        Label[] formLabels = {
                lblFirstName, lblLastName, lblFullName, lblEmpId, lblDob, lblEmail, lblMobile,
                lblMarital, lblAddress, lblDept, lblCat, lblDesig, lblLoc, lblDoj, lblStatus,
                lblPfAcc, lblUan, lblMedical, lblMedicalDesc
        };
        for (Label lbl : formLabels) {
            if (lbl != null) UiTheme.applyFormLabel(lbl);
        }

        if (lblPfNote != null) {
            lblPfNote.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            lblPfNote.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }

        // Input Fields
        TextInputControl[] fields = {firstNameField, lastNameField, employeeIdField, emailField, mobileField, addressArea, pfAccountField, uanField, medicalDescArea};
        for (TextInputControl f : fields) {
            if (f != null) UiTheme.applyInputField(f);
        }

        if (fullNameField != null) UiTheme.applyReadOnlyField(fullNameField);

        // Combos & Pickers
        ComboBox<?>[] combos = {maritalStatusCombo, departmentCombo, categoryCombo, designationCombo, workLocationCombo};
        for (ComboBox<?> c : combos) {
            if (c != null) UiTheme.applyComboBox(c);
        }

        if (dobPicker != null) UiTheme.applyDatePicker(dobPicker);
        if (dojPicker != null) UiTheme.applyDatePicker(dojPicker);

        // Action Buttons
        if (btnClear != null) UiTheme.applySecondaryButton(btnClear);
        if (btnRegister != null) UiTheme.applyPrimaryButton(btnRegister);

        // Preview Box Styling
        if (policyPreviewTitle != null) {
            policyPreviewTitle.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 15));
            policyPreviewTitle.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }
        if (previewNoticeLabel != null) {
            previewNoticeLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            previewNoticeLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
        if (previewBasicLabel != null) {
            previewBasicLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
            previewBasicLabel.setTextFill(UiTheme.COLOR_ACTION_BLUE);
        }
        if (previewHraLabel != null) previewHraLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
        if (previewDaLabel != null) previewDaLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
        if (previewConveyanceLabel != null) previewConveyanceLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
        if (previewPfHeaderLabel != null) {
            previewPfHeaderLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
            previewPfHeaderLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }
        if (previewPfRatesLabel != null) previewPfRatesLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));

        if (previewInfoBox != null) {
            previewInfoBox.setBackground(new Background(new BackgroundFill(Color.rgb(239, 246, 255), new CornerRadii(6), Insets.EMPTY)));
            previewInfoBox.setBorder(new Border(new BorderStroke(Color.rgb(191, 219, 254), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        }
        if (previewNoteTitle != null) {
            previewNoteTitle.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
            previewNoteTitle.setTextFill(UiTheme.COLOR_ACTION_BLUE);
        }
        if (previewNoteText != null) {
            previewNoteText.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            previewNoteText.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
    }

    private void setupFormControls() {
        maritalStatusCombo.setItems(FXCollections.observableArrayList("Single", "Married", "Divorced", "Widowed"));
        maritalStatusCombo.getSelectionModel().selectFirst();

        departmentCombo.setItems(FXCollections.observableArrayList("Engineering", "Management", "Quality Assurance", "Finance", "Human Resources", "Operations"));
        departmentCombo.getSelectionModel().selectFirst();

        categoryCombo.setItems(FXCollections.observableArrayList("Permanent", "Bench"));
        categoryCombo.getSelectionModel().selectFirst();

        workLocationCombo.setItems(FXCollections.observableArrayList("Telangana", "Andhra Pradesh"));
        workLocationCombo.getSelectionModel().selectFirst();

        activeCheckBox.setSelected(true);
        dojPicker.setValue(LocalDate.now());

        medicalDescBox.setVisible(false);
        medicalDescBox.setManaged(false);
    }

    private void setupLiveListeners() {
        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> updateFullName());
        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> updateFullName());

        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateDesignationOptions();
            updatePolicyPreview();
        });

        designationCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePolicyPreview());

        medicalToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasCondition = medicalYesRadio.isSelected();
            medicalDescBox.setVisible(hasCondition);
            medicalDescBox.setManaged(hasCondition);
        });
    }

    private void updateFullName() {
        String first = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String last = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        if (first.isEmpty() && last.isEmpty()) {
            fullNameField.setText("");
        } else if (first.isEmpty()) {
            fullNameField.setText(last);
        } else if (last.isEmpty()) {
            fullNameField.setText(first);
        } else {
            fullNameField.setText(first + " " + last);
        }
    }

    private void updateDesignationOptions() {
        String category = categoryCombo.getValue();
        if (category == null) return;

        List<DesignationPolicy> policies = policyRepository.loadDesignationPolicies();
        List<String> activeDesigs = policies.stream()
                .filter(p -> p.isActive() && p.getEmployeeCategory().equalsIgnoreCase(category))
                .map(DesignationPolicy::getDesignationName)
                .distinct()
                .sorted()
                .toList();

        designationCombo.setItems(FXCollections.observableArrayList(activeDesigs));
        if (!activeDesigs.isEmpty()) {
            designationCombo.getSelectionModel().selectFirst();
        } else {
            designationCombo.setValue(null);
        }
        updatePolicyPreview();
    }

    private void updatePolicyPreview() {
        String desig = designationCombo.getValue();
        String category = categoryCombo.getValue();

        if (desig == null || category == null) {
            policyPreviewTitle.setText("No Active Policy Selected");
            previewBasicLabel.setText("Monthly Basic / Daily Rate: -");
            previewHraLabel.setText("Fixed HRA: -");
            previewDaLabel.setText("Fixed DA: -");
            previewConveyanceLabel.setText("Fixed Conveyance: -");
            previewPfRatesLabel.setText("PF: -");
            return;
        }

        Optional<DesignationPolicy> opt = policyRepository.findDesignationPolicy(desig, category);
        if (opt.isPresent()) {
            DesignationPolicy p = opt.get();
            policyPreviewTitle.setText(p.getDesignationName() + " (" + p.getEmployeeCategory() + ")");

            if ("Permanent".equalsIgnoreCase(p.getEmployeeCategory())) {
                previewBasicLabel.setText("Monthly Basic: " + CurrencyFormatter.formatRupees(p.getMonthlyBasicSalary()));
                previewHraLabel.setText("Fixed HRA: " + CurrencyFormatter.formatRupees(p.getHra()));
                previewDaLabel.setText("Fixed DA: " + CurrencyFormatter.formatRupees(p.getDa()));
                previewConveyanceLabel.setText("Fixed Conveyance: " + CurrencyFormatter.formatRupees(p.getConveyance()));
            } else {
                previewBasicLabel.setText("Daily Rate Basic: " + CurrencyFormatter.formatRupees(p.getDailyRate()) + " / day");
                previewHraLabel.setText("Fixed HRA: ₹ 0.00 (Bench: Not Applicable)");
                previewDaLabel.setText("Fixed DA: ₹ 0.00 (Bench: Not Applicable)");
                previewConveyanceLabel.setText("Fixed Conveyance: ₹ 0.00 (Bench: Not Applicable)");
            }

            PFPolicy pfPolicy = policyRepository.loadPfPolicy();
            if (p.isPfApplicable() && pfPolicy != null && pfPolicy.isDefaultApplicable() && pfPolicy.isConfigured()) {
                previewPfRatesLabel.setText(String.format("Active: Employee %.2f%%, Employer %.2f%%",
                        pfPolicy.getEmployeeContributionRate().doubleValue() <= 1.0 ? pfPolicy.getEmployeeContributionRate().doubleValue() * 100 : pfPolicy.getEmployeeContributionRate().doubleValue(),
                        pfPolicy.getEmployerContributionRate().doubleValue() <= 1.0 ? pfPolicy.getEmployerContributionRate().doubleValue() * 100 : pfPolicy.getEmployerContributionRate().doubleValue()));
            } else if (p.isPfApplicable() && (pfPolicy == null || !pfPolicy.isDefaultApplicable() || !pfPolicy.isConfigured())) {
                previewPfRatesLabel.setText("Designation eligible (Demodata), but Global PF is Inactive in Settings");
            } else {
                previewPfRatesLabel.setText("PF Not Applicable for this designation");
            }
        }
    }

    @FXML
    public void handleRegister() {
        String empId = employeeIdField.getText() == null ? "" : employeeIdField.getText().trim();
        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String mobile = mobileField.getText() == null ? "" : mobileField.getText().trim();
        String maritalStatus = maritalStatusCombo.getValue();
        String address = addressArea.getText() == null ? "" : addressArea.getText().trim();

        String department = departmentCombo.getValue();
        String category = categoryCombo.getValue();
        String designation = designationCombo.getValue();
        String workLocation = workLocationCombo.getValue();
        LocalDate doj = dojPicker.getValue();
        boolean active = activeCheckBox.isSelected();

        String pfAccount = pfAccountField.getText() == null ? "" : pfAccountField.getText().trim();
        String uan = uanField.getText() == null ? "" : uanField.getText().trim();
        boolean pfApplicable = pfApplicableCheckBox.isSelected();

        boolean hasMedical = medicalYesRadio.isSelected();
        String medicalDesc = hasMedical ? (medicalDescArea.getText() == null ? "" : medicalDescArea.getText().trim()) : "";

        // Build Employee Object based on category
        Employee employee;
        if ("Permanent".equalsIgnoreCase(category)) {
            employee = new PermanentEmployee(
                    empId, firstName, lastName, dob, email, mobile,
                    maritalStatus, address, department, designation,
                    workLocation, doj, active, pfAccount, uan,
                    pfApplicable, hasMedical, medicalDesc
            );
        } else if ("Bench".equalsIgnoreCase(category)) {
            employee = new BenchEmployee(
                    empId, firstName, lastName, dob, email, mobile,
                    maritalStatus, address, department, designation,
                    workLocation, doj, active, pfAccount, uan,
                    pfApplicable, hasMedical, medicalDesc
            );
        } else {
            showError("Invalid Employee Category", "Only Permanent and Bench categories are permitted.");
            return;
        }

        // Validate via EmployeeManager and save to repository
        try {
            employeeManager.registerEmployee(employee);
            employeeRepository.save(employee);
            showSuccess("Registration Successful",
                    "Employee " + employee.getFullName() + " (" + employee.getEmployeeId() + ") successfully registered as " + category + ".");
            handleClear();
        } catch (IllegalArgumentException e) {
            showError("Validation Failed", e.getMessage());
        } catch (IOException e) {
            showError("Storage Error", "Failed to save employee to CSV repository:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleClear() {
        firstNameField.clear();
        lastNameField.clear();
        fullNameField.clear();
        employeeIdField.clear();
        dobPicker.setValue(null);
        emailField.clear();
        mobileField.clear();
        maritalStatusCombo.getSelectionModel().selectFirst();
        addressArea.clear();

        departmentCombo.getSelectionModel().selectFirst();
        categoryCombo.getSelectionModel().selectFirst();
        updateDesignationOptions();
        workLocationCombo.getSelectionModel().selectFirst();
        dojPicker.setValue(LocalDate.now());
        activeCheckBox.setSelected(true);

        pfAccountField.clear();
        uanField.clear();
        pfApplicableCheckBox.setSelected(false);

        medicalNoRadio.setSelected(true);
        medicalDescArea.clear();
        medicalDescBox.setVisible(false);
        medicalDescBox.setManaged(false);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
