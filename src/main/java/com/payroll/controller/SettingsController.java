package com.payroll.controller;

import com.payroll.model.CompanySettings;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.PFPolicy;
import com.payroll.model.ProfessionalTaxPolicy;
import com.payroll.service.AppContext;
import com.payroll.storage.PolicyRepository;
import com.payroll.util.CurrencyFormatter;
import com.payroll.util.UiTheme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Designation and Salary Policy Settings screen.
 * Allows administrators to configure designation policies, global PF parameters,
 * professional tax slabs, and company details.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class SettingsController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox viewContainer;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private TabPane tabPane;

    // Tab 1 Designation Policies Controls
    @FXML private VBox policyTableCard;
    @FXML private Label tableCardHeader;
    @FXML private Button btnNewPolicy;
    @FXML private Button btnDeactivatePolicy;
    @FXML private VBox policyFormCard;
    @FXML private Label formCardHeader;
    @FXML private Label formHintLabel;

    @FXML private TableView<DesignationPolicy> designationTable;
    @FXML private TableColumn<DesignationPolicy, String> colDesigName;
    @FXML private TableColumn<DesignationPolicy, String> colCategory;
    @FXML private TableColumn<DesignationPolicy, String> colBasicRate;
    @FXML private TableColumn<DesignationPolicy, String> colHra;
    @FXML private TableColumn<DesignationPolicy, String> colDa;
    @FXML private TableColumn<DesignationPolicy, String> colConveyance;
    @FXML private TableColumn<DesignationPolicy, String> colPfApp;
    @FXML private TableColumn<DesignationPolicy, String> colPfRates;
    @FXML private TableColumn<DesignationPolicy, String> colStatus;

    // Form inputs for designation policy
    @FXML private Label lblDesigName;
    @FXML private TextField desigNameField;
    @FXML private Label lblCategory;
    @FXML private ComboBox<String> categoryField;
    @FXML private Label lblDailyRate;
    @FXML private TextField dailyRateField;
    @FXML private Label lblMonthlyBasic;
    @FXML private TextField monthlyBasicField;
    @FXML private Label lblHra;
    @FXML private TextField hraField;
    @FXML private Label lblDa;
    @FXML private TextField daField;
    @FXML private Label lblConveyance;
    @FXML private TextField conveyanceField;
    @FXML private Label lblOtherEarnings;
    @FXML private TextField otherEarningsField;
    @FXML private Label lblOtherDeductions;
    @FXML private TextField otherDeductionsField;

    @FXML private Label lblPfApp;
    @FXML private CheckBox pfApplicableCheck;
    @FXML private Label lblEmpPfRate;
    @FXML private TextField employeePfRateField;
    @FXML private Label lblEmployerPfRate;
    @FXML private TextField employerPfRateField;
    @FXML private Label lblPfWageBase;
    @FXML private ComboBox<String> pfWageBaseCombo;
    @FXML private Label lblPolicyStatus;
    @FXML private CheckBox activePolicyCheck;
    @FXML private Button btnSavePolicy;

    // Tab 2 Global PF Policy Controls
    @FXML private VBox pfSettingsCard;
    @FXML private Label pfCardHeader;
    @FXML private Label pfCardHint;
    @FXML private VBox pfStatusNoticeBox;
    @FXML private Label pfStatusTitleLabel;
    @FXML private Label pfStatusDescLabel;

    @FXML private Label lblGlobalPfEnable;
    @FXML private CheckBox globalPfApplicableCheck;
    @FXML private Label lblGlobalEeRate;
    @FXML private TextField globalEeRateField;
    @FXML private Label lblGlobalErRate;
    @FXML private TextField globalErRateField;
    @FXML private Label lblGlobalWageBase;
    @FXML private ComboBox<String> globalPfWageBaseCombo;
    @FXML private Label lblGlobalWageCeiling;
    @FXML private TextField globalWageCeilingField;
    @FXML private CheckBox globalEnforceCeilingCheck;
    @FXML private Button btnSaveGlobalPf;

    // Tab 3 Professional Tax Slabs
    @FXML private Label lblStateSelect;
    @FXML private ComboBox<String> ptStateSelector;
    @FXML private Label lblPtHint;
    @FXML private TableView<ProfessionalTaxPolicy.TaxSlab> ptTable;
    @FXML private TableColumn<ProfessionalTaxPolicy.TaxSlab, String> colPtMin;
    @FXML private TableColumn<ProfessionalTaxPolicy.TaxSlab, String> colPtMax;
    @FXML private TableColumn<ProfessionalTaxPolicy.TaxSlab, String> colPtTax;

    // Tab 4 Company Details
    @FXML private VBox companyCard;
    @FXML private Label companyCardHeader;
    @FXML private Label lblCompName;
    @FXML private TextField companyNameField;
    @FXML private Label lblCompAddr;
    @FXML private TextArea companyAddressArea;
    @FXML private Label lblPayslipTitle;
    @FXML private TextField payslipTitleField;
    @FXML private Label lblHrEmail;
    @FXML private TextField contactEmailField;
    @FXML private Label lblPhone;
    @FXML private TextField contactPhoneField;
    @FXML private Button btnSaveCompany;

    private final PolicyRepository policyRepo;
    private final ObservableList<DesignationPolicy> policyList = FXCollections.observableArrayList();

    public SettingsController() {
        this.policyRepo = AppContext.getInstance().getPolicyRepository();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupDesignationTable();
        setupCategoryToggleBehavior();
        setupGlobalPfTab();
        setupProfessionalTaxTab();
        setupCompanyDetailsTab();
        loadPolicies();
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

        if (policyTableCard != null) UiTheme.applyCard(policyTableCard);
        if (policyFormCard != null) UiTheme.applyCard(policyFormCard);
        if (pfSettingsCard != null) UiTheme.applyCard(pfSettingsCard);
        if (companyCard != null) UiTheme.applyCard(companyCard);

        if (tableCardHeader != null) UiTheme.applySectionHeader(tableCardHeader);
        if (formCardHeader != null) UiTheme.applySectionHeader(formCardHeader);
        if (pfCardHeader != null) UiTheme.applySectionHeader(pfCardHeader);
        if (companyCardHeader != null) UiTheme.applySectionHeader(companyCardHeader);

        if (formHintLabel != null) {
            formHintLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            formHintLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
        if (pfCardHint != null) {
            pfCardHint.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            pfCardHint.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
        if (lblPtHint != null) {
            lblPtHint.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            lblPtHint.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }

        // Form labels
        Label[] labels = {
                lblDesigName, lblCategory, lblDailyRate, lblMonthlyBasic, lblHra, lblDa,
                lblConveyance, lblOtherEarnings, lblOtherDeductions, lblPfApp, lblEmpPfRate,
                lblEmployerPfRate, lblPfWageBase, lblPolicyStatus, lblGlobalPfEnable,
                lblGlobalEeRate, lblGlobalErRate, lblGlobalWageBase, lblGlobalWageCeiling,
                lblStateSelect, lblCompName, lblCompAddr, lblPayslipTitle, lblHrEmail, lblPhone
        };
        for (Label l : labels) {
            if (l != null) UiTheme.applyFormLabel(l);
        }

        // Input fields
        TextInputControl[] inputs = {
                desigNameField, dailyRateField, monthlyBasicField, hraField, daField,
                conveyanceField, otherEarningsField, otherDeductionsField,
                employeePfRateField, employerPfRateField, globalEeRateField, globalErRateField,
                globalWageCeilingField, companyNameField, companyAddressArea, payslipTitleField,
                contactEmailField, contactPhoneField
        };
        for (TextInputControl in : inputs) {
            if (in != null) UiTheme.applyInputField(in);
        }

        if (categoryField != null) UiTheme.applyComboBox(categoryField);
        if (pfWageBaseCombo != null) UiTheme.applyComboBox(pfWageBaseCombo);
        if (globalPfWageBaseCombo != null) UiTheme.applyComboBox(globalPfWageBaseCombo);
        if (ptStateSelector != null) UiTheme.applyComboBox(ptStateSelector);

        // Buttons
        if (btnNewPolicy != null) UiTheme.applySecondaryButton(btnNewPolicy);
        if (btnDeactivatePolicy != null) UiTheme.applyDangerButton(btnDeactivatePolicy);
        if (btnSavePolicy != null) UiTheme.applyPrimaryButton(btnSavePolicy);
        if (btnSaveGlobalPf != null) UiTheme.applyPrimaryButton(btnSaveGlobalPf);
        if (btnSaveCompany != null) UiTheme.applyPrimaryButton(btnSaveCompany);
    }

    private void setupDesignationTable() {
        PFPolicy globalPf = policyRepo.loadPfPolicy();
        boolean globalActive = globalPf != null && globalPf.isDefaultApplicable() && globalPf.isConfigured();

        colDesigName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesignationName()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmployeeCategory()));
        colBasicRate.setCellValueFactory(cell -> {
            DesignationPolicy p = cell.getValue();
            if ("Permanent".equalsIgnoreCase(p.getEmployeeCategory())) {
                return new SimpleStringProperty(CurrencyFormatter.formatRupees(p.getMonthlyBasicSalary()) + "/mo");
            } else {
                return new SimpleStringProperty(CurrencyFormatter.formatRupees(p.getDailyRate()) + "/day");
            }
        });
        colHra.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.formatRupees(cell.getValue().getHra())));
        colDa.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.formatRupees(cell.getValue().getDa())));
        colConveyance.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.formatRupees(cell.getValue().getConveyance())));

        colPfApp.setCellValueFactory(cell -> {
            DesignationPolicy p = cell.getValue();
            if (p.isPfApplicable()) {
                return new SimpleStringProperty(globalActive ? "Active" : "Eligible (Global Inactive)");
            } else {
                return new SimpleStringProperty("Not Applicable");
            }
        });

        colPfRates.setCellValueFactory(cell -> {
            DesignationPolicy p = cell.getValue();
            if (p.isPfApplicable()) {
                if (globalActive) {
                    return new SimpleStringProperty(String.format("EE: %.2f%%, ER: %.2f%%",
                            globalPf.getEmployeeContributionRate(), globalPf.getEmployerContributionRate()));
                } else {
                    return new SimpleStringProperty("EE: 0.12, ER: 0.12 (Pending Global)");
                }
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Inactive"));

        // Status column styling using pure JavaFX APIs
        colStatus.setCellFactory(col -> new TableCell<DesignationPolicy, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
                    if ("Active".equalsIgnoreCase(item)) {
                        setTextFill(UiTheme.COLOR_SUCCESS_GREEN);
                    } else {
                        setTextFill(UiTheme.COLOR_DANGER_RED);
                    }
                }
            }
        });

        // Row selection populates form for editing
        designationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populatePolicyForm(newVal);
            }
        });
    }

    private void setupCategoryToggleBehavior() {
        categoryField.setItems(FXCollections.observableArrayList("Permanent", "Bench"));
        categoryField.getSelectionModel().selectFirst();

        pfWageBaseCombo.setItems(FXCollections.observableArrayList("BASIC_DA", "BASIC_ONLY", "GROSS"));
        pfWageBaseCombo.getSelectionModel().selectFirst();

        categoryField.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isBench = "Bench".equalsIgnoreCase(newVal);
            dailyRateField.setDisable(!isBench);
            monthlyBasicField.setDisable(isBench);
            hraField.setDisable(isBench);
            daField.setDisable(isBench);
            conveyanceField.setDisable(isBench);

            if (isBench) {
                monthlyBasicField.setText("0.00");
                hraField.setText("0.00");
                daField.setText("0.00");
                conveyanceField.setText("0.00");
            } else {
                dailyRateField.setText("0.00");
            }
        });

        // Default state: Permanent
        dailyRateField.setDisable(true);
        dailyRateField.setText("0.00");
    }

    public void loadPolicies() {
        policyList.clear();
        policyList.addAll(policyRepo.loadDesignationPolicies());
        designationTable.setItems(policyList);
        setupDesignationTable();
    }

    private void populatePolicyForm(DesignationPolicy p) {
        desigNameField.setText(p.getDesignationName());
        categoryField.setValue(p.getEmployeeCategory());
        dailyRateField.setText(p.getDailyRate().toPlainString());
        monthlyBasicField.setText(p.getMonthlyBasicSalary().toPlainString());
        hraField.setText(p.getHra().toPlainString());
        daField.setText(p.getDa().toPlainString());
        conveyanceField.setText(p.getConveyance().toPlainString());
        otherEarningsField.setText(p.getOtherEarnings().toPlainString());
        otherDeductionsField.setText(p.getOtherDeductions().toPlainString());
        pfApplicableCheck.setSelected(p.isPfApplicable());
        employeePfRateField.setText(p.getEmployeePfRate() != null ? p.getEmployeePfRate().toPlainString() : "");
        employerPfRateField.setText(p.getEmployerPfRate() != null ? p.getEmployerPfRate().toPlainString() : "");
        pfWageBaseCombo.setValue(p.getPfWageBase() != null ? p.getPfWageBase() : "BASIC_DA");
        activePolicyCheck.setSelected(p.isActive());
    }

    @FXML
    public void handleNewPolicy() {
        designationTable.getSelectionModel().clearSelection();
        desigNameField.clear();
        categoryField.getSelectionModel().selectFirst();
        dailyRateField.setText("0.00");
        monthlyBasicField.setText("35000.00");
        hraField.setText("3500.00");
        daField.setText("2000.00");
        conveyanceField.setText("1000.00");
        otherEarningsField.setText("0.00");
        otherDeductionsField.setText("0.00");
        pfApplicableCheck.setSelected(false);
        employeePfRateField.clear();
        employerPfRateField.clear();
        activePolicyCheck.setSelected(true);
    }

    @FXML
    public void handleSaveDesignationPolicy() {
        String desigName = desigNameField.getText() == null ? "" : desigNameField.getText().trim();
        String category = categoryField.getValue();

        if (desigName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Designation Name is mandatory.");
            return;
        }

        try {
            BigDecimal dailyRate = new BigDecimal(dailyRateField.getText().trim());
            BigDecimal monthlyBasic = new BigDecimal(monthlyBasicField.getText().trim());
            BigDecimal hra = new BigDecimal(hraField.getText().trim());
            BigDecimal da = new BigDecimal(daField.getText().trim());
            BigDecimal conveyance = new BigDecimal(conveyanceField.getText().trim());
            BigDecimal otherEarnings = new BigDecimal(otherEarningsField.getText().trim());
            BigDecimal otherDeductions = new BigDecimal(otherDeductionsField.getText().trim());
            boolean pfApp = pfApplicableCheck.isSelected();

            BigDecimal eeRate = null;
            BigDecimal erRate = null;
            String wageBase = pfWageBaseCombo.getValue();

            // Strict rule: If PF is applicable, administrator MUST configure explicit rates (no assumed 12%)
            if (pfApp) {
                String eeStr = employeePfRateField.getText() == null ? "" : employeePfRateField.getText().trim();
                String erStr = employerPfRateField.getText() == null ? "" : employerPfRateField.getText().trim();

                if (eeStr.isEmpty() || erStr.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "PF Configuration Required",
                            "When PF is marked eligible, both Employee and Employer contribution rates must be explicitly configured.");
                    return;
                }

                eeRate = new BigDecimal(eeStr);
                erRate = new BigDecimal(erStr);
                if (eeRate.compareTo(BigDecimal.ONE) > 0) eeRate = eeRate.divide(new BigDecimal("100"));
                if (erRate.compareTo(BigDecimal.ONE) > 0) erRate = erRate.divide(new BigDecimal("100"));

                if (eeRate.compareTo(BigDecimal.ZERO) < 0 || erRate.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert(Alert.AlertType.ERROR, "Invalid PF Rates", "PF contribution rates cannot be negative.");
                    return;
                }
            }

            boolean active = activePolicyCheck.isSelected();

            DesignationPolicy policy = new DesignationPolicy(
                    desigName, category, monthlyBasic, hra, da,
                    conveyance, otherEarnings, dailyRate, pfApp,
                    eeRate, erRate, wageBase, otherDeductions, active
            );

            policyRepo.saveDesignationPolicy(policy);
            loadPolicies();
            showAlert(Alert.AlertType.INFORMATION, "Policy Saved", "Policy for '" + desigName + "' (" + category + ") successfully saved.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Number Format Error", "Please enter valid numeric monetary values:\n" + e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Failed to save policy: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeactivatePolicy() {
        DesignationPolicy selected = designationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a designation policy to deactivate.");
            return;
        }

        try {
            DesignationPolicy updated = new DesignationPolicy(
                    selected.getDesignationName(), selected.getEmployeeCategory(),
                    selected.getMonthlyBasicSalary(), selected.getHra(), selected.getDa(),
                    selected.getConveyance(), selected.getOtherEarnings(), selected.getDailyRate(),
                    selected.isPfApplicable(), selected.getEmployeePfRate(),
                    selected.getEmployerPfRate(), selected.getPfWageBase(),
                    selected.getOtherDeductions(), false
            );
            policyRepo.saveDesignationPolicy(updated);
            loadPolicies();
            showAlert(Alert.AlertType.INFORMATION, "Policy Deactivated", "Policy has been deactivated. Historical records remain unaffected.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Deactivation Failed", "Error: " + e.getMessage());
        }
    }

    // --- TAB 2: Global PF Policy Methods ---

    private void setupGlobalPfTab() {
        globalPfWageBaseCombo.setItems(FXCollections.observableArrayList("BASIC_DA", "BASIC_ONLY", "GROSS"));
        globalPfWageBaseCombo.getSelectionModel().selectFirst();
        refreshGlobalPfDisplay();
    }

    private void refreshGlobalPfDisplay() {
        PFPolicy pf = policyRepo.loadPfPolicy();

        if (pf != null) {
            globalPfApplicableCheck.setSelected(pf.isDefaultApplicable());
            globalEeRateField.setText(pf.getEmployeeContributionRate() != null ? pf.getEmployeeContributionRate().toPlainString() : "");
            globalErRateField.setText(pf.getEmployerContributionRate() != null ? pf.getEmployerContributionRate().toPlainString() : "");
            if (pf.getWageBaseType() != null) globalPfWageBaseCombo.setValue(pf.getWageBaseType());
            globalWageCeilingField.setText(pf.getWageCeiling() != null ? pf.getWageCeiling().toPlainString() : "");
            globalEnforceCeilingCheck.setSelected(pf.isEnforceWageCeiling());

            if (pf.isDefaultApplicable() && pf.isConfigured()) {
                pfStatusTitleLabel.setText("Active (Administrator Configured)");
                pfStatusTitleLabel.setTextFill(UiTheme.COLOR_SUCCESS_GREEN);
                pfStatusTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
                pfStatusDescLabel.setText(String.format("Global PF is ACTIVE. Statutory deductions apply at Employee %.2f%% and Employer %.2f%% on %s.",
                        pf.getEmployeeContributionRate().doubleValue() <= 1.0 ? pf.getEmployeeContributionRate().doubleValue() * 100 : pf.getEmployeeContributionRate().doubleValue(),
                        pf.getEmployerContributionRate().doubleValue() <= 1.0 ? pf.getEmployerContributionRate().doubleValue() * 100 : pf.getEmployerContributionRate().doubleValue(),
                        pf.getWageBaseType()));
                pfStatusNoticeBox.setBackground(new Background(new BackgroundFill(Color.rgb(240, 253, 244), new CornerRadii(6), Insets.EMPTY)));
                pfStatusNoticeBox.setBorder(new Border(new BorderStroke(Color.rgb(187, 247, 208), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
            } else {
                pfStatusTitleLabel.setText("Disabled / Unconfigured");
                pfStatusTitleLabel.setTextFill(UiTheme.COLOR_DANGER_RED);
                pfStatusTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
                pfStatusDescLabel.setText("Global PF is currently DISABLED. By statutory rule, no PF will be deducted from any employee until an administrator explicitly configures and saves rates here.");
                pfStatusNoticeBox.setBackground(new Background(new BackgroundFill(Color.rgb(254, 242, 242), new CornerRadii(6), Insets.EMPTY)));
                pfStatusNoticeBox.setBorder(new Border(new BorderStroke(Color.rgb(254, 202, 202), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
            }
        }
    }

    @FXML
    public void handleSaveGlobalPfPolicy() {
        boolean enabled = globalPfApplicableCheck.isSelected();
        BigDecimal eeRate = null;
        BigDecimal erRate = null;
        BigDecimal wageCeiling = null;
        String wageBase = globalPfWageBaseCombo.getValue();
        boolean enforceCeiling = globalEnforceCeilingCheck.isSelected();

        if (enabled) {
            String eeStr = globalEeRateField.getText() == null ? "" : globalEeRateField.getText().trim();
            String erStr = globalErRateField.getText() == null ? "" : globalErRateField.getText().trim();

            if (eeStr.isEmpty() || erStr.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Configuration Error",
                        "When Global PF is enabled, both Employee and Employer contribution rates must be provided.");
                return;
            }

            try {
                eeRate = new BigDecimal(eeStr);
                erRate = new BigDecimal(erStr);
                if (eeRate.compareTo(BigDecimal.ONE) > 0) eeRate = eeRate.divide(new BigDecimal("100"));
                if (erRate.compareTo(BigDecimal.ONE) > 0) erRate = erRate.divide(new BigDecimal("100"));

                if (eeRate.compareTo(BigDecimal.ZERO) < 0 || erRate.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Contribution rates cannot be negative.");
                    return;
                }

                String ceilingStr = globalWageCeilingField.getText() == null ? "" : globalWageCeilingField.getText().trim();
                if (!ceilingStr.isEmpty()) {
                    wageCeiling = new BigDecimal(ceilingStr);
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Format Error", "Please enter valid numeric rates:\n" + e.getMessage());
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Global PF Policy Update");
        confirm.setHeaderText("Update Company-wide Statutory PF Policy?");
        confirm.setContentText(enabled ?
                "Global PF will be ACTIVATED. Employees holding PF-eligible designations will have PF contributions deducted during monthly payroll." :
                "Global PF will be DISABLED. No PF deductions will occur during monthly payroll.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            PFPolicy updatedPolicy = new PFPolicy(enabled, eeRate, erRate, wageBase, wageCeiling, enforceCeiling);
            policyRepo.savePfPolicy(updatedPolicy);
            refreshGlobalPfDisplay();
            loadPolicies();
            showAlert(Alert.AlertType.INFORMATION, "Policy Saved", "Global Provident Fund configuration successfully updated.");
        }
    }

    // --- TAB 3: Professional Tax Methods ---

    private void setupProfessionalTaxTab() {
        ptStateSelector.setItems(FXCollections.observableArrayList("Telangana", "Andhra Pradesh"));
        ptStateSelector.getSelectionModel().selectFirst();

        colPtMin.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.formatRupees(cell.getValue().getMinSalary())));
        colPtMax.setCellValueFactory(cell -> {
            BigDecimal max = cell.getValue().getMaxSalary();
            return new SimpleStringProperty(max.compareTo(new BigDecimal("999999999")) >= 0 ? "Above" : CurrencyFormatter.formatRupees(max));
        });
        colPtTax.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.formatRupees(cell.getValue().getTaxAmount()) + " / month"));

        loadPtSlabsForSelectedState();
        ptStateSelector.valueProperty().addListener((obs, oldVal, newVal) -> loadPtSlabsForSelectedState());
    }

    private void loadPtSlabsForSelectedState() {
        String state = ptStateSelector.getValue();
        if (state == null) return;
        ProfessionalTaxPolicy policy = policyRepo.getPtPolicy(state);
        ptTable.setItems(FXCollections.observableArrayList(policy.getSlabs()));
    }

    // --- TAB 4: Company Details Methods ---

    private void setupCompanyDetailsTab() {
        CompanySettings s = policyRepo.loadCompanySettings();
        companyNameField.setText(s.getCompanyName());
        companyAddressArea.setText(s.getCompanyAddress());
        payslipTitleField.setText(s.getPayslipTitle());
        contactEmailField.setText(s.getContactEmail());
        contactPhoneField.setText(s.getContactPhone());
    }

    @FXML
    public void handleSaveCompanySettings() {
        String name = companyNameField.getText().trim();
        String addr = companyAddressArea.getText().trim();
        String title = payslipTitleField.getText().trim();
        String email = contactEmailField.getText().trim();
        String phone = contactPhoneField.getText().trim();

        if (name.isEmpty() || addr.isEmpty() || title.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Company name, address, and payslip title are mandatory.");
            return;
        }

        CompanySettings settings = new CompanySettings(name, addr, title, email, phone);
        try {
            policyRepo.saveCompanySettings(settings);
            showAlert(Alert.AlertType.INFORMATION, "Settings Saved", "Company details successfully updated.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save company settings:\n" + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
