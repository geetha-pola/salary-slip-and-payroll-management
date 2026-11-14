package com.payroll.controller;

import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.service.AppContext;
import com.payroll.service.PdfSalarySlipGenerator;
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
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for the Salary Slip Generation and PDF Export screen.
 * Allows administrators to search and filter saved payroll records, inspect full
 * pro-rated breakdowns, preview Indian Rupee words, and generate audit-ready PDF payslips.
 * 
 * Strict Hackathon Compliance:
 * - Pure JavaFX + FXML + Java Controllers (Strictly NO CSS, NO HTML, NO JavaScript, NO WebViews).
 * - All styling rendered via native JavaFX APIs and UiTheme.
 */
public class SalarySlipController {

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
    @FXML private Button btnReset;
    @FXML private Label totalCountLabel;

    // Table & Batch Actions
    @FXML private TableView<PayrollRecord> payrollTable;
    @FXML private TableColumn<PayrollRecord, String> colRecordId;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeId;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeName;
    @FXML private TableColumn<PayrollRecord, String> colCategory;
    @FXML private TableColumn<PayrollRecord, String> colPayPeriod;
    @FXML private TableColumn<PayrollRecord, String> colGross;
    @FXML private TableColumn<PayrollRecord, String> colDeductions;
    @FXML private TableColumn<PayrollRecord, String> colNet;
    @FXML private TableColumn<PayrollRecord, String> colGeneratedDate;
    @FXML private Label tablePlaceholderLabel;

    @FXML private Button btnBatchGenerate;
    @FXML private Button btnOpenFolder;
    @FXML private Label batchStatusLabel;

    // Preview Card
    @FXML private VBox previewCard;
    @FXML private Label previewTitleLabel;
    @FXML private VBox noSelectionBox;
    @FXML private Label lblNoSelectionIcon;
    @FXML private Label lblNoSelectionText;

    @FXML private ScrollPane previewScrollPane;
    @FXML private VBox empSummaryBox;
    @FXML private Label lblSummaryName;
    @FXML private Label lblSummaryIdAndType;
    @FXML private Label lblSummaryDesig;
    @FXML private Label lblSummaryPeriodAndDate;
    @FXML private Label lblSummaryAttendance;

    @FXML private Label lblBreakdownHeader;
    @FXML private GridPane breakdownGrid;

    @FXML private VBox netSalaryCard;
    @FXML private Label lblNetTitle;
    @FXML private Label lblNetValue;
    @FXML private Label lblNetInWords;

    @FXML private VBox employerPfCard;
    @FXML private Label lblEmployerPfTitle;
    @FXML private Label lblEmployerPfValue;

    @FXML private Label lblPrivacyNotice;
    @FXML private Button btnGenerateOpen;
    @FXML private Button btnSaveAs;
    @FXML private Label actionStatusLabel;

    private final PayrollRepository payrollRepository;
    private final PolicyRepository policyRepository;
    private final PdfSalarySlipGenerator pdfGenerator;

    private final ObservableList<PayrollRecord> masterPayrollData = FXCollections.observableArrayList();
    private FilteredList<PayrollRecord> filteredPayrollData;

