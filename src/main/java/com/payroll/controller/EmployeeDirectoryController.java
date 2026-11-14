package com.payroll.controller;

import com.payroll.model.BenchEmployee;
import com.payroll.model.Employee;
import com.payroll.model.PermanentEmployee;
import com.payroll.service.AppContext;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PolicyRepository;
import com.payroll.util.DateFormatter;
import com.payroll.util.UiTheme;
import com.payroll.util.ValidationUtil;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Employee Directory screen.
 * Supports real-time search, multi-parameter filtering, viewing, editing, and deactivating employees.
 * Styled purely with native JavaFX APIs (No CSS, No HTML).
 */
public class EmployeeDirectoryController {

    @FXML private VBox rootContainer;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Button btnEdit;
    @FXML private Button btnDeactivate;

    @FXML private HBox toolbarCard;
    @FXML private Label lblSearch;
    @FXML private TextField searchField;
    @FXML private Label lblDept;
    @FXML private ComboBox<String> filterDepartment;
    @FXML private Label lblType;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label lblLoc;
    @FXML private ComboBox<String> filterLocation;
    @FXML private Button btnReset;
    @FXML private Label totalCountLabel;

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colCategory;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, String> colDesignation;
    @FXML private TableColumn<Employee, String> colLocation;
    @FXML private TableColumn<Employee, String> colDoj;
    @FXML private TableColumn<Employee, String> colStatus;

    private final EmployeeRepository employeeRepo;
    private final PolicyRepository policyRepo;
    private final ObservableList<Employee> employeeList = FXCollections.observableArrayList();

    public EmployeeDirectoryController() {
        this.employeeRepo = AppContext.getInstance().getEmployeeRepository();
        this.policyRepo = AppContext.getInstance().getPolicyRepository();
    }

