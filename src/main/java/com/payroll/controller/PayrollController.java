package com.payroll.controller;

import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PayrollCalculator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
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
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Monthly Payroll Processing screen.
 * Supports individual permanent and bench employee processing with live breakdowns,
 * duplicate payroll prevention, and monthly batch execution.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class PayrollController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox viewContainer;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private TabPane tabPane;

    // --- TAB 1: Permanent Employee Controls ---
    @FXML private VBox permFormCard;
    @FXML private Label permSec1Header;
    @FXML private Label permSec2Header;
    @FXML private Label permSec3Header;

    @FXML private Label lblPermSelectEmp;
    @FXML private ComboBox<Employee> permEmployeeCombo;
    @FXML private Label lblPermPayPeriod;
    @FXML private TextField permPayPeriodField;
    @FXML private Label lblPermPayDate;
    @FXML private DatePicker permPayDatePicker;
    @FXML private Label lblPermLocation;
    @FXML private TextField permLocationField;
    @FXML private Label lblPermDept;
    @FXML private TextField permDeptField;
    @FXML private TextField permDojField;
    @FXML private Label lblPermPfUan;
    @FXML private TextField permPfUanField;
    @FXML private Label lblPermPtConfig;
    @FXML private TextField permPtConfigField;

    @FXML private Label lblPermTotalDays;
    @FXML private TextField permTotalDaysField;
    @FXML private Label lblPermPresentDays;
    @FXML private TextField permPresentDaysField;
    @FXML private Label lblPermAbsentDays;
    @FXML private TextField permAbsentDaysField;

    @FXML private Label lblPermDesig;
    @FXML private TextField permDesigField;
    @FXML private Label lblPermPolicyBasic;
    @FXML private TextField permPolicyBasicField;
    @FXML private Label lblPermPolicyHra;
    @FXML private TextField permPolicyHraField;
    @FXML private Label lblPermPolicyDa;
    @FXML private TextField permPolicyDaField;
    @FXML private Label lblPermPolicyConveyance;
    @FXML private TextField permPolicyConveyanceField;
    @FXML private Label lblPermPfStatus;
    @FXML private TextField permPfStatusField;

    @FXML private Button btnPermReset;
    @FXML private Button btnPermProcess;

    // Permanent Live Breakdown Card
    @FXML private VBox permBreakdownCard;
    @FXML private Label permBreakdownHeader;
    @FXML private Label permBreakdownSubtitle;
    @FXML private Label permEarnTitle;
    @FXML private Label permPayableBasicLabel;
    @FXML private Label permPayableHraLabel;
    @FXML private Label permPayableDaLabel;
    @FXML private Label permPayableConveyanceLabel;
    @FXML private Label permOtherEarningsLabel;
    @FXML private Label permGrossHeaderLabel;
    @FXML private Label permGrossEarningsLabel;

    @FXML private Label permDedTitle;
    @FXML private Label permEmployeePfLabel;
    @FXML private Label permPtLabel;
    @FXML private Label permOtherDeductionsLabel;
    @FXML private Label permTotalDedHeaderLabel;
    @FXML private Label permTotalDeductionsLabel;

    @FXML private VBox permNetSalaryBox;
    @FXML private Label permNetHeaderLabel;
    @FXML private Label permNetSalaryLabel;

    @FXML private VBox permEmployerPfBox;
    @FXML private Label permEmployerPfHeader;
    @FXML private Label permEmployerPfLabel;
    @FXML private Label permEmployerPfNote;

    // --- TAB 2: Bench Employee Controls ---
    @FXML private VBox benchFormCard;
    @FXML private Label benchSec1Header;
    @FXML private Label benchSec2Header;
    @FXML private Label benchSec3Header;

    @FXML private Label lblBenchSelectEmp;
    @FXML private ComboBox<Employee> benchEmployeeCombo;
    @FXML private Label lblBenchPayPeriod;
    @FXML private TextField benchPayPeriodField;
    @FXML private Label lblBenchPayDate;
    @FXML private DatePicker benchPayDatePicker;
    @FXML private Label lblBenchLocation;
    @FXML private TextField benchLocationField;
    @FXML private Label lblBenchDept;
    @FXML private TextField benchDeptField;
    @FXML private TextField benchDojField;
    @FXML private Label lblBenchPfUan;
    @FXML private TextField benchPfUanField;
    @FXML private Label lblBenchPtConfig;
    @FXML private TextField benchPtConfigField;

    @FXML private Label lblBenchProject;
    @FXML private ComboBox<String> benchProjectCombo;
    @FXML private Label lblBenchProjectDays;
    @FXML private TextField benchProjectDaysField;

    @FXML private Label lblBenchDesig;
    @FXML private TextField benchDesigField;
    @FXML private Label lblBenchDailyRate;
    @FXML private TextField benchDailyRateField;
    @FXML private Label benchRuleNoticeLabel;

    @FXML private Button btnBenchReset;
    @FXML private Button btnBenchProcess;

    // Bench Live Breakdown Card
    @FXML private VBox benchBreakdownCard;
    @FXML private Label benchBreakdownHeader;
    @FXML private Label benchBreakdownSubtitle;
    @FXML private Label benchEarnTitle;
    @FXML private Label benchPayableBasicLabel;
    @FXML private Label benchHraLabel;
    @FXML private Label benchDaLabel;
    @FXML private Label benchConveyanceLabel;
    @FXML private Label benchGrossHeaderLabel;
    @FXML private Label benchGrossEarningsLabel;

    @FXML private Label benchDedTitle;
    @FXML private Label benchEmployeePfLabel;
    @FXML private Label benchPtLabel;
    @FXML private Label benchOtherDeductionsLabel;
    @FXML private Label benchTotalDedHeaderLabel;
    @FXML private Label benchTotalDeductionsLabel;

    @FXML private VBox benchNetSalaryBox;
    @FXML private Label benchNetHeaderLabel;
    @FXML private Label benchNetSalaryLabel;

    @FXML private VBox benchEmployerPfBox;
    @FXML private Label benchEmployerPfHeader;
    @FXML private Label benchEmployerPfLabel;
    @FXML private Label benchEmployerPfNote;

    // --- TAB 3: Batch Run Controls ---
    @FXML private VBox batchCard;
    @FXML private Label batchTitleLabel;
    @FXML private Label batchSubtitleLabel;
    @FXML private Label lblBatchCat;
    @FXML private ComboBox<String> batchCategoryCombo;
    @FXML private Label lblBatchPeriod;
    @FXML private TextField batchPeriodField;
    @FXML private Label lblBatchDays;
    @FXML private TextField batchDaysField;
    @FXML private Button btnRunBatch;
    @FXML private Label batchStatusLabel;

    @FXML private TableView<BatchResult> batchResultTable;
    @FXML private TableColumn<BatchResult, String> colBatchRecordId;
    @FXML private TableColumn<BatchResult, String> colBatchEmpName;
    @FXML private TableColumn<BatchResult, String> colBatchCategory;
    @FXML private TableColumn<BatchResult, String> colBatchGross;
    @FXML private TableColumn<BatchResult, String> colBatchDed;
    @FXML private TableColumn<BatchResult, String> colBatchNet;
    @FXML private TableColumn<BatchResult, String> colBatchStatus;

    // Repositories & Services
    private final EmployeeRepository employeeRepo;
    private final PolicyRepository policyRepo;
    private final PayrollRepository payrollRepo;
    private final PayrollCalculator payrollCalculator;

    // Current calculation cache
    private PayrollRecord currentPermRecord;
    private PayrollRecord currentBenchRecord;
    private boolean showDialogs = true;

    public PayrollController() {
        this(AppContext.getInstance().getEmployeeRepository(),
             AppContext.getInstance().getPolicyRepository(),
             AppContext.getInstance().getPayrollRepository(),
             AppContext.getInstance().getPayrollCalculator());
    }

    public PayrollController(EmployeeRepository employeeRepo, PolicyRepository policyRepo,
                             PayrollRepository payrollRepo, PayrollCalculator payrollCalculator) {
        this.employeeRepo = employeeRepo;
        this.policyRepo = policyRepo;
        this.payrollRepo = payrollRepo;
        this.payrollCalculator = payrollCalculator;
    }

    public void setShowDialogs(boolean showDialogs) {
        this.showDialogs = showDialogs;
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupDefaultDates();
        setupEmployeeCombos();
        setupBenchProjects();
        setupBatchTable();
        setupLiveCalculationListeners();
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

        // Cards
        if (permFormCard != null) UiTheme.applyCard(permFormCard);
        if (permBreakdownCard != null) UiTheme.applyCard(permBreakdownCard);
        if (benchFormCard != null) UiTheme.applyCard(benchFormCard);
        if (benchBreakdownCard != null) UiTheme.applyCard(benchBreakdownCard);
        if (batchCard != null) UiTheme.applyCard(batchCard);

        // Section Headers
        Label[] headers = {
                permSec1Header, permSec2Header, permSec3Header, permBreakdownHeader,
                benchSec1Header, benchSec2Header, benchSec3Header, benchBreakdownHeader,
                batchTitleLabel
        };
        for (Label h : headers) {
            if (h != null) UiTheme.applySectionHeader(h);
        }

        // Subtitles
        Label[] subs = {permBreakdownSubtitle, benchBreakdownSubtitle, batchSubtitleLabel, benchRuleNoticeLabel};
        for (Label s : subs) {
            if (s != null) {
                s.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
                s.setTextFill(UiTheme.COLOR_MUTED_GRAY);
            }
        }

        // Form Labels
        Label[] formLabels = {
                lblPermSelectEmp, lblPermPayPeriod, lblPermPayDate, lblPermLocation,
                lblPermDept, lblPermPfUan, lblPermPtConfig,
                lblPermTotalDays, lblPermPresentDays, lblPermAbsentDays, lblPermDesig,
                lblPermPolicyBasic, lblPermPolicyHra, lblPermPolicyDa, lblPermPolicyConveyance,
                lblPermPfStatus, lblBenchSelectEmp, lblBenchPayPeriod, lblBenchPayDate,
                lblBenchLocation, lblBenchDept, lblBenchPfUan, lblBenchPtConfig,
                lblBenchProject, lblBenchProjectDays, lblBenchDesig,
                lblBenchDailyRate, lblBatchCat, lblBatchPeriod, lblBatchDays
        };
        for (Label l : formLabels) {
            if (l != null) UiTheme.applyFormLabel(l);
        }

        // Breakdown Titles
        Label[] breakTitles = {permEarnTitle, permDedTitle, benchEarnTitle, benchDedTitle};
        for (Label bt : breakTitles) {
            if (bt != null) {
                bt.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
                bt.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
            }
        }

        if (permGrossHeaderLabel != null) permGrossHeaderLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        if (permTotalDedHeaderLabel != null) permTotalDedHeaderLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        if (benchGrossHeaderLabel != null) benchGrossHeaderLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        if (benchTotalDedHeaderLabel != null) benchTotalDedHeaderLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));

        // Input Fields
        TextInputControl[] inputs = {
                permPayPeriodField, permTotalDaysField, permPresentDaysField,
                benchPayPeriodField, benchProjectDaysField, batchPeriodField, batchDaysField
        };
        for (TextInputControl in : inputs) {
            if (in != null) UiTheme.applyInputField(in);
        }

        // Read-only fields
        TextInputControl[] readOnly = {
                permLocationField, permDeptField, permDojField, permPfUanField, permPtConfigField,
                permAbsentDaysField, permDesigField, permPolicyBasicField,
                permPolicyHraField, permPolicyDaField, permPolicyConveyanceField, permPfStatusField,
                benchLocationField, benchDeptField, benchDojField, benchPfUanField, benchPtConfigField,
                benchDesigField, benchDailyRateField
        };
        for (TextInputControl ro : readOnly) {
            if (ro != null) UiTheme.applyReadOnlyField(ro);
        }

        // Combos & DatePickers
        if (permEmployeeCombo != null) UiTheme.applyComboBox(permEmployeeCombo);
        if (benchEmployeeCombo != null) UiTheme.applyComboBox(benchEmployeeCombo);
        if (benchProjectCombo != null) UiTheme.applyComboBox(benchProjectCombo);
        if (batchCategoryCombo != null) UiTheme.applyComboBox(batchCategoryCombo);
        if (permPayDatePicker != null) UiTheme.applyDatePicker(permPayDatePicker);
        if (benchPayDatePicker != null) UiTheme.applyDatePicker(benchPayDatePicker);

        // Buttons
        if (btnPermProcess != null) UiTheme.applyPrimaryButton(btnPermProcess);
        if (btnPermReset != null) UiTheme.applySecondaryButton(btnPermReset);
        if (btnBenchProcess != null) UiTheme.applyPrimaryButton(btnBenchProcess);
        if (btnBenchReset != null) UiTheme.applySecondaryButton(btnBenchReset);
        if (btnRunBatch != null) UiTheme.applyPrimaryButton(btnRunBatch);

        // Net Salary Box Highlights
        styleNetSalaryBox(permNetSalaryBox, permNetHeaderLabel, permNetSalaryLabel);
        styleNetSalaryBox(benchNetSalaryBox, benchNetHeaderLabel, benchNetSalaryLabel);

        // Employer PF Boxes
        styleEmployerPfBox(permEmployerPfBox, permEmployerPfHeader, permEmployerPfLabel, permEmployerPfNote);
        styleEmployerPfBox(benchEmployerPfBox, benchEmployerPfHeader, benchEmployerPfLabel, benchEmployerPfNote);
    }

    private void styleNetSalaryBox(VBox box, Label header, Label value) {
        if (box == null) return;
        box.setBackground(new Background(new BackgroundFill(Color.rgb(239, 246, 255), new CornerRadii(6), Insets.EMPTY)));
        box.setBorder(new Border(new BorderStroke(Color.rgb(191, 219, 254), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5))));
        if (header != null) {
            header.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
            header.setTextFill(UiTheme.COLOR_ACTION_BLUE);
        }
        if (value != null) {
            value.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 20));
            value.setTextFill(UiTheme.COLOR_ACTION_BLUE);
        }
    }

    private void styleEmployerPfBox(VBox box, Label header, Label value, Label note) {
        if (box == null) return;
        box.setBackground(new Background(new BackgroundFill(Color.rgb(248, 250, 252), new CornerRadii(6), Insets.EMPTY)));
        box.setBorder(new Border(new BorderStroke(Color.rgb(226, 232, 240), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        if (header != null) {
            header.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 10));
            header.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
        if (value != null) {
            value.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
            value.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }
        if (note != null) {
            note.setFont(Font.font(UiTheme.FONT_FAMILY, 10));
            note.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        }
    }

    private void setupDefaultDates() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        permPayPeriodField.setText(currentMonth);
        benchPayPeriodField.setText(currentMonth);
        batchPeriodField.setText(currentMonth);

        permPayDatePicker.setValue(LocalDate.now());
        benchPayDatePicker.setValue(LocalDate.now());

        batchCategoryCombo.setItems(FXCollections.observableArrayList("Permanent", "Bench"));
        batchCategoryCombo.getSelectionModel().selectFirst();
    }

    private void setupEmployeeCombos() {
        List<Employee> all = employeeRepo.loadAll();

        // 1. Permanent Employees
        List<Employee> permList = all.stream()
                .filter(e -> e.isActive() && "Permanent".equalsIgnoreCase(e.getEmployeeCategory()))
                .toList();
        permEmployeeCombo.setItems(FXCollections.observableArrayList(permList));
        setEmployeeConverter(permEmployeeCombo);

        // 2. Bench Employees
        List<Employee> benchList = all.stream()
                .filter(e -> e.isActive() && "Bench".equalsIgnoreCase(e.getEmployeeCategory()))
                .toList();
        benchEmployeeCombo.setItems(FXCollections.observableArrayList(benchList));
        setEmployeeConverter(benchEmployeeCombo);
    }

    private void setEmployeeConverter(ComboBox<Employee> combo) {
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee e) {
                return e == null ? "" : e.getFullName() + " (" + e.getEmployeeId() + ") - " + e.getDesignation();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });
    }

    private void setupBenchProjects() {
        List<String> projects = policyRepo.loadProjects();
        benchProjectCombo.setItems(FXCollections.observableArrayList(projects));
        if (!projects.isEmpty()) {
            benchProjectCombo.getSelectionModel().selectFirst();
        }
    }

    private void setupLiveCalculationListeners() {
        // Permanent listeners
        permEmployeeCombo.valueProperty().addListener((obs, oldVal, newVal) -> recalculatePermPayroll());
        permPayPeriodField.textProperty().addListener((obs, oldVal, newVal) -> recalculatePermPayroll());
        permPayDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> recalculatePermPayroll());
        permTotalDaysField.textProperty().addListener((obs, oldVal, newVal) -> recalculatePermPayroll());
        permPresentDaysField.textProperty().addListener((obs, oldVal, newVal) -> recalculatePermPayroll());

        // Bench listeners
        benchEmployeeCombo.valueProperty().addListener((obs, oldVal, newVal) -> recalculateBenchPayroll());
        benchPayPeriodField.textProperty().addListener((obs, oldVal, newVal) -> recalculateBenchPayroll());
        benchPayDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> recalculateBenchPayroll());
        benchProjectCombo.valueProperty().addListener((obs, oldVal, newVal) -> recalculateBenchPayroll());
        benchProjectDaysField.textProperty().addListener((obs, oldVal, newVal) -> recalculateBenchPayroll());
    }

    // --- PERMANENT PAYROLL LOGIC ---

    private void recalculatePermPayroll() {
        Employee employee = permEmployeeCombo.getValue();
        if (employee == null) {
            clearPermLabels();
            return;
        }

        permDesigField.setText(employee.getDesignation());
        permLocationField.setText(employee.getWorkLocation());
        permDeptField.setText(employee.getDepartment() != null ? employee.getDepartment() : "General");
        permDojField.setText(employee.getDateOfJoining() != null ? employee.getDateOfJoining().toString() : "N/A");
        permPfUanField.setText((employee.getPfAccountNumber() != null && !employee.getPfAccountNumber().isEmpty() ? employee.getPfAccountNumber() : "None") +
                " / " + (employee.getUan() != null && !employee.getUan().isEmpty() ? employee.getUan() : "None"));

        ProfessionalTaxPolicy ptPolicy = policyRepo.getPtPolicy(employee.getWorkLocation());
        permPtConfigField.setText(ptPolicy != null ? (employee.getWorkLocation() + " (" + ptPolicy.getSlabs().size() + " configurable slabs)") : (employee.getWorkLocation() + " [No policy configured]"));

        Optional<DesignationPolicy> optPolicy = policyRepo.findDesignationPolicy(employee.getDesignation(), "Permanent");
        if (optPolicy.isEmpty()) {
            clearPermLabels();
            permDesigField.setText(employee.getDesignation() + " (No Active Policy Found)");
            return;
        }

        DesignationPolicy policy = optPolicy.get();
        permPolicyBasicField.setText(CurrencyFormatter.formatRupees(policy.getMonthlyBasicSalary()));
        permPolicyHraField.setText(CurrencyFormatter.formatRupees(policy.getHra()));
        permPolicyDaField.setText(CurrencyFormatter.formatRupees(policy.getDa()));
        permPolicyConveyanceField.setText(CurrencyFormatter.formatRupees(policy.getConveyance()));

        PFPolicy globalPf = policyRepo.loadPfPolicy();
        boolean pfActive = employee.isPfApplicable() && policy.isPfApplicable() &&
                globalPf != null && globalPf.isDefaultApplicable() && globalPf.isConfigured();
        permPfStatusField.setText(pfActive ? "Active (" + globalPf.getEmployeeContributionRate() + ")" : "Not Deducted (Global Inactive / Exempt)");

        // Validate days input
        int totalDays;
        int presentDays;
        try {
            totalDays = Integer.parseInt(permTotalDaysField.getText().trim());
            presentDays = Integer.parseInt(permPresentDaysField.getText().trim());
        } catch (NumberFormatException e) {
            clearPermBreakdownLabels();
            return;
        }

        if (totalDays <= 0 || presentDays < 0 || presentDays > totalDays) {
            permAbsentDaysField.setText("Invalid");
            clearPermBreakdownLabels();
            return;
        }

        int absentDays = totalDays - presentDays;
        permAbsentDaysField.setText(String.valueOf(absentDays));

        String payPeriod = permPayPeriodField.getText().trim();
        LocalDate payDate = permPayDatePicker.getValue() != null ? permPayDatePicker.getValue() : LocalDate.now();

        AttendanceRecord attendance = new AttendanceRecord(employee.getEmployeeId(), payPeriod, totalDays, presentDays);

        try {
            currentPermRecord = payrollCalculator.calculatePayroll(employee, policy, attendance, null, payPeriod, payDate, globalPf);

            // Populate Live Preview
            permPayableBasicLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPayableBasic()));
            permPayableHraLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPayableHra()));
            permPayableDaLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPayableDa()));
            permPayableConveyanceLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPayableConveyance()));
            permOtherEarningsLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getOtherEarnings()));
            permGrossEarningsLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getGrossEarnings()));

            permEmployeePfLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPfEmployeeDeduction()));
            permPtLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getProfessionalTax()));
            permOtherDeductionsLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getOtherDeductions()));
            permTotalDeductionsLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getTotalDeductions()));

            permNetSalaryLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getNetSalary()));
            permEmployerPfLabel.setText(CurrencyFormatter.formatRupees(currentPermRecord.getPfEmployerContribution()));
        } catch (Exception e) {
            clearPermBreakdownLabels();
        }
    }

    private void clearPermLabels() {
        permDesigField.clear();
        permLocationField.clear();
        permDeptField.clear();
        permDojField.clear();
        permPfUanField.clear();
        permPtConfigField.clear();
        permPolicyBasicField.clear();
        permPolicyHraField.clear();
        permPolicyDaField.clear();
        permPolicyConveyanceField.clear();
        permPfStatusField.clear();
        permAbsentDaysField.setText("0");
        clearPermBreakdownLabels();
    }

    private void clearPermBreakdownLabels() {
        permPayableBasicLabel.setText("₹ 0.00");
        permPayableHraLabel.setText("₹ 0.00");
        permPayableDaLabel.setText("₹ 0.00");
        permPayableConveyanceLabel.setText("₹ 0.00");
        permOtherEarningsLabel.setText("₹ 0.00");
        permGrossEarningsLabel.setText("₹ 0.00");
        permEmployeePfLabel.setText("₹ 0.00");
        permPtLabel.setText("₹ 0.00");
        permOtherDeductionsLabel.setText("₹ 0.00");
        permTotalDeductionsLabel.setText("₹ 0.00");
        permNetSalaryLabel.setText("₹ 0.00");
        permEmployerPfLabel.setText("₹ 0.00");
        currentPermRecord = null;
    }

    @FXML
    public void handlePermReset() {
        permEmployeeCombo.setValue(null);
        clearPermLabels();
        permTotalDaysField.setText("30");
        permPresentDaysField.setText("30");
        permPayPeriodField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        permPayDatePicker.setValue(LocalDate.now());
    }

    @FXML
    public void handlePermProcess() {
        Employee employee = permEmployeeCombo.getValue();
        if (employee == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Required", "Please select an active permanent employee.");
            return;
        }

        String payPeriod = permPayPeriodField.getText().trim();
        if (payPeriod.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Pay period is mandatory (e.g. 2026-11).");
            return;
        }

        if (currentPermRecord == null) {
            showAlert(Alert.AlertType.ERROR, "Calculation Error", "Please verify attendance inputs before processing.");
            return;
        }

        // Strict Duplicate Prevention Rule: Cannot process payroll twice for same employee in same pay period
        if (payrollRepo.exists(employee.getEmployeeId(), payPeriod)) {
            showAlert(Alert.AlertType.WARNING, "Duplicate Payroll Prevention",
                    "Payroll for employee " + employee.getFullName() + " (" + employee.getEmployeeId() +
                            ") has ALREADY been processed for period " + payPeriod + ".\n\n" +
                            "Duplicate payroll processing for the same pay period is strictly prohibited.");
            return;
        }

        try {
            payrollRepo.save(currentPermRecord);
            showAlert(Alert.AlertType.INFORMATION, "Payroll Processed Successfully",
                    "Payroll Record ID: " + currentPermRecord.getRecordId() + "\n" +
                            "Employee: " + currentPermRecord.getEmployeeNameSnapshot() + "\n" +
                            "Pay Period: " + currentPermRecord.getPayPeriod() + "\n" +
                            "Gross Earnings: " + CurrencyFormatter.formatRupees(currentPermRecord.getGrossEarnings()) + "\n" +
                            "Total Deductions: " + CurrencyFormatter.formatRupees(currentPermRecord.getTotalDeductions()) + "\n" +
                            "Net Payable Salary: " + CurrencyFormatter.formatRupees(currentPermRecord.getNetSalary()) + "\n\n" +
                            "Immutable record saved to CSV repository. Ready for Salary Slip generation.");
            handlePermReset();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Storage Error", "Failed to save payroll record to CSV:\n" + e.getMessage());
        }
    }

    // --- BENCH PAYROLL LOGIC ---

    private void recalculateBenchPayroll() {
        Employee employee = benchEmployeeCombo.getValue();
        if (employee == null) {
            clearBenchLabels();
            return;
        }

        benchDesigField.setText(employee.getDesignation());
        benchLocationField.setText(employee.getWorkLocation());
        benchDeptField.setText(employee.getDepartment() != null ? employee.getDepartment() : "General");
        benchDojField.setText(employee.getDateOfJoining() != null ? employee.getDateOfJoining().toString() : "N/A");
        benchPfUanField.setText((employee.getPfAccountNumber() != null && !employee.getPfAccountNumber().isEmpty() ? employee.getPfAccountNumber() : "None") +
                " / " + (employee.getUan() != null && !employee.getUan().isEmpty() ? employee.getUan() : "None"));

        ProfessionalTaxPolicy ptPolicy = policyRepo.getPtPolicy(employee.getWorkLocation());
        benchPtConfigField.setText(ptPolicy != null ? (employee.getWorkLocation() + " (" + ptPolicy.getSlabs().size() + " configurable slabs)") : (employee.getWorkLocation() + " [No policy configured]"));

        Optional<DesignationPolicy> optPolicy = policyRepo.findDesignationPolicy(employee.getDesignation(), "Bench");
        if (optPolicy.isEmpty()) {
            clearBenchLabels();
            benchDesigField.setText(employee.getDesignation() + " (No Active Policy Found)");
            return;
        }

        DesignationPolicy policy = optPolicy.get();
        benchDailyRateField.setText(CurrencyFormatter.formatRupees(policy.getDailyRate()) + " / day");

        int projectDays;
        try {
            projectDays = Integer.parseInt(benchProjectDaysField.getText().trim());
        } catch (NumberFormatException e) {
            clearBenchBreakdownLabels();
            return;
        }

        if (projectDays < 0 || projectDays > 31) {
            clearBenchBreakdownLabels();
            return;
        }

        String projectName = benchProjectCombo.getValue() != null ? benchProjectCombo.getValue() : "Default Project";
        String payPeriod = benchPayPeriodField.getText().trim();
        LocalDate payDate = benchPayDatePicker.getValue() != null ? benchPayDatePicker.getValue() : LocalDate.now();

        ProjectRecord project = new ProjectRecord(employee.getEmployeeId(), payPeriod, projectName, projectDays);
        PFPolicy globalPf = policyRepo.loadPfPolicy();

        try {
            currentBenchRecord = payrollCalculator.calculatePayroll(employee, policy, null, project, payPeriod, payDate, globalPf);

            // Populate Bench Preview
            benchPayableBasicLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getPayableBasic()));
            benchHraLabel.setText("₹ 0.00 (Bench: N/A)");
            benchDaLabel.setText("₹ 0.00 (Bench: N/A)");
            benchConveyanceLabel.setText("₹ 0.00 (Bench: N/A)");
            benchGrossEarningsLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getGrossEarnings()));

            benchEmployeePfLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getPfEmployeeDeduction()));
            benchPtLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getProfessionalTax()));
            benchOtherDeductionsLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getOtherDeductions()));
            benchTotalDeductionsLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getTotalDeductions()));

            benchNetSalaryLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getNetSalary()));
            benchEmployerPfLabel.setText(CurrencyFormatter.formatRupees(currentBenchRecord.getPfEmployerContribution()));
        } catch (Exception e) {
            clearBenchBreakdownLabels();
        }
    }

    private void clearBenchLabels() {
        benchDesigField.clear();
        benchLocationField.clear();
        benchDeptField.clear();
        benchDojField.clear();
        benchPfUanField.clear();
        benchPtConfigField.clear();
        benchDailyRateField.clear();
        clearBenchBreakdownLabels();
    }

    private void clearBenchBreakdownLabels() {
        benchPayableBasicLabel.setText("₹ 0.00");
        benchHraLabel.setText("₹ 0.00");
        benchDaLabel.setText("₹ 0.00");
        benchConveyanceLabel.setText("₹ 0.00");
        benchGrossEarningsLabel.setText("₹ 0.00");
        benchEmployeePfLabel.setText("₹ 0.00");
        benchPtLabel.setText("₹ 0.00");
        benchOtherDeductionsLabel.setText("₹ 0.00");
        benchTotalDeductionsLabel.setText("₹ 0.00");
        benchNetSalaryLabel.setText("₹ 0.00");
        benchEmployerPfLabel.setText("₹ 0.00");
        currentBenchRecord = null;
    }

    @FXML
    public void handleBenchReset() {
        benchEmployeeCombo.setValue(null);
        clearBenchLabels();
        benchProjectDaysField.setText("15");
        benchPayPeriodField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        benchPayDatePicker.setValue(LocalDate.now());
    }

    @FXML
    public void handleBenchProcess() {
        Employee employee = benchEmployeeCombo.getValue();
        if (employee == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Required", "Please select an active bench employee.");
            return;
        }

        String payPeriod = benchPayPeriodField.getText().trim();
        if (payPeriod.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Pay period is mandatory.");
            return;
        }

        if (currentBenchRecord == null) {
            showAlert(Alert.AlertType.ERROR, "Calculation Error", "Please verify project inputs before processing.");
            return;
        }

        // Duplicate Check
        if (payrollRepo.exists(employee.getEmployeeId(), payPeriod)) {
            showAlert(Alert.AlertType.WARNING, "Duplicate Payroll Prevention",
                    "Payroll for employee " + employee.getFullName() + " (" + employee.getEmployeeId() +
                            ") has ALREADY been processed for period " + payPeriod + ".");
            return;
        }

        try {
            payrollRepo.save(currentBenchRecord);
            showAlert(Alert.AlertType.INFORMATION, "Bench Payroll Processed",
                    "Payroll Record ID: " + currentBenchRecord.getRecordId() + "\n" +
                            "Employee: " + currentBenchRecord.getEmployeeNameSnapshot() + "\n" +
                            "Eligible Days: " + currentBenchRecord.getEligibleProjectDays() + " days\n" +
                            "Gross Earnings: " + CurrencyFormatter.formatRupees(currentBenchRecord.getGrossEarnings()) + "\n" +
                            "Net Payable Salary: " + CurrencyFormatter.formatRupees(currentBenchRecord.getNetSalary()));
            handleBenchReset();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Storage Error", "Failed to save payroll record:\n" + e.getMessage());
        }
    }

    // --- TAB 3: BATCH PAYROLL LOGIC ---

    private void setupBatchTable() {
        colBatchRecordId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().recordId));
        colBatchEmpName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().empName));
        colBatchCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().category));
        colBatchGross.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().gross));
        colBatchDed.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().deductions));
        colBatchNet.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().net));
        colBatchStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
    }

    @FXML
    public void handleRunBatch() {
        String category = batchCategoryCombo.getValue();
        String payPeriod = batchPeriodField.getText().trim();

        if (payPeriod.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Pay period is mandatory for batch processing.");
            return;
        }

        int standardDays;
        try {
            standardDays = Integer.parseInt(batchDaysField.getText().trim());
            if (standardDays <= 0 || standardDays > 31) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Standard days must be between 1 and 31.");
            return;
        }

        List<Employee> allEmployees = employeeRepo.loadAll();
        List<Employee> targetEmployees = allEmployees.stream()
                .filter(e -> e.isActive() && e.getEmployeeCategory().equalsIgnoreCase(category))
                .toList();

        if (targetEmployees.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Employees Found", "No active " + category + " employees found to process.");
            return;
        }

        List<BatchResult> results = new ArrayList<>();
        int processedCount = 0;
        int skippedCount = 0;
        LocalDate payDate = LocalDate.now();
        PFPolicy globalPf = policyRepo.loadPfPolicy();

        for (Employee emp : targetEmployees) {
            if (payrollRepo.exists(emp.getEmployeeId(), payPeriod)) {
                results.add(new BatchResult("N/A", emp.getFullName() + " (" + emp.getEmployeeId() + ")", category, "-", "-", "-", "SKIPPED (Duplicate)"));
                skippedCount++;
                continue;
            }

            Optional<DesignationPolicy> optPolicy = policyRepo.findDesignationPolicy(emp.getDesignation(), category);
            if (optPolicy.isEmpty()) {
                results.add(new BatchResult("N/A", emp.getFullName() + " (" + emp.getEmployeeId() + ")", category, "-", "-", "-", "ERROR (No Policy)"));
                continue;
            }

            try {
                PayrollRecord record;
                if ("Permanent".equalsIgnoreCase(category)) {
                    AttendanceRecord att = new AttendanceRecord(emp.getEmployeeId(), payPeriod, standardDays, standardDays);
                    record = payrollCalculator.calculatePayroll(emp, optPolicy.get(), att, null, payPeriod, payDate, globalPf);
                } else {
                    ProjectRecord proj = new ProjectRecord(emp.getEmployeeId(), payPeriod, "Bench Operations", standardDays);
                    record = payrollCalculator.calculatePayroll(emp, optPolicy.get(), null, proj, payPeriod, payDate, globalPf);
                }

                payrollRepo.save(record);
                results.add(new BatchResult(record.getRecordId(), record.getEmployeeNameSnapshot(), category,
                        CurrencyFormatter.formatRupees(record.getGrossEarnings()),
                        CurrencyFormatter.formatRupees(record.getTotalDeductions()),
                        CurrencyFormatter.formatRupees(record.getNetSalary()),
                        "PROCESSED"));
                processedCount++;
            } catch (Exception ex) {
                results.add(new BatchResult("N/A", emp.getFullName(), category, "-", "-", "-", "FAILED (" + ex.getMessage() + ")"));
            }
        }

        batchResultTable.setItems(FXCollections.observableArrayList(results));
        batchStatusLabel.setText(String.format("Batch Complete: %d processed, %d skipped duplicates.", processedCount, skippedCount));
        batchStatusLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        batchStatusLabel.setTextFill(UiTheme.COLOR_SUCCESS_GREEN);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        if (!showDialogs) {
            return;
        }
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper model for Batch TableView
    public static class BatchResult {
        public final String recordId;
        public final String empName;
        public final String category;
        public final String gross;
        public final String deductions;
        public final String net;
        public final String status;

        public BatchResult(String recordId, String empName, String category, String gross, String deductions, String net, String status) {
            this.recordId = recordId;
            this.empName = empName;
            this.category = category;
            this.gross = gross;
            this.deductions = deductions;
            this.net = net;
            this.status = status;
        }
    }
}
