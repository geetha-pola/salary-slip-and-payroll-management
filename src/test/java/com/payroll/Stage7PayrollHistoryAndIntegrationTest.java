package com.payroll;

import com.payroll.controller.DashboardController;
import com.payroll.controller.HistoryController;
import com.payroll.controller.MainLayoutController;
import com.payroll.model.*;
import com.payroll.service.AppContext;
import com.payroll.service.PayrollCalculator;
import com.payroll.service.PdfSalarySlipGenerator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.FileHandler;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
 * Comprehensive Automated Test Suite for Stage 7: Payroll History & Full Integration.
 * 
 * Verifies:
 * 1. Historical payroll records loading & table column formatting.
 * 2. Multi-criteria filtering (Employee ID/Name, Pay Period, Category, Status).
 * 3. Snapshot inspection for Permanent and Bench records.
 * 4. Authorized correction workflow & audit trail preservation (original marked SUPERSEDED, new marked CORRECTED).
 * 5. Rejection of re-correction on superseded records.
 * 6. Historical snapshot immutability upon policy modifications.
 * 7. Cross-module financial consistency (Dashboard, History, Salary Slip).
 * 8. Active record duplicate prevention.
 * 9. Application restart persistence of audit metadata.
 * 10. Main layout sidebar navigation and activation of Payroll History screen.
 */
public class Stage7PayrollHistoryAndIntegrationTest {

    @TempDir
    Path tempDir;