    @FXML
    public void initialize() {
        applyPureJavaFxStyles();
        setupFilterCombos();
        setupTableColumns();
        loadEmployees();

        // Real-time search and filter listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterDepartment.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterCategory.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterLocation.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyPureJavaFxStyles() {
        if (rootContainer != null) {
            rootContainer.setBackground(new Background(new BackgroundFill(UiTheme.COLOR_BG_LIGHT, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        if (pageTitleLabel != null) UiTheme.applyPageTitle(pageTitleLabel);
        if (pageSubtitleLabel != null) UiTheme.applyPageSubtitle(pageSubtitleLabel);
        if (btnEdit != null) UiTheme.applyPrimaryButton(btnEdit);
        if (btnDeactivate != null) UiTheme.applyDangerButton(btnDeactivate);

        if (toolbarCard != null) UiTheme.applyCard(toolbarCard);

        if (lblSearch != null) UiTheme.applyFormLabel(lblSearch);
        if (lblDept != null) UiTheme.applyFormLabel(lblDept);
        if (lblType != null) UiTheme.applyFormLabel(lblType);
        if (lblLoc != null) UiTheme.applyFormLabel(lblLoc);

        if (searchField != null) UiTheme.applyInputField(searchField);
        if (filterDepartment != null) UiTheme.applyComboBox(filterDepartment);
        if (filterCategory != null) UiTheme.applyComboBox(filterCategory);
        if (filterLocation != null) UiTheme.applyComboBox(filterLocation);

        if (btnReset != null) UiTheme.applySecondaryButton(btnReset);
        if (totalCountLabel != null) UiTheme.applyBadge(totalCountLabel, Color.rgb(224, 231, 255), UiTheme.COLOR_ACTION_BLUE);
    }

    private void setupFilterCombos() {
        filterDepartment.setItems(FXCollections.observableArrayList("All", "Engineering", "Management", "Quality Assurance", "Finance", "Human Resources", "Operations"));
        filterDepartment.getSelectionModel().selectFirst();

        filterCategory.setItems(FXCollections.observableArrayList("All", "Permanent", "Bench"));
        filterCategory.getSelectionModel().selectFirst();

        filterLocation.setItems(FXCollections.observableArrayList("All", "Telangana", "Andhra Pradesh"));
        filterLocation.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmployeeId()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmployeeCategory()));
        colDepartment.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDepartment()));
        colDesignation.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesignation()));
        colLocation.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getWorkLocation()));
        colDoj.setCellValueFactory(cell -> new SimpleStringProperty(DateFormatter.formatDisplay(cell.getValue().getDateOfJoining())));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Inactive"));

        // Status column visual styling using pure JavaFX TableCell APIs
        colStatus.setCellFactory(column -> new TableCell<Employee, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
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
    }

    public void loadEmployees() {
        employeeList.clear();
        employeeList.addAll(employeeRepo.loadAll());
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedDept = filterDepartment.getValue();
        String selectedCat = filterCategory.getValue();
        String selectedLoc = filterLocation.getValue();

        List<Employee> filtered = employeeList.stream().filter(emp -> {
            // Search query matches ID or Name
            boolean matchesSearch = searchText.isEmpty() ||
                    emp.getEmployeeId().toLowerCase().contains(searchText) ||
                    emp.getFullName().toLowerCase().contains(searchText);

            // Department filter
            boolean matchesDept = selectedDept == null || "All".equalsIgnoreCase(selectedDept) ||
                    emp.getDepartment().equalsIgnoreCase(selectedDept);

            // Category filter
            boolean matchesCat = selectedCat == null || "All".equalsIgnoreCase(selectedCat) ||
                    emp.getEmployeeCategory().equalsIgnoreCase(selectedCat);

            // Location filter
            boolean matchesLoc = selectedLoc == null || "All".equalsIgnoreCase(selectedLoc) ||
                    emp.getWorkLocation().equalsIgnoreCase(selectedLoc);

            return matchesSearch && matchesDept && matchesCat && matchesLoc;
        }).toList();

        employeeTable.setItems(FXCollections.observableArrayList(filtered));
        totalCountLabel.setText("Showing " + filtered.size() + " of " + employeeList.size() + " Employees");
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        filterDepartment.getSelectionModel().selectFirst();
        filterCategory.getSelectionModel().selectFirst();
        filterLocation.getSelectionModel().selectFirst();
        applyFilters();
    }

    @FXML
    public void handleDeactivateEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an employee to deactivate.");
            return;
        }

        if (!selected.isActive()) {
            showAlert(Alert.AlertType.INFORMATION, "Already Inactive", "Employee " + selected.getFullName() + " is already inactive.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deactivation");
        confirm.setHeaderText("Deactivate Employee: " + selected.getFullName() + " (" + selected.getEmployeeId() + ")");
        confirm.setContentText("Deactivating will prevent this employee from being included in new monthly payroll processing.\n\nHistorical payroll records will remain untouched.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                employeeRepo.deactivate(selected.getEmployeeId());
                loadEmployees();
                showAlert(Alert.AlertType.INFORMATION, "Deactivation Successful", "Employee " + selected.getEmployeeId() + " has been marked inactive.");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Update Error", "Failed to deactivate employee:\n" + e.getMessage());
            }
        }
    }

    @FXML
    public void handleEditEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an employee to edit.");
            return;
        }

        // Build Edit Dialog using pure JavaFX components
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee Details - " + selected.getEmployeeId());
        dialog.setHeaderText("Update contact and employment details for " + selected.getFullName() + " (" + selected.getEmployeeCategory() + ")");

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField(selected.getEmail());
        TextField mobileField = new TextField(selected.getMobileNumber());
        TextArea addressField = new TextArea(selected.getAddress());
        addressField.setPrefRowCount(2);

        ComboBox<String> maritalCombo = new ComboBox<>(FXCollections.observableArrayList("Single", "Married", "Divorced", "Widowed"));
        maritalCombo.setValue(selected.getMaritalStatus());

        ComboBox<String> deptCombo = new ComboBox<>(FXCollections.observableArrayList("Engineering", "Management", "Quality Assurance", "Finance", "Human Resources", "Operations"));
        deptCombo.setValue(selected.getDepartment());

        ComboBox<String> locCombo = new ComboBox<>(FXCollections.observableArrayList("Telangana", "Andhra Pradesh"));
        locCombo.setValue(selected.getWorkLocation());

        CheckBox activeBox = new CheckBox("Active Status");
        activeBox.setSelected(selected.isActive());

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Mobile:"), 0, 1);
        grid.add(mobileField, 1, 1);
        grid.add(new Label("Marital Status:"), 0, 2);
        grid.add(maritalCombo, 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(deptCombo, 1, 3);
        grid.add(new Label("Work Location:"), 0, 4);
        grid.add(locCombo, 1, 4);
        grid.add(new Label("Address:"), 0, 5);
        grid.add(addressField, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(activeBox, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String newEmail = emailField.getText().trim();
                String newMobile = mobileField.getText().trim();

                if (!ValidationUtil.isValidEmail(newEmail)) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid Email address format.");
                    return null;
                }
                if (!ValidationUtil.isValidMobile(newMobile)) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Mobile number must be exactly 10 digits.");
                    return null;
                }

                // Create updated instance preserving immutable snapshots & original ID
                if ("Permanent".equalsIgnoreCase(selected.getEmployeeCategory())) {
                    return new PermanentEmployee(
                            selected.getEmployeeId(), selected.getFirstName(), selected.getLastName(),
                            selected.getDateOfBirth(), newEmail, newMobile,
                            maritalCombo.getValue(), addressField.getText().trim(), deptCombo.getValue(),
                            selected.getDesignation(), locCombo.getValue(), selected.getDateOfJoining(),
                            activeBox.isSelected(), selected.getPfAccountNumber(), selected.getUan(),
                            selected.isPfApplicable(), selected.isMedicalCondition(), selected.getMedicalDescription()
                    );
                } else {
                    return new BenchEmployee(
                            selected.getEmployeeId(), selected.getFirstName(), selected.getLastName(),
                            selected.getDateOfBirth(), newEmail, newMobile,
                            maritalCombo.getValue(), addressField.getText().trim(), deptCombo.getValue(),
                            selected.getDesignation(), locCombo.getValue(), selected.getDateOfJoining(),
                            activeBox.isSelected(), selected.getPfAccountNumber(), selected.getUan(),
                            selected.isPfApplicable(), selected.isMedicalCondition(), selected.getMedicalDescription()
                    );
                }
            }
            return null;
        });

        Optional<Employee> updated = dialog.showAndWait();
        updated.ifPresent(emp -> {
            try {
                employeeRepo.update(emp);
                loadEmployees();
                showAlert(Alert.AlertType.INFORMATION, "Update Successful", "Employee " + emp.getEmployeeId() + " successfully updated.");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Update Error", "Failed to update employee in CSV:\n" + e.getMessage());
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
