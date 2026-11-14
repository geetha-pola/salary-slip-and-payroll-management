package com.payroll.controller;

import com.payroll.model.CompanySettings;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.Employee;
import com.payroll.model.PFPolicy;
import com.payroll.model.PayrollRecord;
import com.payroll.service.AppContext;
import com.payroll.service.PayrollCalculator;
import com.payroll.service.PdfSalarySlipGenerator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import com.payroll.util.CurrencyFormatter;
import com.payroll.util.NumberToWordsConverter;
import com.payroll.util.UiTheme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Payroll History & Audit Archive module.
 * Displays all historical payroll records with search, filtering, snapshot inspection,
 * authorized correction workflows, and salary slip navigation.
 * 
 * Strict Hackathon Compliance:
 * - Pure JavaFX + FXML + Java Controllers (Strictly NO CSS, NO HTML, NO JavaScript, NO WebViews).
 * - All styling rendered via native JavaFX APIs and UiTheme.
 */
public class HistoryController {

    @FXML private VBox rootContainer;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // Toolbar
    @FXML private HBox toolbarCard;
    @FXML private Label lblSearch;
    @FXML private TextField searchField;
    @FXML private Label lblPeriod;
    @FXML private ComboBox<String> filterPeriod;
    @FXML private Label lblCategory;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label lblStatus;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Button btnReset;
    @FXML private Label totalCountLabel;

    // Table
    @FXML private TableView<PayrollRecord> historyTable;
    @FXML private TableColumn<PayrollRecord, String> colRecordId;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeId;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeName;
    @FXML private TableColumn<PayrollRecord, String> colCategory;
    @FXML private TableColumn<PayrollRecord, String> colPayPeriod;
    @FXML private TableColumn<PayrollRecord, String> colGross;
    @FXML private TableColumn<PayrollRecord, String> colDeductions;
    @FXML private TableColumn<PayrollRecord, String> colNet;
    @FXML private TableColumn<PayrollRecord, String> colStatus;
    @FXML private TableColumn<PayrollRecord, String> colDate;
    @FXML private Label tablePlaceholderLabel;

    // Snapshot Inspector Card
    @FXML private VBox previewCard;
    @FXML private Label previewTitleLabel;
    @FXML private VBox noSelectionBox;
    @FXML private Label lblNoSelectionIcon;
    @FXML private Label lblNoSelectionText;

    @FXML private ScrollPane previewScrollPane;
    @FXML private VBox empSummaryBox;
    @FXML private Label lblSummaryName;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblSummaryIdAndType;
    @FXML private Label lblSummaryDesig;
    @FXML private Label lblSummaryPeriodAndDate;
    @FXML private Label lblSummaryAttendance;

    @FXML private VBox auditTrailBox;
    @FXML private Label lblAuditHeader;
    @FXML private Label lblAuditOriginalId;
    @FXML private Label lblAuditReason;
    @FXML private Label lblAuditTimestamp;

    @FXML private Label lblBreakdownHeader;
    @FXML private GridPane breakdownGrid;

    @FXML private VBox netSalaryCard;
    @FXML private Label lblNetTitle;
    @FXML private Label lblNetValue;
    @FXML private Label lblNetInWords;

    @FXML private VBox employerPfCard;
    @FXML private Label lblEmployerPfTitle;
    @FXML private Label lblEmployerPfValue;

    @FXML private VBox policySnapshotBox;
    @FXML private Label lblPolicySnapshotTitle;
    @FXML private Label lblPolicySnapshotValue;

    @FXML private Button btnOpenSalarySlip;
    @FXML private Button btnCorrect;
    @FXML private Label actionStatusLabel;

    private final PayrollRepository payrollRepository;
    private final PolicyRepository policyRepository;
    private final EmployeeRepository employeeRepository;
    private final PdfSalarySlipGenerator pdfGenerator;
    private final PayrollCalculator payrollCalculator;

    private final ObservableList<PayrollRecord> masterPayrollData = FXCollections.observableArrayList();
    private FilteredList<PayrollRecord> filteredPayrollData;

