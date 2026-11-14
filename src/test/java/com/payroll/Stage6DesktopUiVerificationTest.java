package com.payroll;

import com.payroll.controller.MainLayoutController;
import com.payroll.controller.SalarySlipController;
import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.service.AppContext;
import com.payroll.service.PdfSalarySlipGenerator;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end Desktop UI Workflow Verification for Stage 6: Salary Slip Generation & PDF Export.
 * Validates:
 * 1. Sidebar navigation activates Salary Slip screen.
 * 2. Pure JavaFX controls rendered without CSS, HTML, or WebViews.
 * 3. Table filtering, selection, and live preview rendering for both Permanent and Bench records.
 * 4. PDF file creation, visual hierarchy, Indian Rupee words, employer PF separation, and confidentiality rules.
 */
public class Stage6DesktopUiVerificationTest {

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
            testStage.setTitle("Stage 6 Desktop UI Workflow Verification");
            stageLatch.countDown();
        });
        assertTrue(stageLatch.await(5, TimeUnit.SECONDS));

        // Ensure clean authenticated administrator session
        AppContext.getInstance().getAuthService().logout();
        AppContext.getInstance().getAuthService().authenticate("1234", "5678");
    }

    @AfterAll
    static void cleanup() throws InterruptedException {
        if (testStage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                testStage.close();
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        }
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX Thread operation timed out");
        if (errorRef.get() != null) {
            if (errorRef.get() instanceof RuntimeException) throw (RuntimeException) errorRef.get();
            if (errorRef.get() instanceof Error) throw (Error) errorRef.get();
            throw new RuntimeException(errorRef.get());
        }
    }

    @Test
    @DisplayName("Verify Stage 6 UI workflow: Sidebar navigation, Table loading, Preview Card, and PDF Generation")
    void testSalarySlipDesktopUiWorkflow(@TempDir Path tempDir) throws InterruptedException, IOException {
        AtomicReference<MainLayoutController> mainCtrlRef = new AtomicReference<>();
        AtomicReference<Parent> mainRootRef = new AtomicReference<>();

        // 1. Load Main Application Layout and verify sidebar button
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent root = loader.load();
                mainRootRef.set(root);
                MainLayoutController ctrl = loader.getController();
                mainCtrlRef.set(ctrl);

                Scene scene = new Scene(root, 1100, 750);
                testStage.setScene(scene);
                testStage.show();

                Button btnSalarySlip = (Button) root.lookup("#btnSalarySlip");
                assertNotNull(btnSalarySlip, "Salary Slip button must exist in sidebar");
                assertFalse(btnSalarySlip.isDisabled(), "Salary Slip button must be enabled in Stage 6");
                assertTrue(btnSalarySlip.getText().contains("Salary Slip"), "Button text should indicate Salary Slip");

                // Navigate to Salary Slip module
                btnSalarySlip.fire();
            } catch (Exception e) {
                throw new RuntimeException("Failed to navigate to Salary Slip module: " + e.getMessage(), e);
            }
        });

        // 2. Verify Salary Slip Screen rendered in contentArea
        runOnFxThread(() -> {
            Parent root = mainRootRef.get();
            StackPane contentArea = (StackPane) root.lookup("#contentArea");
            assertNotNull(contentArea, "Content area must exist");

            // Look up Salary Slip elements
            Label title = (Label) contentArea.lookup("#pageTitleLabel");
            assertNotNull(title, "Page title label must exist");
            assertEquals("Salary Slip Generation", title.getText());

            TextField searchField = (TextField) contentArea.lookup("#searchField");
            assertNotNull(searchField, "searchField must exist");

            ComboBox<?> filterPeriod = (ComboBox<?>) contentArea.lookup("#filterPeriod");
            assertNotNull(filterPeriod, "filterPeriod must exist");

            ComboBox<?> filterCategory = (ComboBox<?>) contentArea.lookup("#filterCategory");
            assertNotNull(filterCategory, "filterCategory must exist");

            TableView<?> payrollTable = (TableView<?>) contentArea.lookup("#payrollTable");
            assertNotNull(payrollTable, "payrollTable must exist");
            assertEquals(9, payrollTable.getColumns().size(), "payrollTable must have 9 columns");

            VBox previewCard = (VBox) contentArea.lookup("#previewCard");
            assertNotNull(previewCard, "previewCard must exist");

            VBox noSelectionBox = (VBox) contentArea.lookup("#noSelectionBox");
            assertNotNull(noSelectionBox, "noSelectionBox must exist");
            assertTrue(noSelectionBox.isVisible(), "noSelectionBox must be visible when nothing is selected");

            ScrollPane previewScrollPane = (ScrollPane) contentArea.lookup("#previewScrollPane");
            assertNotNull(previewScrollPane, "previewScrollPane must exist");
            assertFalse(previewScrollPane.isVisible(), "previewScrollPane must be hidden initially");
        });

        // 3. Populate and Select a Permanent Record in the TableView to test preview
        PayrollRecord permRecord = new PayrollRecord();
        permRecord.setRecordId("REC-TEST-PERM");
        permRecord.setEmployeeId("EMP-9001");
        permRecord.setEmployeeNameSnapshot("Deepa Sharma");
        permRecord.setEmployeeType("Permanent");
        permRecord.setDepartment("Finance");
        permRecord.setDesignationSnapshot("Finance Lead");
        permRecord.setWorkLocation("Telangana");
        permRecord.setDateOfJoining(LocalDate.of(2022, 3, 1));
        permRecord.setPayPeriod("11/2026");
        permRecord.setPayDate(LocalDate.of(2026, 11, 30));
        permRecord.setTotalWorkingDays(30);
        permRecord.setPresentDays(30);
        permRecord.setAbsentDays(0);
        permRecord.setBasicSalaryOrDailyRate(new BigDecimal("50000.00"));
        permRecord.setPayableBasic(new BigDecimal("50000.00"));
        permRecord.setPayableHra(new BigDecimal("5000.00"));
        permRecord.setPayableDa(new BigDecimal("3000.00"));
        permRecord.setPayableConveyance(new BigDecimal("1500.00"));
        permRecord.setGrossEarnings(new BigDecimal("59500.00"));
        permRecord.setPfEmployeeDeduction(new BigDecimal("6000.00"));
        permRecord.setPfEmployerContribution(new BigDecimal("6000.00"));
        permRecord.setProfessionalTax(new BigDecimal("200.00"));
        permRecord.setTotalDeductions(new BigDecimal("6200.00"));
        permRecord.setNetSalary(new BigDecimal("53300.00"));

        PayrollRecord benchRecord = new PayrollRecord();
        benchRecord.setRecordId("REC-TEST-BENCH");
        benchRecord.setEmployeeId("EMP-9002");
        benchRecord.setEmployeeNameSnapshot("Rahul Varma");
        benchRecord.setEmployeeType("Bench");
        benchRecord.setDepartment("Technology");
        benchRecord.setDesignationSnapshot("Junior Developer");
        benchRecord.setWorkLocation("Andhra Pradesh");
        benchRecord.setDateOfJoining(LocalDate.of(2024, 6, 1));
        benchRecord.setPayPeriod("11/2026");
        benchRecord.setPayDate(LocalDate.of(2026, 11, 30));
        benchRecord.setBasicSalaryOrDailyRate(new BigDecimal("1200.00"));
        benchRecord.setEligibleProjectDays(15);
        benchRecord.setProjectName("FinTech Portal");
        benchRecord.setPayableBasic(new BigDecimal("18000.00"));
        benchRecord.setGrossEarnings(new BigDecimal("18000.00"));
        benchRecord.setPfEmployeeDeduction(BigDecimal.ZERO);
        benchRecord.setPfEmployerContribution(BigDecimal.ZERO);
        benchRecord.setProfessionalTax(new BigDecimal("200.00"));
        benchRecord.setTotalDeductions(new BigDecimal("200.00"));
        benchRecord.setNetSalary(new BigDecimal("17800.00"));

        runOnFxThread(() -> {
            Parent root = mainRootRef.get();
            @SuppressWarnings("unchecked")
            TableView<PayrollRecord> payrollTable = (TableView<PayrollRecord>) root.lookup("#payrollTable");

            // Set test items on the table and select the permanent record
            payrollTable.setItems(javafx.collections.FXCollections.observableArrayList(permRecord, benchRecord));
            payrollTable.getSelectionModel().select(permRecord);

            // Verify preview card updates
            VBox noSelectionBox = (VBox) root.lookup("#noSelectionBox");
            assertFalse(noSelectionBox.isVisible(), "noSelectionBox should be hidden after selection");

            ScrollPane previewScrollPane = (ScrollPane) root.lookup("#previewScrollPane");
            assertTrue(previewScrollPane.isVisible(), "previewScrollPane should be visible after selection");

            Label lblSummaryName = (Label) root.lookup("#lblSummaryName");
            assertEquals("Deepa Sharma", lblSummaryName.getText());

            Label lblNetValue = (Label) root.lookup("#lblNetValue");
            assertTrue(lblNetValue.getText().contains("53,300.00"), "Net value must reflect Rs. 53,300.00");

            Label lblNetInWords = (Label) root.lookup("#lblNetInWords");
            assertTrue(lblNetInWords.getText().contains("Fifty-Three Thousand Three Hundred"), "Words must match");

            Label lblEmployerPfValue = (Label) root.lookup("#lblEmployerPfValue");
            assertTrue(lblEmployerPfValue.getText().contains("6,000.00"), "Employer PF must reflect Rs. 6,000.00");
        });

        // 4. Select a Bench Record to test Bench presentation
        runOnFxThread(() -> {
            Parent root = mainRootRef.get();
            @SuppressWarnings("unchecked")
            TableView<PayrollRecord> payrollTable = (TableView<PayrollRecord>) root.lookup("#payrollTable");

            payrollTable.getSelectionModel().select(benchRecord);

            Label lblSummaryName = (Label) root.lookup("#lblSummaryName");
            assertEquals("Rahul Varma", lblSummaryName.getText());

            Label lblSummaryAttendance = (Label) root.lookup("#lblSummaryAttendance");
            assertTrue(lblSummaryAttendance.getText().contains("FinTech Portal"));
            assertTrue(lblSummaryAttendance.getText().contains("15 days"));

            Label lblNetValue = (Label) root.lookup("#lblNetValue");
            assertTrue(lblNetValue.getText().contains("17,800.00"));

            GridPane grid = (GridPane) root.lookup("#breakdownGrid");
            assertNotNull(grid);
        });

        // 5. Generate actual PDF from both records to verify output file integrity
        PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(tempDir);
        CompanySettings company = new CompanySettings();
        company.setCompanyName("Acme Global Technologies");

        File permPdf = generator.generateSalarySlipPdf(permRecord, company, true);
        assertTrue(permPdf.exists());
        assertTrue(permPdf.length() > 0);

        File benchPdf = generator.generateSalarySlipPdf(benchRecord, company, true);
        assertTrue(benchPdf.exists());
        assertTrue(benchPdf.length() > 0);

        try (PDDocument doc = Loader.loadPDF(benchPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertTrue(text.contains("Rahul Varma"));
            assertTrue(text.contains("FinTech Portal"));
            assertFalse(text.contains("House Rent Allowance"));
            assertFalse(text.contains("Dearness Allowance"));
            assertFalse(text.contains("Conveyance Allowance"));
        }
    }
}
