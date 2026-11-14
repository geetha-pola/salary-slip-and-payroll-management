package com.payroll.controller;

import com.payroll.model.Employee;
import com.payroll.model.PayrollRecord;
import com.payroll.service.AppContext;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
import com.payroll.util.CurrencyFormatter;
import com.payroll.util.UiTheme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Corporate Executive Dashboard.
 * Displays live, dynamically computed statistics from persistent CSV records.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class DashboardController {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox viewContainer;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private Button btnRefresh;

    @FXML
    private Button btnSeedDemo;

    @FXML
    private Label demoStatusLabel;

    // Stat Cards
    @FXML
    private VBox cardActiveEmp;

    @FXML
    private VBox cardPermEmp;

    @FXML
    private VBox cardBenchEmp;

    @FXML
    private VBox cardMonthlyPay;

    @FXML
    private VBox cardEmpPf;

    @FXML
    private VBox cardEmployerPf;

    @FXML
    private VBox cardHistorical;

    @FXML
    private VBox cardOperationalHubs;

    @FXML
    private Label totalActiveEmployeesLabel;

    @FXML
    private Label totalPermanentLabel;

    @FXML
    private Label totalBenchLabel;

    @FXML
    private Label currentMonthPayrollLabel;

    @FXML
    private Label totalEmployeePfLabel;

    @FXML
    private Label totalEmployerPfLabel;

    @FXML
    private Label totalPayrollRecordsLabel;

    // Tables & Lower Section
    @FXML
    private VBox recentTableCard;

    @FXML
    private Label recentTableTitle;

    @FXML
    private TableView<PayrollRecord> recentPayrollTable;

    @FXML
    private TableColumn<PayrollRecord, String> colRecordId;

    @FXML
    private TableColumn<PayrollRecord, String> colEmpName;

    @FXML
    private TableColumn<PayrollRecord, String> colDesignation;

    @FXML
    private TableColumn<PayrollRecord, String> colPeriod;

    @FXML
    private TableColumn<PayrollRecord, String> colGross;

    @FXML
    private TableColumn<PayrollRecord, String> colNet;

    @FXML
    private VBox deptDistributionCard;

    @FXML
    private Label deptTitleLabel;

    @FXML
    private VBox deptDistributionBox;

    private final EmployeeRepository employeeRepo;
    private final PayrollRepository payrollRepo;

    public DashboardController() {
        this(AppContext.getInstance().getEmployeeRepository(), AppContext.getInstance().getPayrollRepository());
    }

    public DashboardController(EmployeeRepository employeeRepo, PayrollRepository payrollRepo) {
        this.employeeRepo = employeeRepo;
        this.payrollRepo = payrollRepo;
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupTableColumns();
        refreshDashboard();
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
        if (btnRefresh != null) UiTheme.applySecondaryButton(btnRefresh);
        if (btnSeedDemo != null) UiTheme.applyPrimaryButton(btnSeedDemo);
        if (demoStatusLabel != null) UiTheme.applySuccessLabel(demoStatusLabel);

        // Apply Stat Cards
        styleStatCardNode(cardActiveEmp, false);
        styleStatCardNode(cardPermEmp, false);
        styleStatCardNode(cardBenchEmp, false);
        styleStatCardNode(cardMonthlyPay, true);
        styleStatCardNode(cardEmpPf, false);
        styleStatCardNode(cardEmployerPf, false);
        styleStatCardNode(cardHistorical, false);
        styleStatCardNode(cardOperationalHubs, false);

        if (recentTableCard != null) UiTheme.applyCard(recentTableCard);
        if (recentTableTitle != null) UiTheme.applySectionHeader(recentTableTitle);

        if (deptDistributionCard != null) UiTheme.applyCard(deptDistributionCard);
        if (deptTitleLabel != null) UiTheme.applySectionHeader(deptTitleLabel);
    }

    private void styleStatCardNode(VBox card, boolean highlight) {
        if (card == null) return;
        UiTheme.applyStatCard(card, highlight);

        List<Node> children = card.getChildren();
        if (children.size() >= 3) {
            if (children.get(0) instanceof Label lbl0) {
                UiTheme.applyStatTitle(lbl0);
            }
            if (children.get(1) instanceof Label lbl1) {
                UiTheme.applyStatValue(lbl1);
                if (highlight) {
                    lbl1.setTextFill(UiTheme.COLOR_ACTION_BLUE);
                }
            }
            if (children.get(2) instanceof Label lbl2) {
                UiTheme.applyStatSubtext(lbl2);
            }
        }
    }

    private void setupTableColumns() {
        colRecordId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecordId()));
        colEmpName.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmployeeNameSnapshot() + " (" + data.getValue().getEmployeeId() + ")"
        ));
        colDesignation.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDesignationSnapshot() + " [" + data.getValue().getEmployeeType() + "]"
        ));
        colPeriod.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPayPeriod()));
        colGross.setCellValueFactory(data -> new SimpleStringProperty(
                CurrencyFormatter.formatRupees(data.getValue().getGrossEarnings())
        ));
        colNet.setCellValueFactory(data -> new SimpleStringProperty(
                CurrencyFormatter.formatRupees(data.getValue().getNetSalary())
        ));
    }

    public void refreshDashboard() {
        List<Employee> allEmployees = employeeRepo.loadAll();
        List<PayrollRecord> allPayroll = payrollRepo.loadAll();

        // 1. Employee statistics
        long activeCount = allEmployees.stream().filter(Employee::isActive).count();
        long permCount = allEmployees.stream().filter(e -> e.isActive() && "Permanent".equalsIgnoreCase(e.getEmployeeCategory())).count();
        long benchCount = allEmployees.stream().filter(e -> e.isActive() && "Bench".equalsIgnoreCase(e.getEmployeeCategory())).count();

        totalActiveEmployeesLabel.setText(String.valueOf(activeCount));
        totalPermanentLabel.setText(String.valueOf(permCount));
        totalBenchLabel.setText(String.valueOf(benchCount));

        // 2. Financial statistics from payroll repository (Display 'Not Available Yet' until payroll records exist)
        if (allPayroll.isEmpty()) {
            currentMonthPayrollLabel.setText("Not Available Yet");
            currentMonthPayrollLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 15));
            currentMonthPayrollLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

            totalEmployeePfLabel.setText("Not Available Yet");
            totalEmployeePfLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 15));
            totalEmployeePfLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

            totalEmployerPfLabel.setText("Not Available Yet");
            totalEmployerPfLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 15));
            totalEmployerPfLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

            totalPayrollRecordsLabel.setText("Not Available Yet");
            totalPayrollRecordsLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 15));
            totalPayrollRecordsLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
        } else {
            String p1 = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String p2 = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));
            String p3 = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy"));

            // Filter active non-superseded records for consistent financial aggregations
            List<PayrollRecord> activeRecords = allPayroll.stream()
                    .filter(p -> !p.isSuperseded())
                    .toList();

            BigDecimal currentMonthDisbursed = activeRecords.stream()
                    .filter(p -> p1.equalsIgnoreCase(p.getPayPeriod()) || p2.equalsIgnoreCase(p.getPayPeriod()) || p3.equalsIgnoreCase(p.getPayPeriod()))
                    .map(PayrollRecord::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            currentMonthPayrollLabel.setText(CurrencyFormatter.formatRupees(currentMonthDisbursed));
            currentMonthPayrollLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 22));
            currentMonthPayrollLabel.setTextFill(UiTheme.COLOR_ACTION_BLUE);

            BigDecimal totalEmployeePf = activeRecords.stream()
                    .map(PayrollRecord::getPfEmployeeDeduction)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalEmployeePfLabel.setText(CurrencyFormatter.formatRupees(totalEmployeePf));
            totalEmployeePfLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 22));
            totalEmployeePfLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

            BigDecimal totalEmployerPf = activeRecords.stream()
                    .map(PayrollRecord::getPfEmployerContribution)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalEmployerPfLabel.setText(CurrencyFormatter.formatRupees(totalEmployerPf));
            totalEmployerPfLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 22));
            totalEmployerPfLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

            totalPayrollRecordsLabel.setText(String.valueOf(activeRecords.size()));
            totalPayrollRecordsLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 22));
            totalPayrollRecordsLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);
        }

        // 3. Populate Recent Activity Table (latest 10)
        List<PayrollRecord> recentList = new ArrayList<>(allPayroll);
        Collections.reverse(recentList);
        if (recentList.size() > 10) {
            recentList = recentList.subList(0, 10);
        }
        recentPayrollTable.setItems(FXCollections.observableArrayList(recentList));

        // 4. Populate Department Distribution
        updateDepartmentDistribution(allEmployees, activeCount);
    }

    private void updateDepartmentDistribution(List<Employee> allEmployees, long activeCount) {
        deptDistributionBox.getChildren().clear();

        if (activeCount == 0) {
            Label emptyLabel = new Label("No active employees registered yet.");
            emptyLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 12));
            emptyLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
            deptDistributionBox.getChildren().add(emptyLabel);
            return;
        }

        Map<String, Long> deptCounts = allEmployees.stream()
                .filter(Employee::isActive)
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        for (Map.Entry<String, Long> entry : deptCounts.entrySet()) {
            VBox itemBox = new VBox(4);
            HBox header = new HBox();
            Label nameLabel = new Label(entry.getKey());
            nameLabel.setFont(Font.font(UiTheme.FONT_FAMILY, FontWeight.BOLD, 12));
            nameLabel.setTextFill(UiTheme.COLOR_PRIMARY_NAVY);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label countLabel = new Label(entry.getValue() + " (" + (entry.getValue() * 100 / activeCount) + "%)");
            countLabel.setFont(Font.font(UiTheme.FONT_FAMILY, 11));
            countLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);

            header.getChildren().addAll(nameLabel, spacer, countLabel);

            ProgressBar bar = new ProgressBar((double) entry.getValue() / activeCount);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(8);

            itemBox.getChildren().addAll(header, bar);
            deptDistributionBox.getChildren().add(itemBox);
        }
    }

    @FXML
    public void handleSeedDemoData() {
        if (!employeeRepo.loadAll().isEmpty()) {
            demoStatusLabel.setText("Notice: Employees already exist. Will not duplicate records.");
            demoStatusLabel.setTextFill(UiTheme.COLOR_MUTED_GRAY);
            return;
        }

        employeeRepo.seedSampleEmployees();
        demoStatusLabel.setText("Successfully populated 4 demonstration employees (2 Permanent, 2 Bench).");
        demoStatusLabel.setTextFill(UiTheme.COLOR_SUCCESS_GREEN);
        refreshDashboard();
    }
}