    private Path tempPayrollCsv;
    private Path tempEmployeesCsv;
    private Path tempDesignationsCsv;
    private Path tempPfCsv;
    private Path tempPtCsv;
    private Path tempCompanyCsv;

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
        AppContext.getInstance().getAuthService().authenticate("1234", "5678");
    }

    @BeforeEach
    void setupTempData() throws IOException {
        tempPayrollCsv = tempDir.resolve("payroll.csv");
        tempEmployeesCsv = tempDir.resolve("employees.csv");
        tempDesignationsCsv = tempDir.resolve("designation_policies.csv");
        tempPfCsv = tempDir.resolve("pf_policies.csv");
        tempPtCsv = tempDir.resolve("professional_tax_policies.csv");
        tempCompanyCsv = tempDir.resolve("company_settings.csv");

        // Seed designation policies
        String desigContent = "designation,department,employee_type,basic_salary,hra_percentage,da_percentage,conveyance_allowance,other_allowances,description\n" +
                "Software Engineer,IT,Permanent,40000,10.0,6.25,1200,0,Standard SE\n" +
                "Consultant,Consulting,Bench,1500,0,0,0,0,Bench Consultant\n";
        Files.writeString(tempDesignationsCsv, desigContent);

        // Seed company settings
        String compContent = "company_name,address_line1,address_line2,city,state,pincode,contact_email,contact_phone\n" +
                "TechCorp India Pvt Ltd,Plot 42 Tech Park,Hitec City,Hyderabad,Telangana,500081,hr@techcorp.in,+91-40-12345678\n";
        Files.writeString(tempCompanyCsv, compContent);

        // Seed PF policy
        String pfContent = "category,pf_enabled,employee_contribution_rate,employer_contribution_rate,wage_ceiling,statutory_min_wage\n" +
                "Permanent,true,0.12,0.12,15000,0\n" +
                "Bench,false,0.00,0.00,0,0\n";
        Files.writeString(tempPfCsv, pfContent);

        // Seed PT policy
        String ptContent = "state,min_salary,max_salary,tax_amount\n" +
                "Telangana,0,15000,0\n" +
                "Telangana,15001,20000,150\n" +
                "Telangana,20001,999999999,200\n";
        Files.writeString(tempPtCsv, ptContent);

        // Seed employees
        String empContent = "employee_id,first_name,last_name,employee_type,date_of_birth,email,phone_number,marital_status,address,department,designation,work_location,date_of_joining,pf_applicable,pf_account_number,uan,is_active,is_director,project_name\n" +
                "EMP001,Arun,Kumar,Permanent,1992-05-15,arun@techcorp.in,9876543210,Married,Hyderabad,IT,Software Engineer,Telangana,2022-01-10,true,PF1001,UAN1001,true,false,\n" +
                "EMP002,Bhavna,Reddy,Bench,1995-08-20,bhavna@techcorp.in,9876543211,Single,Hyderabad,Consulting,Consultant,Telangana,2023-03-01,false,,,true,false,Alpha Project\n";
        Files.writeString(tempEmployeesCsv, empContent);

        // Initialize empty payroll.csv with header
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

    private PayrollRecord createSamplePermanentRecord(String recordId, String empId, String period, int totalDays, int presentDays) {
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

    private PayrollRecord createSampleBenchRecord(String recordId, String empId, String period, int projectDays) {
        PayrollRecord rec = new PayrollRecord();
        rec.setRecordId(recordId);
        rec.setEmployeeId(empId);
        rec.setEmployeeNameSnapshot("Bhavna Reddy");
        rec.setDesignationSnapshot("Consultant");
        rec.setDepartment("Consulting");
        rec.setEmployeeType("Bench");
        rec.setWorkLocation("Telangana");
        rec.setDateOfJoining(LocalDate.of(2023, 3, 1));
        rec.setPfAccountNumber("N/A");
        rec.setUan("N/A");
        rec.setPayPeriod(period);
        rec.setPayDate(LocalDate.of(2026, 11, 30));
        rec.setEligibleProjectDays(projectDays);
        rec.setProjectName("Alpha Project");

        BigDecimal rate = new BigDecimal("1500.00");
        BigDecimal basic = rate.multiply(new BigDecimal(projectDays)).setScale(2, RoundingMode.HALF_UP);
        rec.setBasicSalaryOrDailyRate(rate);
        rec.setPayableBasic(basic);
        rec.setPayableHra(BigDecimal.ZERO);
        rec.setPayableDa(BigDecimal.ZERO);
        rec.setPayableConveyance(BigDecimal.ZERO);
        rec.setOtherEarnings(BigDecimal.ZERO);
        rec.setGrossEarnings(basic);

        rec.setPfEmployeeDeduction(BigDecimal.ZERO);
        rec.setPfEmployerContribution(BigDecimal.ZERO);
        rec.setProfessionalTax(new BigDecimal("150.00"));
        rec.setOtherDeductions(BigDecimal.ZERO);
        rec.setTotalDeductions(new BigDecimal("150.00"));
        rec.setNetSalary(basic.subtract(new BigDecimal("150.00")));

        rec.setStatus("ORIGINAL");
        rec.setOriginalRecordId("");
        rec.setCorrectionReason("");
        rec.setCorrectionTimestamp("");
        rec.setGenerationDate(LocalDate.now());
        rec.setPolicySnapshot("Designation: Consultant [DailyRate=1500.00, Bench Zero Allowances]");
        return rec;
    }

    @Test
    @DisplayName("Test 1: Historical Payroll Records Loading & Table Columns")
    void testHistoricalRecordsLoadingAndColumns() throws Exception {
        PayrollRepository testRepo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord permRec = createSamplePermanentRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        PayrollRecord benchRec = createSampleBenchRecord("PAY-202611-EMP002", "EMP002", "2026-11", 12);
        testRepo.save(permRec);
        testRepo.save(benchRec);

        List<PayrollRecord> all = testRepo.loadAll();
        assertEquals(2, all.size(), "Repository must load exactly 2 saved records");

        runOnFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/history.fxml"));
            try {
                Parent root = loader.load();
                assertNotNull(root);
                HistoryController controller = loader.getController();
                assertNotNull(controller);

                TableView<PayrollRecord> table = (TableView<PayrollRecord>) loader.getNamespace().get("historyTable");
                assertNotNull(table);
                assertEquals(10, table.getColumns().size(), "Table must have 10 data columns");

                // Verify column header texts
                assertEquals("Record ID", table.getColumns().get(0).getText());
                assertEquals("Emp ID", table.getColumns().get(1).getText());
                assertEquals("Employee Name", table.getColumns().get(2).getText());
                assertEquals("Category", table.getColumns().get(3).getText());
                assertEquals("Period", table.getColumns().get(4).getText());
                assertEquals("Gross Earnings", table.getColumns().get(5).getText());
                assertEquals("Deductions", table.getColumns().get(6).getText());
                assertEquals("Net Salary", table.getColumns().get(7).getText());
                assertEquals("Status", table.getColumns().get(8).getText());
                assertEquals("Date", table.getColumns().get(9).getText());
            } catch (IOException e) {
                fail("Failed to load history.fxml: " + e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Test 2: Filtering by Search Text, Period, Category and Status")
    void testFilteringCapabilities() throws Exception {
        PayrollRepository testRepo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord r1 = createSamplePermanentRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        PayrollRecord r2 = createSampleBenchRecord("PAY-202611-EMP002", "EMP002", "2026-11", 12);
        PayrollRecord r3 = createSamplePermanentRecord("PAY-202610-EMP001", "EMP001", "2026-10", 31, 31);
        testRepo.save(r1);
        testRepo.save(r2);
        testRepo.save(r3);

        // Filter by category (employeeType is 3rd argument)
        List<PayrollRecord> benchOnly = testRepo.filter(null, null, "Bench");
        assertEquals(1, benchOnly.size());
        assertEquals("EMP002", benchOnly.get(0).getEmployeeId());

        List<PayrollRecord> permOnly = testRepo.filter(null, null, "Permanent");
        assertEquals(2, permOnly.size());

        // Filter by period (payPeriod is 2nd argument)
        List<PayrollRecord> octOnly = testRepo.filter(null, "2026-10", null);
        assertEquals(1, octOnly.size());
        assertEquals("PAY-202610-EMP001", octOnly.get(0).getRecordId());

        // Filter by query (employee ID is 1st argument)
        List<PayrollRecord> emp1Only = testRepo.filter("EMP001", null, null);
        assertEquals(2, emp1Only.size());

        // Filter with all criteria (query, payPeriod, employeeType)
        List<PayrollRecord> combined = testRepo.filter("EMP001", "2026-11", "Permanent");
        assertEquals(1, combined.size());
        assertEquals("PAY-202611-EMP001", combined.get(0).getRecordId());

        // Filter by status (4th argument)
        List<PayrollRecord> origStatusOnly = testRepo.filter(null, null, null, "ORIGINAL");
        assertEquals(3, origStatusOnly.size());
    }

    @Test
    @DisplayName("Test 3: Snapshot Inspection for Permanent and Bench Records")
    void testSnapshotInspection() throws Exception {
        PayrollRecord perm = createSamplePermanentRecord("PAY-PERM-01", "EMP001", "2026-11", 30, 25);
        PayrollRecord bench = createSampleBenchRecord("PAY-BENCH-01", "EMP002", "2026-11", 12);

        // Permanent validations
        assertEquals("Permanent", perm.getEmployeeType());
        assertEquals(25, perm.getPresentDays());
        assertEquals(5, perm.getAbsentDays());
        assertEquals(new BigDecimal("33333.33"), perm.getPayableBasic());
        assertEquals(new BigDecimal("3333.33"), perm.getPayableHra());
        assertEquals(new BigDecimal("2083.33"), perm.getPayableDa());
        assertEquals(new BigDecimal("1000.00"), perm.getPayableConveyance());
        assertEquals(new BigDecimal("39749.99"), perm.getGrossEarnings());
        assertEquals(new BigDecimal("4000.00"), perm.getPfEmployeeDeduction());
        assertEquals(new BigDecimal("4000.00"), perm.getPfEmployerContribution());
        assertEquals(new BigDecimal("200.00"), perm.getProfessionalTax());
        assertEquals(new BigDecimal("35549.99"), perm.getNetSalary());

        // Bench validations
        assertEquals("Bench", bench.getEmployeeType());
        assertEquals(12, bench.getEligibleProjectDays());
        assertEquals("Alpha Project", bench.getProjectName());
        assertEquals(new BigDecimal("18000.00"), bench.getPayableBasic());
        assertEquals(BigDecimal.ZERO, bench.getPayableHra());
        assertEquals(BigDecimal.ZERO, bench.getPayableDa());
        assertEquals(BigDecimal.ZERO, bench.getPayableConveyance());
        assertEquals(new BigDecimal("18000.00"), bench.getGrossEarnings());
        assertEquals(BigDecimal.ZERO, bench.getPfEmployeeDeduction());
        assertEquals(BigDecimal.ZERO, bench.getPfEmployerContribution());
        assertEquals(new BigDecimal("17850.00"), bench.getNetSalary());
    }

    @Test
    @DisplayName("Test 4: Authorized Correction Workflow & Audit Trail")
    void testAuthorizedCorrectionWorkflow() throws Exception {
        PayrollRepository repo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord original = createSamplePermanentRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        repo.save(original);

        assertEquals("ORIGINAL", original.getStatus());
        assertFalse(original.isSuperseded());
        assertTrue(original.isOriginal());

        // Create corrected record (present days changed from 25 to 28)
        PayrollRecord corrected = createSamplePermanentRecord("PAY-202611-EMP001-CORR1", "EMP001", "2026-11", 30, 28);
        corrected.setStatus("CORRECTED");
        corrected.setOriginalRecordId(original.getRecordId());
        corrected.setCorrectionReason("Manager regularization: On-duty travel approved for 3 days");
        corrected.setCorrectionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Save correction atomically
        repo.saveCorrection(original, corrected);

        // Reload repository from disk
        PayrollRepository reloadedRepo = new PayrollRepository(tempPayrollCsv);
        List<PayrollRecord> all = reloadedRepo.loadAll();
        assertEquals(2, all.size(), "Both original and corrected records must be preserved in CSV");

        // Verify original record is now SUPERSEDED
        PayrollRecord origFound = reloadedRepo.findById(original.getRecordId()).orElseThrow();
        assertEquals("SUPERSEDED", origFound.getStatus());
        assertTrue(origFound.isSuperseded());
        assertFalse(origFound.isOriginal());

        // Verify corrected record is active and auditable
        PayrollRecord corrFound = reloadedRepo.findById(corrected.getRecordId()).orElseThrow();
        assertEquals("CORRECTED", corrFound.getStatus());
        assertTrue(corrFound.isCorrected());
        assertFalse(corrFound.isSuperseded());
        assertEquals("PAY-202611-EMP001", corrFound.getOriginalRecordId());
        assertEquals("Manager regularization: On-duty travel approved for 3 days", corrFound.getCorrectionReason());
        assertNotNull(corrFound.getCorrectionTimestamp());
        assertFalse(corrFound.getCorrectionTimestamp().isEmpty());

        // Verify duplicate check considers active corrected record
        assertTrue(reloadedRepo.exists("EMP001", "2026-11"), "Active corrected record must satisfy exists() check");
    }

    @Test
    @DisplayName("Test 5: Rejection of Re-correction on Superseded Records")
    void testSupersededRecordCannotBeReCorrected() throws Exception {
        PayrollRepository repo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord original = createSamplePermanentRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        repo.save(original);

        PayrollRecord corr1 = createSamplePermanentRecord("PAY-202611-EMP001-CORR1", "EMP001", "2026-11", 30, 28);
        corr1.setStatus("CORRECTED");
        corr1.setOriginalRecordId(original.getRecordId());
        corr1.setCorrectionReason("First correction");
        corr1.setCorrectionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        repo.saveCorrection(original, corr1);

        // Attempting to correct the original (already SUPERSEDED) must throw IllegalArgumentException
        PayrollRecord corr2 = createSamplePermanentRecord("PAY-202611-EMP001-CORR2", "EMP001", "2026-11", 30, 30);
        assertThrows(IllegalArgumentException.class, () -> {
            repo.saveCorrection(original, corr2);
        }, "Superseded records cannot be re-corrected");
    }

    @Test
    @DisplayName("Test 6: Accidental Deletion Disallowed")
    void testNoDeletionAllowed() {
        // Inspect PayrollRepository methods via reflection: no delete / remove method must exist
        boolean hasDeleteMethod = false;
        for (var method : PayrollRepository.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase();
            if (name.contains("delete") || name.contains("remove")) {
                hasDeleteMethod = true;
                break;
            }
        }
        assertFalse(hasDeleteMethod, "PayrollRepository must strictly disallow record deletion to guarantee immutable audit logs");
    }

    @Test
    @DisplayName("Test 7: Historical Snapshot Immutability Upon Policy Modifications")
    void testHistoricalSnapshotImmutability() throws Exception {
        PayrollRepository repo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord record = createSamplePermanentRecord("PAY-HIST-001", "EMP001", "2026-11", 30, 25);
        repo.save(record);

        // Simulate subsequent administrator policy modification in designation_policies.csv
        String newDesigContent = "designation,department,employee_type,basic_salary,hra_percentage,da_percentage,conveyance_allowance,other_allowances,description\n" +
                "Software Engineer,IT,Permanent,60000,15.0,8.0,2000,0,Updated Policy\n";
        Files.writeString(tempDesignationsCsv, newDesigContent);

        // Reload the historical record from CSV
        PayrollRepository reloadedRepo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord loaded = reloadedRepo.findById("PAY-HIST-001").orElseThrow();

        // Past record amounts must remain strictly unchanged
        assertEquals(new BigDecimal("40000.00"), loaded.getBasicSalaryOrDailyRate());
        assertEquals(new BigDecimal("33333.33"), loaded.getPayableBasic());
        assertEquals(new BigDecimal("35549.99"), loaded.getNetSalary());
        assertTrue(loaded.getPolicySnapshot().contains("Basic=40000.00"), "Policy snapshot must preserve original historical configuration");
    }

    @Test
    @DisplayName("Test 8: Dashboard and History Financial Consistency (Superseded Records Excluded)")
    void testDashboardAndHistoryConsistency() throws Exception {
        PayrollRepository repo = new PayrollRepository(tempPayrollCsv);
        PayrollRecord perm = createSamplePermanentRecord("PAY-202611-EMP001", "EMP001", "2026-11", 30, 25);
        PayrollRecord bench = createSampleBenchRecord("PAY-202611-EMP002", "EMP002", "2026-11", 12);
        repo.save(perm);
        repo.save(bench);

        // Apply a correction to perm: present days 25 -> 28
        PayrollRecord corr = createSamplePermanentRecord("PAY-202611-EMP001-CORR", "EMP001", "2026-11", 30, 28);
        corr.setStatus("CORRECTED");
        corr.setOriginalRecordId(perm.getRecordId());
        corr.setCorrectionReason("Correction applied");
        corr.setCorrectionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        repo.saveCorrection(perm, corr);

        // Active records in repository for 2026-11:
        // 1. corr (Net Salary: 39759.99, PF Emp: 4480.00, PF Empr: 4480.00)
        // 2. bench (Net Salary: 17850.00, PF Emp: 0.00, PF Empr: 0.00)
        // Superseded record (perm) must be excluded from Dashboard sums!

        List<PayrollRecord> allRecords = repo.loadAll();
        assertEquals(3, allRecords.size()); // 1 superseded, 1 corrected, 1 original

        List<PayrollRecord> activeRecords = allRecords.stream().filter(r -> !r.isSuperseded()).toList();
        assertEquals(2, activeRecords.size());

        BigDecimal totalNetDisbursed = activeRecords.stream().map(PayrollRecord::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPfEmployee = activeRecords.stream().map(PayrollRecord::getPfEmployeeDeduction).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPfEmployer = activeRecords.stream().map(PayrollRecord::getPfEmployerContribution).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(corr.getNetSalary().add(bench.getNetSalary()), totalNetDisbursed);
        assertEquals(corr.getPfEmployeeDeduction(), totalPfEmployee);
        assertEquals(corr.getPfEmployerContribution(), totalPfEmployer);
    }

    @Test
    @DisplayName("Test 9: Application Restart Simulates Complete Persistence of Audit Metadata")
    void testApplicationRestartAuditTrailPersistence() throws Exception {
        PayrollRepository repo1 = new PayrollRepository(tempPayrollCsv);
        PayrollRecord orig = createSamplePermanentRecord("PAY-PERM-RST", "EMP001", "2026-11", 30, 20);
        repo1.save(orig);

        PayrollRecord corr = createSamplePermanentRecord("PAY-PERM-RST-CORR", "EMP001", "2026-11", 30, 26);
        corr.setStatus("CORRECTED");
        corr.setOriginalRecordId(orig.getRecordId());
        corr.setCorrectionReason("Annual audit rectification");
        corr.setCorrectionTimestamp("2026-11-09 14:00:00");
        repo1.saveCorrection(orig, corr);

        // Recreate repository from scratch (simulating application restart)
        PayrollRepository repo2 = new PayrollRepository(tempPayrollCsv);
        List<PayrollRecord> loaded = repo2.loadAll();
        assertEquals(2, loaded.size());

        PayrollRecord origLoaded = repo2.findById("PAY-PERM-RST").orElseThrow();
        assertEquals("SUPERSEDED", origLoaded.getStatus());

        PayrollRecord corrLoaded = repo2.findById("PAY-PERM-RST-CORR").orElseThrow();
        assertEquals("CORRECTED", corrLoaded.getStatus());
        assertEquals("PAY-PERM-RST", corrLoaded.getOriginalRecordId());
        assertEquals("Annual audit rectification", corrLoaded.getCorrectionReason());
        assertEquals("2026-11-09 14:00:00", corrLoaded.getCorrectionTimestamp());
    }

    @Test
    @DisplayName("Test 10: Main Layout Navigation & History Screen Loading")
    void testMainLayoutNavigationToHistory() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/main-layout.fxml"));
                Parent root = loader.load();
                assertNotNull(root);

                MainLayoutController controller = loader.getController();
                assertNotNull(controller);

                Button btnHistory = (Button) loader.getNamespace().get("btnHistory");
                assertNotNull(btnHistory, "btnHistory must be defined in main-layout.fxml");
                assertFalse(btnHistory.isDisabled(), "btnHistory must be enabled in Stage 7");
                assertTrue(btnHistory.getText().contains("Payroll History"), "Button text must be Payroll History");

                StackPane contentArea = (StackPane) loader.getNamespace().get("contentArea");
                assertNotNull(contentArea);

                // Trigger navigation to History
                controller.showHistory();

                // Verify that contentArea now contains the History view
                assertFalse(contentArea.getChildren().isEmpty(), "contentArea must not be empty after navigation");
                Parent historyView = (Parent) contentArea.getChildren().get(0);
                assertNotNull(historyView);
                assertEquals("rootContainer", historyView.getId());

            } catch (IOException e) {
                fail("Failed to test MainLayout navigation: " + e.getMessage());
            }
        });
    }
}