    public SalarySlipController() {
        AppContext context = AppContext.getInstance();
        this.payrollRepository = context.getPayrollRepository();
        this.policyRepository = context.getPolicyRepository();
        this.pdfGenerator = context.getPdfSalarySlipGenerator();
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

        // Toolbar styling
        UiTheme.applyCard(toolbarCard);
        UiTheme.applyFormLabel(lblSearch);
        UiTheme.applyInputField(searchField);
        UiTheme.applyFormLabel(lblPeriod);
        UiTheme.applyComboBox(filterPeriod);
        UiTheme.applyFormLabel(lblCategory);
        UiTheme.applyComboBox(filterCategory);
        UiTheme.applySecondaryButton(btnReset);

        totalCountLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
        totalCountLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        // Action Buttons
        UiTheme.applySecondaryButton(btnBatchGenerate);
        UiTheme.applySecondaryButton(btnOpenFolder);
        batchStatusLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        batchStatusLabel.setTextFill(UiTheme.COLOR_SUCCESS_GREEN);

        // Preview Card styling
        UiTheme.applyCard(previewCard);
        previewTitleLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 16));
        previewTitleLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

        lblNoSelectionIcon.setFont(Font.font(UiTheme.FONT_FAMILY, 36));
        lblNoSelectionText.setFont(Font.font(UiTheme.FONT_FAMILY, 13));
        lblNoSelectionText.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        // Summary details
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

        // Privacy Notice
        lblPrivacyNotice.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
        lblPrivacyNotice.setTextFill(UiTheme.COLOR_MUTED_GRAY);

        // Preview Actions
        UiTheme.applyPrimaryButton(btnGenerateOpen);
        UiTheme.applySecondaryButton(btnSaveAs);

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

        colGeneratedDate.setCellValueFactory(data -> new SimpleStringProperty(
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

        // Populate Category filter options
        filterCategory.setItems(FXCollections.observableArrayList("All Categories", "Permanent", "Bench"));
        filterCategory.getSelectionModel().selectFirst();
    }

    private void setupFilters() {
        filteredPayrollData = new FilteredList<>(masterPayrollData, p -> true);

        // Bind filter predicates to controls
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());
        filterPeriod.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());
        filterCategory.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterPredicate());

        SortedList<PayrollRecord> sortedData = new SortedList<>(filteredPayrollData);
        sortedData.comparatorProperty().bind(payrollTable.comparatorProperty());
        payrollTable.setItems(sortedData);

        updateRecordCount();
    }

    private void applyFilterPredicate() {
        String searchText = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String selectedPeriod = filterPeriod.getValue();
        String selectedCategory = filterCategory.getValue();

        filteredPayrollData.setPredicate(rec -> {
            if (rec == null) return false;
            // Superseded records cannot accidentally be treated as current payroll
            if (rec.isSuperseded() || "SUPERSEDED".equalsIgnoreCase(rec.getStatus())) {
                return false;
            }

            // Search filter
            if (!searchText.isEmpty()) {
                boolean matchId = rec.getEmployeeId() != null && rec.getEmployeeId().toLowerCase().contains(searchText);
                boolean matchName = rec.getEmployeeNameSnapshot() != null && rec.getEmployeeNameSnapshot().toLowerCase().contains(searchText);
                boolean matchRecId = rec.getRecordId() != null && rec.getRecordId().toLowerCase().contains(searchText);
                if (!matchId && !matchName && !matchRecId) {
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

            return true;
        });

        updateRecordCount();
    }

    private void updateRecordCount() {
        int count = filteredPayrollData != null ? filteredPayrollData.size() : masterPayrollData.size();
        totalCountLabel.setText(count + " Record" + (count == 1 ? "" : "s") + " Available");
    }

    private void setupSelectionListener() {
        payrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
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

        // 1. Employee Identification
        lblSummaryName.setText(rec.getEmployeeNameSnapshot());
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

        // 2. Breakdown Grid
        breakdownGrid.getChildren().clear();
        int row = 0;

        if (!isBench) {
            addGridRow("Basic Salary (Payable):", "Rs. " + CurrencyFormatter.format(rec.getPayableBasic()), row++, false);
            addGridRow("House Rent Allowance (HRA):", "Rs. " + CurrencyFormatter.format(rec.getPayableHra()), row++, false);
            addGridRow("Dearness Allowance (DA):", "Rs. " + CurrencyFormatter.format(rec.getPayableDa()), row++, false);
            addGridRow("Conveyance Allowance:", "Rs. " + CurrencyFormatter.format(rec.getPayableConveyance()), row++, false);
            if (rec.getOtherEarnings() != null && rec.getOtherEarnings().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addGridRow("Other Earnings:", "Rs. " + CurrencyFormatter.format(rec.getOtherEarnings()), row++, false);
            }
        } else {
            addGridRow("Basic Pay (Days x Rate):", "Rs. " + CurrencyFormatter.format(rec.getPayableBasic()), row++, false);
            addGridRow("Allowances (HRA / DA / Conv):", "Rs. 0.00 (N/A for Bench)", row++, false);
            if (rec.getOtherEarnings() != null && rec.getOtherEarnings().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addGridRow("Other Earnings:", "Rs. " + CurrencyFormatter.format(rec.getOtherEarnings()), row++, false);
            }
        }

        // Gross total row
        addGridRow("Gross Earnings:", "Rs. " + CurrencyFormatter.format(rec.getGrossEarnings()), row++, true);

        // Deductions
        addGridRow("Employee PF Contribution:", "Rs. " + CurrencyFormatter.format(rec.getPfEmployeeDeduction()), row++, false);
        addGridRow("Professional Tax (PT):", "Rs. " + CurrencyFormatter.format(rec.getProfessionalTax()), row++, false);
        if (rec.getOtherDeductions() != null && rec.getOtherDeductions().compareTo(java.math.BigDecimal.ZERO) > 0) {
            addGridRow("Other Deductions:", "Rs. " + CurrencyFormatter.format(rec.getOtherDeductions()), row++, false);
        }
        addGridRow("Total Deductions:", "Rs. " + CurrencyFormatter.format(rec.getTotalDeductions()), row++, true);

        // 3. Net Salary & In Words
        lblNetValue.setText("Rs. " + CurrencyFormatter.format(rec.getNetSalary()));
        lblNetInWords.setText("In Words: " + NumberToWordsConverter.convertToIndianCurrencyWords(rec.getNetSalary()));

        // 4. Employer PF
        lblEmployerPfValue.setText("Rs. " + CurrencyFormatter.format(rec.getPfEmployerContribution()) + " (Statutory company expense)");
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
        batchStatusLabel.setText("");
        actionStatusLabel.setText("");
    }

    @FXML
    public void handleGenerateAndOpen() {
        PayrollRecord selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payroll record from the table first.");
            return;
        }

        CompanySettings company = policyRepository.getCompanySettings();
        File targetFile = pdfGenerator.getDefaultTargetFile(selected);

        // Overwrite protection
        if (targetFile.exists()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("File Already Exists");
            confirmAlert.setHeaderText("A salary slip PDF already exists for this record:");
            confirmAlert.setContentText(targetFile.getName() + "\n\nDo you want to overwrite the existing file?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                actionStatusLabel.setText("Operation cancelled. File not overwritten.");
                return;
            }
        }

        try {
            File pdf = pdfGenerator.generateSalarySlipPdf(selected, company, targetFile, true);
            actionStatusLabel.setText("✅ Generated: " + pdf.getName());

            // Open in desktop default viewer if supported
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(pdf);
                } catch (Exception openEx) {
                    System.err.println("Could not auto-open PDF in viewer: " + openEx.getMessage());
                }
            }
        } catch (Exception e) {
            actionStatusLabel.setText("❌ Generation failed: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "PDF Generation Error", "Failed to generate salary slip: " + e.getMessage());
        }
    }

    @FXML
    public void handleSaveAs() {
        PayrollRecord selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payroll record from the table first.");
            return;
        }

        CompanySettings company = policyRepository.getCompanySettings();
        String safePeriod = selected.getPayPeriod().replace("/", "-").trim();
        String defaultName = "SalarySlip_" + selected.getEmployeeId() + "_" + safePeriod + ".pdf";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Salary Slip PDF As");
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        File defaultDir = pdfGenerator.getOutputDir().toFile();
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File chosenFile = fileChooser.showSaveDialog(rootContainer.getScene().getWindow());
        if (chosenFile != null) {
            try {
                pdfGenerator.generateSalarySlipPdf(selected, company, chosenFile, true);
                actionStatusLabel.setText("✅ Saved to: " + chosenFile.getName());
                showAlert(Alert.AlertType.INFORMATION, "File Saved", "Salary slip successfully saved to:\n" + chosenFile.getAbsolutePath());
            } catch (Exception e) {
                actionStatusLabel.setText("❌ Save failed: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save salary slip PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleOpenFolder() {
        File dir = pdfGenerator.getOutputDir().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(dir);
                batchStatusLabel.setText("Opened folder: " + dir.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.INFORMATION, "Payslips Directory", "Payslips directory path:\n" + dir.getAbsolutePath());
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Payslips Directory", "Payslips directory path:\n" + dir.getAbsolutePath());
        }
    }

    @FXML
    public void handleBatchGenerate() {
        ObservableList<PayrollRecord> recordsToGenerate = payrollTable.getItems();
        if (recordsToGenerate == null || recordsToGenerate.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Records", "No payroll records are available for the current filter criteria.");
            return;
        }

        int total = recordsToGenerate.size();
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Batch PDF Generation");
        confirmAlert.setHeaderText("Generate " + total + " Salary Slip PDFs?");
        confirmAlert.setContentText("All generated PDF files will be saved into the '" + pdfGenerator.getOutputDir().toString() + "' directory.\nExisting files will be refreshed.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        CompanySettings company = policyRepository.getCompanySettings();
        int successCount = 0;
        int failCount = 0;

        for (PayrollRecord rec : recordsToGenerate) {
            try {
                pdfGenerator.generateSalarySlipPdf(rec, company, true);
                successCount++;
            } catch (Exception e) {
                System.err.println("Error generating PDF for record " + rec.getRecordId() + ": " + e.getMessage());
                failCount++;
            }
        }

        batchStatusLabel.setText("Batch complete: " + successCount + " generated, " + failCount + " failed.");
        showAlert(Alert.AlertType.INFORMATION, "Batch Complete", "Successfully generated " + successCount + " PDF salary slips in '" + pdfGenerator.getOutputDir().toString() + "'.");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

