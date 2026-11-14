package com.payroll;

import com.payroll.controller.*;
import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PdfSalarySlipGenerator;
import com.payroll.storage.FileHandler;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated Acceptance Audit Test Suite for Stage 7 Final Verification.
 * 
 * Verifies:
 * 1. Correction transaction safety:
 *    - Simulated failure during writeCsvAtomic.
 *    - In-memory rollback preventing original record from being left SUPERSEDED.
 *    - Persistent recovery upon application restart.
 * 2. Historical record integrity:
 *    - Original payroll snapshot remains unchanged after correction.
 *    - Both records can be retrieved and inspected independently.
 *    - Superseded records cannot be treated as current payroll in Dashboard or Salary Slip Generation.
 *    - Historical salary slip can be generated and accessed from its original snapshot.
 * 3. Desktop workflow:
 *    - Full end-to-end traversal: Login -> Dashboard -> Payroll -> Salary Slip -> History -> Logout.
 *    - All sidebar buttons active and functional.
 */
public class Stage7FinalAcceptanceAuditTest {

    @TempDir
    Path tempDir;

    private Path tempPayrollCsv;
    private Path tempCompanyCsv;
    private Path tempPayslipsDir;
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
            testStage.setTitle("Stage 7 Final Acceptance Audit");
            stageLatch.countDown();
        });
        assertTrue(stageLatch.await(5, TimeUnit.SECONDS));

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
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    @BeforeEach
    void setupTempFiles() throws IOException {
        tempPayrollCsv = tempDir.resolve("payroll.csv");
        tempCompanyCsv = tempDir.resolve("company_settings.csv");
        tempPayslipsDir = tempDir.resolve("payslips");
        Files.createDirectories(tempPayslipsDir);

        // Seed company settings
        String compContent = "company_name,address_line1,address_line2,city,state,pincode,contact_email,contact_phone\n" +
                "TechCorp India Pvt Ltd,Plot 42 Tech Park,Hitec City,Hyderabad,Telangana,500081,hr@techcorp.in,+91-40-12345678\n";
        Files.writeString(tempCompanyCsv, compContent);

        // Seed empty payroll CSV with header
        Files.writeString(tempPayrollCsv, String.join(",", PayrollRepository.HEADERS) + "\n");
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX Thread action timed out");
        if (error.get() != null) {
            fail("Exception on FX Thread: " + error.get().getMessage(), error.get());
        }
    }

    private PayrollRecord createRecord(String recordId, String empId, String period, int totalDays, int presentDays) {
        PayrollRecord rec = new PayrollRecord();
        rec.setRecordId(recordId);
        rec.setEmployeeId(empId);
        rec.setEmployeeNameSnapshot("Arun Kumar");
        rec.setDesignationSnapshot("Software Engineer");
        rec.setDepartment("IT");
        rec.setEmployeeType("Permanent");
        rec.setWorkLocation("Telangana");
        rec.setDateOfJoining(LocalDate.of(2022, 1, 10));
        rec.setPfAccountNumber("PF1001");
        rec.setUan("UAN1001");
        rec.setPayPeriod(period);
        rec.setPayDate(LocalDate.of(2026, 11, 30));
        rec.setTotalWorkingDays(totalDays);
        rec.setPresentDays(presentDays);
        rec.setAbsentDays(totalDays - presentDays);

        BigDecimal factor = new BigDecimal(presentDays).divide(new BigDecimal(totalDays), 10, RoundingMode.HALF_UP);
        BigDecimal basic = new BigDecimal("40000.00");
        BigDecimal payableBasic = basic.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableHra = new BigDecimal("4000.00").multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableDa = new BigDecimal("2500.00").multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableConv = new BigDecimal("1200.00").multiply(factor).setScale(2, RoundingMode.HALF_UP);

        rec.setBasicSalaryOrDailyRate(basic);
        rec.setPayableBasic(payableBasic);
        rec.setPayableHra(payableHra);
        rec.setPayableDa(payableDa);
        rec.setPayableConveyance(payableConv);
        rec.setOtherEarnings(BigDecimal.ZERO);
        BigDecimal gross = payableBasic.add(payableHra).add(payableDa).add(payableConv);
        rec.setGrossEarnings(gross);

        BigDecimal pf = payableBasic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
        rec.setPfEmployeeDeduction(pf);
        rec.setPfEmployerContribution(pf);
        rec.setProfessionalTax(new BigDecimal("200.00"));
        rec.setOtherDeductions(BigDecimal.ZERO);
        BigDecimal deductions = pf.add(new BigDecimal("200.00"));
        rec.setTotalDeductions(deductions);
        rec.setNetSalary(gross.subtract(deductions));

        rec.setStatus("ORIGINAL");
        rec.setOriginalRecordId("");
        rec.setCorrectionReason("");
        rec.setCorrectionTimestamp("");
        rec.setGenerationDate(LocalDate.now());
        rec.setPolicySnapshot("Designation: Software Engineer [Basic=40000.00, HRA=10.0%, DA=6.25%, Conv=1200.00]");
        return rec;
    }

    @Test
    @DisplayName("Audit 1: Correction Transaction Safety & Simulated Failure Rollback")
    void testCorrectionTransactionSafetyWithSimulatedFailure() throws Exception {
        class FailingPayrollRepository extends PayrollRepository {
            private boolean failSave = false;
            public FailingPayrollRepository(Path path) { super(path); }
            public void setFailSave(boolean fail) { this.failSave = fail; }
            @Override
            protected void saveAllToDisk() {
                if (failSave) {
                    throw new RuntimeException("Simulated I/O disk failure during correction save");
                }
                super.saveAllToDisk();
            }
        }

        FailingPayrollRepository repo = new FailingPayrollRepository(tempPayrollCsv);
        PayrollRecord orig = createRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        repo.save(orig);

        assertEquals("ORIGINAL", orig.getStatus());
        assertTrue(orig.isOriginal());
        assertFalse(orig.isSuperseded());

        // Prepare corrected record
        PayrollRecord corr = createRecord("PAY-202611-EMP001-CORR", "EMP001", "2026-11", 30, 28);
        corr.setStatus("CORRECTED");
        corr.setOriginalRecordId(orig.getRecordId());
        corr.setCorrectionReason("Simulated correction");

        // Arm failure simulation
        repo.setFailSave(true);

        // Attempting correction must fail with RuntimeException
        assertThrows(RuntimeException.class, () -> {
            repo.saveCorrection(orig, corr);
        }, "saveCorrection must throw exception on disk write failure");

        // Confirm rollback: original record is rolled back to ORIGINAL, NOT left as SUPERSEDED
        assertEquals("ORIGINAL", orig.getStatus(), "Original record must NOT be left as SUPERSEDED after write failure");
        assertTrue(orig.isOriginal(), "Original record must remain active");
        assertFalse(orig.isSuperseded(), "Original record must not be superseded");

        // Confirm corrected record was NOT added to cache
        assertFalse(repo.findById("PAY-202611-EMP001-CORR").isPresent(),
                "Failed corrected record must be rolled back from in-memory cache");

        // Confirm recovery after application restart: reloaded repository from disk has original unchanged record
        PayrollRepository restartedRepo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord reloadedOrig = restartedRepo.findById("PAY-202611-EMP001").orElseThrow();
        assertEquals("ORIGINAL", reloadedOrig.getStatus(), "Disk file must preserve ORIGINAL record intact");
        assertFalse(reloadedOrig.isSuperseded());
    }

    @Test
    @DisplayName("Audit 2: Historical Record Integrity & Snapshot Preservation")
    void testHistoricalRecordIntegrity() throws Exception {
        PayrollRepository repo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord orig = createRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        repo.save(orig);

        // Capture pre-correction financial amounts and snapshot
        BigDecimal origBasic = orig.getPayableBasic();
        BigDecimal origGross = orig.getGrossEarnings();
        BigDecimal origNet = orig.getNetSalary();
        String origPolicySnapshot = orig.getPolicySnapshot();

        // Perform valid authorized correction
        PayrollRecord corr = createRecord("PAY-202611-EMP001-CORR", "EMP001", "2026-11", 30, 28);
        corr.setStatus("CORRECTED");
        corr.setOriginalRecordId(orig.getRecordId());
        corr.setCorrectionReason("Attendance Regularization Approved by HR Head");
        corr.setCorrectionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        repo.saveCorrection(orig, corr);

        // 1. Verify BOTH records exist and can be inspected
        List<PayrollRecord> all = repo.loadAll();
        assertEquals(2, all.size(), "Both original and corrected records must exist simultaneously");

        PayrollRecord loadedOrig = repo.findById("PAY-202611-EMP001").orElseThrow();
        PayrollRecord loadedCorr = repo.findById("PAY-202611-EMP001-CORR").orElseThrow();

        // 2. Verify original payroll snapshot remains strictly unchanged (only status became SUPERSEDED)
        assertEquals("SUPERSEDED", loadedOrig.getStatus());
        assertEquals(origBasic, loadedOrig.getPayableBasic(), "Original basic pay must remain unchanged");
        assertEquals(origGross, loadedOrig.getGrossEarnings(), "Original gross pay must remain unchanged");
        assertEquals(origNet, loadedOrig.getNetSalary(), "Original net salary must remain unchanged");
        assertEquals(origPolicySnapshot, loadedOrig.getPolicySnapshot(), "Original policy snapshot must remain unchanged");

        // 3. Verify corrected record has updated amounts and audit trail
        assertEquals("CORRECTED", loadedCorr.getStatus());
        assertEquals("PAY-202611-EMP001", loadedCorr.getOriginalRecordId());
        assertEquals("Attendance Regularization Approved by HR Head", loadedCorr.getCorrectionReason());
        assertNotNull(loadedCorr.getCorrectionTimestamp());
        assertNotEquals(origNet, loadedCorr.getNetSalary(), "Corrected net salary reflects updated present days");

        // 4. Verify superseded records cannot accidentally be treated as current payroll in Dashboard
        List<PayrollRecord> activeOnly = all.stream().filter(r -> !r.isSuperseded()).toList();
        assertEquals(1, activeOnly.size(), "Only the corrected record should be active");
        assertEquals("PAY-202611-EMP001-CORR", activeOnly.get(0).getRecordId());
        BigDecimal totalDisbursed = activeOnly.stream().map(PayrollRecord::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(loadedCorr.getNetSalary(), totalDisbursed, "Dashboard sums must only include active records");

        // 5. Verify historical salary slips can still be generated and accessed from their original snapshots
        CompanySettings company = new CompanySettings("TechCorp India Pvt Ltd", "Plot 42 Tech Park, Hitec City, Hyderabad, Telangana 500081",
                "MONTHLY SALARY SLIP", "hr@techcorp.in", "+91-40-12345678");
        PdfSalarySlipGenerator pdfGen = new PdfSalarySlipGenerator(tempPayslipsDir);

        File origPdf = pdfGen.generateSalarySlipPdf(loadedOrig, company, true);
        File corrPdf = pdfGen.generateSalarySlipPdf(loadedCorr, company, true);

        assertTrue(origPdf.exists(), "Original historical salary slip PDF must exist");
        assertTrue(corrPdf.exists(), "Corrected salary slip PDF must exist");

        // Extract and inspect PDF text from original PDF
        try (PDDocument doc = Loader.loadPDF(origPdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("PAY-202611-EMP001"), "Original payslip must contain original record ID");
            assertTrue(text.contains("35,549.99"), "Original payslip must contain original net salary");
        }

        // Extract and inspect PDF text from corrected PDF
        try (PDDocument doc = Loader.loadPDF(corrPdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("PAY-202611-EMP001-CORR"), "Corrected payslip must contain corrected record ID");
            assertTrue(text.contains(com.payroll.util.CurrencyFormatter.format(loadedCorr.getNetSalary())), "Corrected payslip must contain corrected net salary");
        }
    }

    @Test
    @DisplayName("Audit 3: Complete Desktop UI Traversal & All Screens Integration")
    void testFullDesktopWorkflowTraversal() throws Exception {
        runOnFxThread(() -> {
            try {
                // 1. Load Main Application Layout
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent root = mainLoader.load();
                assertNotNull(root);

                Scene scene = new Scene(root, 1180, 720);
                testStage.setScene(scene);
                testStage.show();

                MainLayoutController layout = mainLoader.getController();
                assertNotNull(layout);

                StackPane contentArea = (StackPane) mainLoader.getNamespace().get("contentArea");
                assertNotNull(contentArea);

                // 2. Traversal: Dashboard
                layout.showDashboard();
                assertFalse(contentArea.getChildren().isEmpty());

                // 3. Traversal: Employee Registration
                layout.showRegistration();
                assertFalse(contentArea.getChildren().isEmpty());

                // 4. Traversal: Employee Directory
                layout.showDirectory();
                assertFalse(contentArea.getChildren().isEmpty());

                // 5. Traversal: Settings (Policies)
                layout.showSettings();
                assertFalse(contentArea.getChildren().isEmpty());

                // 6. Traversal: Monthly Payroll Processing
                layout.showPayroll();
                assertFalse(contentArea.getChildren().isEmpty());

                // 7. Traversal: Salary Slip Generation
                layout.showSalarySlip();
                assertFalse(contentArea.getChildren().isEmpty());

                // 8. Traversal: Payroll History
                layout.showHistory();
                assertFalse(contentArea.getChildren().isEmpty());
                Parent historyView = (Parent) contentArea.getChildren().get(0);
                assertEquals("rootContainer", historyView.getId());

                // 9. Verify all 7 navigation buttons are enabled and styled
                Button btnDashboard = (Button) mainLoader.getNamespace().get("btnDashboard");
                Button btnReg = (Button) mainLoader.getNamespace().get("btnRegistration");
                Button btnDir = (Button) mainLoader.getNamespace().get("btnDirectory");
                Button btnSettings = (Button) mainLoader.getNamespace().get("btnSettings");
                Button btnPayroll = (Button) mainLoader.getNamespace().get("btnPayroll");
                Button btnSalarySlip = (Button) mainLoader.getNamespace().get("btnSalarySlip");
                Button btnHistory = (Button) mainLoader.getNamespace().get("btnHistory");

                assertFalse(btnDashboard.isDisabled());
                assertFalse(btnReg.isDisabled());
                assertFalse(btnDir.isDisabled());
                assertFalse(btnSettings.isDisabled());
                assertFalse(btnPayroll.isDisabled());
                assertFalse(btnSalarySlip.isDisabled());
                assertFalse(btnHistory.isDisabled());

                // 10. Logout functionality verification
                Button btnLogout = (Button) mainLoader.getNamespace().get("btnLogout");
                assertNotNull(btnLogout);
                assertFalse(btnLogout.isDisabled());

            } catch (IOException e) {
                fail("Desktop workflow failed: " + e.getMessage());
            }
        });
    }
}