    public HistoryController() {
        AppContext context = AppContext.getInstance();
        this.payrollRepository = context.getPayrollRepository();
        this.policyRepository = context.getPolicyRepository();
        this.employeeRepository = context.getEmployeeRepository();
        this.pdfGenerator = context.getPdfSalarySlipGenerator();
        this.payrollCalculator = context.getPayrollCalculator();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupTableColumns();
        loadPayrollData();
        setupFilters();
        setupSelectionListener();
    }

    private void applyPureJavaFxStyles() {
        UiTheme.applyPageTitle(pageTitleLabel);
        UiTheme.applyPageSubtitle(pageSubtitleLabel);

        // Toolbar
        UiTheme.applyCard(toolbarCard);
        UiTheme.applyFormLabel(lblSearch);
        UiTheme.applyInputField(searchField);
        UiTheme.applyFormLabel(lblPeriod);
        UiTheme.applyComboBox(filterPeriod);
        UiTheme.applyFormLabel(lblCategory);
        UiTheme.applyComboBox(filterCategory);
        UiTheme.applyFormLabel(lblStatus);
        UiTheme.applyComboBox(filterStatus);
        UiTheme.applySecondaryButton(btnReset);

        totalCountLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        totalCountLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        // Inspector Card
        UiTheme.applyCard(previewCard);
        previewTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 16));
        previewTitleLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

        lblNoSelectionIcon.setFont(Font.font(UiTheme.FONT_FAMILY, 36));
        lblNoSelectionText.setFont(Font.font(UiTheme.FONT_FAMILY, 13));
        lblNoSelectionText.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        // Employee summary
        lblSummaryName.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 16));
        lblSummaryName.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

        lblSummaryIdAndType.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        lblSummaryIdAndType.setTextFill(UiTheme.COLOR_ACTION_BLUE);

        lblSummaryDesig.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
        lblSummaryDesig.setTextFill(UiTheme.COLOR_TEXT_DARK);

        lblSummaryPeriodAndDate.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
        lblSummaryPeriodAndDate.setTextFill(UiTheme.COLOR_TEXT_DARK);

        lblSummaryAttendance.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        lblSummaryAttendance.setTextFill(UiTheme.COLOR_TEXT_DARK);

        // Audit Trail Box
        auditTrailBox.setBackground(new Background(new BackgroundFill(Color.rgb(254, 243, 199), new CornerRadii(6), Insets.EMPTY)));
        auditTrailBox.setBorder(new Border(new BorderStroke(Color.rgb(252, 211, 77), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));

        lblAuditHeader.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
        lblAuditHeader.setTextFill(Color.rgb(146, 64, 14));

        lblAuditOriginalId.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 11));
        lblAuditOriginalId.setTextFill(Color.rgb(146, 64, 14));

        lblAuditReason.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
        lblAuditReason.setTextFill(Color.rgb(120, 53, 15));

        lblAuditTimestamp.setFont(Font.font(UiTheme.FONT_FAMILY, 10.5));
        lblAuditTimestamp.setTextFill(Color.rgb(180, 83, 9));

        lblBreakdownHeader.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 13));
        lblBreakdownHeader.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

        // Net Salary Card
        netSalaryCard.setBackground(new Background(new BackgroundFill(Color.rgb(239, 246, 255), new CornerRadii(8), Insets.EMPTY)));
        netSalaryCard.setBorder(new Border(new BorderStroke(Color.rgb(191, 219, 254), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5))));

        lblNetTitle.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
        lblNetTitle.setTextFill(Color.rgb(30, 58, 138));

        lblNetValue.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 22));
        lblNetValue.setTextFill(Color.rgb(30, 58, 138));

        lblNetInWords.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 11.5));
        lblNetInWords.setTextFill(Color.rgb(71, 85, 105));

        // Employer PF Card
        employerPfCard.setBackground(new Background(new BackgroundFill(Color.rgb(248, 250, 252), new CornerRadii(6), Insets.EMPTY)));
        employerPfCard.setBorder(new Border(new BorderStroke(UiTheme.COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));

        lblEmployerPfTitle.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
        lblEmployerPfTitle.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        lblEmployerPfValue.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 11.5));
        lblEmployerPfValue.setTextFill(UiTheme.COLOR_TEXT_DARK);

        // Policy Snapshot Box
        policySnapshotBox.setBackground(new Background(new BackgroundFill(Color.rgb(248, 250, 252), new CornerRadii(6), Insets.EMPTY)));
        policySnapshotBox.setBorder(new Border(new BorderStroke(UiTheme.COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));

        lblPolicySnapshotTitle.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 11));
        lblPolicySnapshotTitle.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        lblPolicySnapshotValue.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
        lblPolicySnapshotValue.setTextFill(UiTheme.COLOR_TEXT_DARK);

        // Actions
        UiTheme.applyPrimaryButton(btnOpenSalarySlip);
        UiTheme.applySecondaryButton(btnCorrect);

        actionStatusLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 11.5));
        actionStatusLabel.setTextFill(UiTheme.COLOR_ACTION_BLUE);

        if (previewScrollPane != null) {
            previewScrollPane.setBackground(Background.EMPTY);
            previewScrollPane.setBorder(Border.EMPTY);
        }
    }

    private void setupTableColumns() {
        colRecordId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecordId()));
        colEmployeeId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployeeId()));
        colEmployeeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployeeNameSnapshot()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployeeType()));
        colPayPeriod.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPayPeriod()));

        colGross.setCellValueFactory(data -> new SimpleStringProperty("Rs. " + CurrencyFormatter.format(data.getValue().getGrossEarnings())));
        colDeductions.setCellValueFactory(data -> new SimpleStringProperty("Rs. " + CurrencyFormatter.format(data.getValue().getTotalDeductions())));
        colNet.setCellValueFactory(data -> new SimpleStringProperty("Rs. " + CurrencyFormatter.format(data.getValue().getNetSalary())));

        // Status column with distinct label
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<PayrollRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    if ("CORRECTED".equalsIgnoreCase(item)) {
                        UiTheme.applyBadge(badge, Color.rgb(254, 243, 199), Color.rgb(180, 83, 9));
                    } else if ("SUPERSEDED".equalsIgnoreCase(item)) {
                        UiTheme.applyBadge(badge, Color.rgb(241, 245, 249), Color.rgb(100, 116, 139));
                    } else {
                        UiTheme.applyBadge(badge, Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
                    }
                    setAlignment(Pos.CENTER);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getGenerationDate() != null ? data.getValue().getGenerationDate().toString() : "N/A"
        ));
    }

    private void loadPayrollData() {
        masterPayrollData.clear();
        List<PayrollRecord> allRecords = payrollRepository.loadAll();
        masterPayrollData.addAll(allRecords);

        // Populate Pay Period filter options
        Set<String> periods = new HashSet<>();
        for (PayrollRecord rec : allRecords) {
            if (rec.getPayPeriod() != null && !rec.getPayPeriod().trim().isEmpty()) {
                periods.add(rec.getPayPeriod().trim());
            }
        }

        ObservableList<String> periodItems = FXCollections.observableArrayList("All Periods");
        periodItems.addAll(periods.stream().sorted().toList());
        filterPeriod.setItems(periodItems);
        filterPeriod.getSelectionModel().selectFirst();

        // Category filter
        filterCategory.setItems(FXCollections.observableArrayList("All Categories", "Permanent", "Bench"));
        filterCategory.getSelectionModel().selectFirst();

        // Status filter
        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "ORIGINAL", "CORRECTED", "SUPERSEDED"));
        filterStatus.getSelectionModel().selectFirst();
    }

    private void setupFilters() {
        filteredPayrollData = new FilteredList<>(masterPayrollData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());
        filterPeriod.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());
        filterCategory.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());
        filterStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());

        SortedList<PayrollRecord> sortedData = new SortedList<>(filteredPayrollData);
        sortedData.comparatorProperty().bind(historyTable.comparatorProperty());
        historyTable.setItems(sortedData);

        updateRecordCount();
    }

    private void applyFilterPredicate() {
        String searchText = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String selectedPeriod = filterPeriod.getValue();
        String selectedCategory = filterCategory.getValue();
        String selectedStatus = filterStatus.getValue();

        filteredPayrollData.setPredicate(rec -> {
            if (rec == null) return false;

            // Search filter (Employee ID, Name, Record ID, Original ID)
            if (!searchText.isEmpty()) {
                boolean matchId = rec.getEmployeeId() != null && rec.getEmployeeId().toLowerCase().contains(searchText);
                boolean matchName = rec.getEmployeeNameSnapshot() != null && rec.getEmployeeNameSnapshot().toLowerCase().contains(searchText);
                boolean matchRecId = rec.getRecordId() != null && rec.getRecordId().toLowerCase().contains(searchText);
                boolean matchOrigId = rec.getOriginalRecordId() != null && rec.getOriginalRecordId().toLowerCase().contains(searchText);
                if (!matchId && !matchName && !matchRecId && !matchOrigId) {
                    return false;
                }
            }

            // Period filter
            if (selectedPeriod != null && !"All Periods".equalsIgnoreCase(selectedPeriod)) {
                if (!selectedPeriod.equalsIgnoreCase(rec.getPayPeriod())) {
                    return false;
                }
            }

            // Category filter
            if (selectedCategory != null && !"All Categories".equalsIgnoreCase(selectedCategory)) {
                if (!selectedCategory.equalsIgnoreCase(rec.getEmployeeType())) {
                    return false;
                }
            }

            // Status filter
            if (selectedStatus != null && !"All Statuses".equalsIgnoreCase(selectedStatus)) {
                if (!selectedStatus.equalsIgnoreCase(rec.getStatus())) {
                    return false;
                }
            }

            return true;
        });

        updateRecordCount();
    }

    private void updateRecordCount() {
        int total = filteredPayrollData != null ? filteredPayrollData.size() : masterPayrollData.size();
        long activeCount = (filteredPayrollData != null ? filteredPayrollData : masterPayrollData)
                .stream().filter(r -> !r.isSuperseded()).count();
        long supersededCount = total - activeCount;

        totalCountLabel.setText(total + " Records (" + activeCount + " Active" +
                (supersededCount > 0 ? ", " + supersededCount + " Superseded" : "") + ")");
    }

    private void setupSelectionListener() {
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updatePreviewCard(newVal);
        });
    }

    private void updatePreviewCard(PayrollRecord rec) {
        actionStatusLabel.setText("");
        if (rec == null) {
            noSelectionBox.setVisible(true);
            noSelectionBox.setManaged(true);
            previewScrollPane.setVisible(false);
            previewScrollPane.setManaged(false);
            return;
        }

        noSelectionBox.setVisible(false);
        noSelectionBox.setManaged(false);
        previewScrollPane.setVisible(true);
        previewScrollPane.setManaged(true);

        // 1. Employee Identification & Status Badge
        lblSummaryName.setText(rec.getEmployeeNameSnapshot());
        String status = rec.getStatus();
        lblStatusBadge.setText(status.toUpperCase());
        if ("CORRECTED".equalsIgnoreCase(status)) {
            UiTheme.applyBadge(lblStatusBadge, Color.rgb(254, 243, 199), Color.rgb(180, 83, 9));
        } else if ("SUPERSEDED".equalsIgnoreCase(status)) {
            UiTheme.applyBadge(lblStatusBadge, Color.rgb(241, 245, 249), Color.rgb(100, 116, 139));
        } else {
            UiTheme.applyBadge(lblStatusBadge, Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
        }

        lblSummaryIdAndType.setText(rec.getEmployeeId() + "  •  " + rec.getEmployeeType() + "  •  " + rec.getWorkLocation());
        lblSummaryDesig.setText("Designation: " + rec.getDesignationSnapshot() + " (" + (rec.getDepartment() != null ? rec.getDepartment() : "General") + ")");

        String payDateStr = rec.getPayDate() != null ? rec.getPayDate().toString() : "N/A";
        lblSummaryPeriodAndDate.setText("Pay Period: " + rec.getPayPeriod() + " (Pay Date: " + payDateStr + ")");

        boolean isBench = "Bench".equalsIgnoreCase(rec.getEmployeeType());
        if (!isBench) {
            lblSummaryAttendance.setText("Attendance: " + rec.getPresentDays() + " / " + rec.getTotalWorkingDays() + " days (LOP: " + rec.getAbsentDays() + ")");
        } else {
            String proj = (rec.getProjectName() != null && !rec.getProjectName().isEmpty()) ? rec.getProjectName() : "Unallocated";
            lblSummaryAttendance.setText("Project: " + proj + " (" + rec.getEligibleProjectDays() + " days @ Rs. " + CurrencyFormatter.format(rec.getBasicSalaryOrDailyRate()) + "/day)");
        }

        // 2. Audit Trail Details
        if (rec.isCorrected()) {
            auditTrailBox.setVisible(true);
            auditTrailBox.setManaged(true);
            lblAuditHeader.setText("Authorized Correction Audit Trail:");
            lblAuditOriginalId.setText("Superseded Original Record: " + rec.getOriginalRecordId());
            lblAuditReason.setText("Correction Reason: " + rec.getCorrectionReason());
            lblAuditTimestamp.setText("Audited At: " + rec.getCorrectionTimestamp());
        } else if (rec.isSuperseded()) {
            auditTrailBox.setVisible(true);
            auditTrailBox.setManaged(true);
            lblAuditHeader.setText("Historical Archival Notice:");
            lblAuditOriginalId.setText("Record Status: SUPERSEDED");
            lblAuditReason.setText("Notice: This record was archived by a subsequent authorized correction.");
            lblAuditTimestamp.setText("Original Generated: " + rec.getGenerationDate());
        } else {
            auditTrailBox.setVisible(false);
            auditTrailBox.setManaged(false);
        }

        // 3. Financial Breakdown Grid
        breakdownGrid.getChildren().clear();
        int row = 0;

        if (!isBench) {
            addGridRow("Basic Salary (Payable):", "Rs. " + CurrencyFormatter.format(rec.getPayableBasic()), row++, false);
            addGridRow("House Rent Allowance (HRA):", "Rs. " + CurrencyFormatter.format(rec.getPayableHra()), row++, false);
            addGridRow("Dearness Allowance (DA):", "Rs. " + CurrencyFormatter.format(rec.getPayableDa()), row++, false);
            addGridRow("Conveyance Allowance:", "Rs. " + CurrencyFormatter.format(rec.getPayableConveyance()), row++, false);
            if (rec.getOtherEarnings() != null && rec.getOtherEarnings().compareTo(BigDecimal.ZERO) > 0) {
                addGridRow("Other Earnings:", "Rs. " + CurrencyFormatter.format(rec.getOtherEarnings()), row++, false);
            }
        } else {
            addGridRow("Basic Pay (Days x Rate):", "Rs. " + CurrencyFormatter.format(rec.getPayableBasic()), row++, false);
            addGridRow("Allowances (HRA / DA / Conv):", "Rs. 0.00 (N/A for Bench)", row++, false);
            if (rec.getOtherEarnings() != null && rec.getOtherEarnings().compareTo(BigDecimal.ZERO) > 0) {
                addGridRow("Other Earnings:", "Rs. " + CurrencyFormatter.format(rec.getOtherEarnings()), row++, false);
            }
        }

        addGridRow("Gross Earnings:", "Rs. " + CurrencyFormatter.format(rec.getGrossEarnings()), row++, true);

        // Deductions
        addGridRow("Employee PF Contribution:", "Rs. " + CurrencyFormatter.format(rec.getPfEmployeeDeduction()), row++, false);
        addGridRow("Professional Tax (PT):", "Rs. " + CurrencyFormatter.format(rec.getProfessionalTax()), row++, false);
        if (rec.getOtherDeductions() != null && rec.getOtherDeductions().compareTo(BigDecimal.ZERO) > 0) {
            addGridRow("Other Deductions:", "Rs. " + CurrencyFormatter.format(rec.getOtherDeductions()), row++, false);
        }
        addGridRow("Total Deductions:", "Rs. " + CurrencyFormatter.format(rec.getTotalDeductions()), row++, true);

        // 4. Net Salary
        lblNetValue.setText("Rs. " + CurrencyFormatter.format(rec.getNetSalary()));
        lblNetInWords.setText("In Words: " + NumberToWordsConverter.convertToIndianCurrencyWords(rec.getNetSalary()));

        // 5. Employer PF
        lblEmployerPfValue.setText("Rs. " + CurrencyFormatter.format(rec.getPfEmployerContribution()) + " (Statutory company expense)");

        // 6. Policy Snapshot
        String policySnap = rec.getPolicySnapshot();
        lblPolicySnapshotValue.setText((policySnap != null && !policySnap.isEmpty()) ? policySnap : "Default designation policy snapshot preserved at calculation time.");

        // Disable correction button if already superseded
        btnCorrect.setDisable(rec.isSuperseded());
    }

    private void addGridRow(String labelText, String valueText, int row, boolean bold) {
        Label lbl = new Label(labelText);
        Label val = new Label(valueText);

        if (bold) {
            lbl.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
            lbl.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
            val.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
            val.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        } else {
            lbl.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            lbl.setTextFill(UiTheme.COLOR_TEXT_DARK);
            val.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
            val.setTextFill(UiTheme.COLOR_TEXT_DARK);
        }

        breakdownGrid.add(lbl, 0, row);
        breakdownGrid.add(val, 1, row);
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        filterPeriod.getSelectionModel().selectFirst();
        filterCategory.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        actionStatusLabel.setText("");
    }

    @FXML
    public void handleOpenSalarySlip() {
        PayrollRecord selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payroll record from the table first.");
            return;
        }

        File targetFile = pdfGenerator.getDefaultTargetFile(selected);
        CompanySettings company = policyRepository.getCompanySettings();

        if (targetFile.exists()) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(targetFile);
                    actionStatusLabel.setText("Opened salary slip: " + targetFile.getName());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.INFORMATION, "Salary Slip File", "PDF salary slip is located at:\n" + targetFile.getAbsolutePath());
                }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Salary Slip File", "PDF salary slip is located at:\n" + targetFile.getAbsolutePath());
            }
        } else {
            // PDF not generated yet for this historical record - offer explicit on-demand generation
            Alert confirmGen = new Alert(Alert.AlertType.CONFIRMATION);
            confirmGen.setTitle("Generate Salary Slip");
            confirmGen.setHeaderText("Salary Slip PDF Not Yet Generated");
            confirmGen.setContentText("A PDF salary slip for Employee " + selected.getEmployeeId() + " (" + selected.getPayPeriod() +
                    ") was not found in 'payslips/'.\n\nWould you like to generate and open it now?");

            Optional<ButtonType> res = confirmGen.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    File generatedPdf = pdfGenerator.generateSalarySlipPdf(selected, company, true);
                    actionStatusLabel.setText("✅ Generated: " + generatedPdf.getName());
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(generatedPdf);
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate salary slip: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    public void handleInitiateCorrection() {
        PayrollRecord selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payroll record from the table first.");
            return;
        }

        if (selected.isSuperseded()) {
            showAlert(Alert.AlertType.WARNING, "Record Superseded", "This record has already been superseded by a subsequent correction and cannot be modified again.");
            return;
        }

        // Open Authorized Correction Dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Authorized Payroll Correction");
        dialog.setHeaderText("Authorized Correction for " + selected.getEmployeeNameSnapshot() + " (" + selected.getEmployeeId() + ")\n" +
                "Pay Period: " + selected.getPayPeriod() + " | Current Net Salary: Rs. " + CurrencyFormatter.format(selected.getNetSalary()));

        boolean isBench = "Bench".equalsIgnoreCase(selected.getEmployeeType());

        // Dialog Form Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 20, 20));

        Label lblWarn = new Label("⚠️ Audit Notice: Original record " + selected.getRecordId() +
                " will be archived as SUPERSEDED. A new immutable CORRECTED record will be created.");
        lblWarn.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 11.5));
        lblWarn.setTextFill(Color.rgb(180, 83, 9));
        grid.add(lblWarn, 0, 0, 2, 1);

        Label lblDaysPrompt = new Label(isBench ? "Corrected Eligible Project Days (0 - 31):" : "Corrected Present Days (0 - " + selected.getTotalWorkingDays() + "):");
        lblDaysPrompt.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        TextField txtNewDays = new TextField(String.valueOf(isBench ? selected.getEligibleProjectDays() : selected.getPresentDays()));
        UiTheme.applyInputField(txtNewDays);
        grid.add(lblDaysPrompt, 0, 1);
        grid.add(txtNewDays, 1, 1);

        Label lblReasonPrompt = new Label("Mandatory Audit Justification / Reason:");
        lblReasonPrompt.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        TextArea txtReason = new TextArea();
        txtReason.setPromptText("Enter audit justification (e.g., HR attendance regularization approved by Manager)...");
        txtReason.setPrefRowCount(3);
        txtReason.setWrapText(true);
        grid.add(lblReasonPrompt, 0, 2);
        grid.add(txtReason, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> dialogResult = dialog.showAndWait();
        if (dialogResult.isEmpty() || dialogResult.get() != ButtonType.OK) {
            return;
        }

        // Validate reason
        String reason = txtReason.getText() != null ? txtReason.getText().trim() : "";
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Correction Rejected", "Mandatory audit justification cannot be blank. Correction aborted.");
            return;
        }

        // Validate and parse days
        int newDays;
        try {
            newDays = Integer.parseInt(txtNewDays.getText().trim());
            if (isBench) {
                if (newDays < 0 || newDays > 31) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Eligible project days must be between 0 and 31.");
                    return;
                }
            } else {
                if (newDays < 0 || newDays > selected.getTotalWorkingDays()) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Present days must be between 0 and total working days (" + selected.getTotalWorkingDays() + ").");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid numeric day count.");
            return;
        }

        // Perform Authorized Correction
        try {
            PayrollRecord correctedRecord = createCorrectedRecord(selected, newDays, isBench, reason);
            payrollRepository.saveCorrection(selected, correctedRecord);

            // Reload data and update UI
            loadPayrollData();
            applyFilterPredicate();
            historyTable.getSelectionModel().select(correctedRecord);

            actionStatusLabel.setText("✅ Correction executed: " + correctedRecord.getRecordId());
            showAlert(Alert.AlertType.INFORMATION, "Correction Success",
                    "Authorized payroll correction successfully saved.\n\n" +
                    "• Original Record: " + selected.getRecordId() + " (Archived as SUPERSEDED)\n" +
                    "• Corrected Record: " + correctedRecord.getRecordId() + "\n" +
                    "• New Net Salary: Rs. " + CurrencyFormatter.format(correctedRecord.getNetSalary()));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Correction Error", "Failed to execute authorized correction: " + e.getMessage());
        }
    }

    private PayrollRecord createCorrectedRecord(PayrollRecord orig, int newDays, boolean isBench, String reason) {
        PayrollRecord corr = new PayrollRecord();
        String corrSuffix = "-CORR" + (System.currentTimeMillis() % 10000);
        corr.setRecordId(orig.getRecordId() + corrSuffix);
        corr.setEmployeeId(orig.getEmployeeId());
        corr.setEmployeeNameSnapshot(orig.getEmployeeNameSnapshot());
        corr.setDesignationSnapshot(orig.getDesignationSnapshot());
        corr.setDepartment(orig.getDepartment());
        corr.setEmployeeType(orig.getEmployeeType());
        corr.setWorkLocation(orig.getWorkLocation());
        corr.setDateOfJoining(orig.getDateOfJoining());
        corr.setPfAccountNumber(orig.getPfAccountNumber());
        corr.setUan(orig.getUan());
        corr.setPayPeriod(orig.getPayPeriod());
        corr.setPayDate(orig.getPayDate());

        corr.setStatus("CORRECTED");
        corr.setOriginalRecordId(orig.getRecordId());
        corr.setCorrectionReason(reason);
        corr.setCorrectionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        corr.setGenerationDate(LocalDate.now());

        if (!isBench) {
            int totalDays = orig.getTotalWorkingDays();
            int presentDays = newDays;
            int absentDays = totalDays - presentDays;

            corr.setTotalWorkingDays(totalDays);
            corr.setPresentDays(presentDays);
            corr.setAbsentDays(absentDays);

            BigDecimal attendanceFactor = totalDays > 0 ?
                    new BigDecimal(presentDays).divide(new BigDecimal(totalDays), 10, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal fixedBasic = orig.getBasicSalaryOrDailyRate();
            // Estimate original fixed allowances from ratios
            BigDecimal payableBasic = fixedBasic.multiply(attendanceFactor).setScale(2, RoundingMode.HALF_UP);
            corr.setBasicSalaryOrDailyRate(fixedBasic);
            corr.setPayableBasic(payableBasic);

            // Prorate HRA, DA, Conveyance proportionally
            BigDecimal origFactor = orig.getTotalWorkingDays() > 0 && orig.getPresentDays() > 0 ?
                    new BigDecimal(orig.getPresentDays()).divide(new BigDecimal(orig.getTotalWorkingDays()), 10, RoundingMode.HALF_UP) : BigDecimal.ONE;

            BigDecimal fixedHra = origFactor.compareTo(BigDecimal.ZERO) > 0 ? orig.getPayableHra().divide(origFactor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal fixedDa = origFactor.compareTo(BigDecimal.ZERO) > 0 ? orig.getPayableDa().divide(origFactor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal fixedConv = origFactor.compareTo(BigDecimal.ZERO) > 0 ? orig.getPayableConveyance().divide(origFactor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal payableHra = fixedHra.multiply(attendanceFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payableDa = fixedDa.multiply(attendanceFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payableConv = fixedConv.multiply(attendanceFactor).setScale(2, RoundingMode.HALF_UP);

            corr.setPayableHra(payableHra);
            corr.setPayableDa(payableDa);
            corr.setPayableConveyance(payableConv);
            corr.setOtherEarnings(orig.getOtherEarnings());

            BigDecimal gross = payableBasic.add(payableHra).add(payableDa).add(payableConv).add(orig.getOtherEarnings());
            corr.setGrossEarnings(gross);

            // Recompute PF if original had PF
            if (orig.getPfEmployeeDeduction().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pfRate = orig.getBasicSalaryOrDailyRate().compareTo(BigDecimal.ZERO) > 0 ?
                        orig.getPfEmployeeDeduction().divide(orig.getPayableBasic(), 4, RoundingMode.HALF_UP) : new BigDecimal("0.12");
                BigDecimal pfEmp = payableBasic.multiply(pfRate).setScale(2, RoundingMode.HALF_UP);
                corr.setPfEmployeeDeduction(pfEmp);
                corr.setPfEmployerContribution(pfEmp);
            } else {
                corr.setPfEmployeeDeduction(BigDecimal.ZERO);
                corr.setPfEmployerContribution(BigDecimal.ZERO);
            }

            corr.setProfessionalTax(orig.getProfessionalTax());
            corr.setOtherDeductions(orig.getOtherDeductions());

            BigDecimal totalDeductions = corr.getPfEmployeeDeduction().add(corr.getProfessionalTax()).add(corr.getOtherDeductions());
            corr.setTotalDeductions(totalDeductions);
            corr.setNetSalary(gross.subtract(totalDeductions));

        } else {
            corr.setBasicSalaryOrDailyRate(orig.getBasicSalaryOrDailyRate());
            corr.setEligibleProjectDays(newDays);
            corr.setProjectName(orig.getProjectName());

            BigDecimal payableBasic = orig.getBasicSalaryOrDailyRate().multiply(new BigDecimal(newDays)).setScale(2, RoundingMode.HALF_UP);
            corr.setPayableBasic(payableBasic);
            corr.setPayableHra(BigDecimal.ZERO);
            corr.setPayableDa(BigDecimal.ZERO);
            corr.setPayableConveyance(BigDecimal.ZERO);
            corr.setOtherEarnings(orig.getOtherEarnings());

            BigDecimal gross = payableBasic.add(orig.getOtherEarnings());
            corr.setGrossEarnings(gross);

            corr.setPfEmployeeDeduction(BigDecimal.ZERO);
            corr.setPfEmployerContribution(BigDecimal.ZERO);
            corr.setProfessionalTax(orig.getProfessionalTax());
            corr.setOtherDeductions(orig.getOtherDeductions());

            BigDecimal totalDeductions = corr.getProfessionalTax().add(corr.getOtherDeductions());
            corr.setTotalDeductions(totalDeductions);
            corr.setNetSalary(gross.subtract(totalDeductions));
        }

        corr.setPolicySnapshot(orig.getPolicySnapshot() + " [Corrected: " + reason + "]");
        return corr;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

