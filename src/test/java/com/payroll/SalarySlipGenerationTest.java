package com.payroll;

import com.payroll.controller.SalarySlipController;
import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.service.PdfSalarySlipGenerator;
import com.payroll.util.NumberToWordsConverter;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive automated test suite for Stage 6: Professional PDF Salary Slip Generation.
 * Validates:
 * 1. NumberToWordsConverter (Indian numbering conventions, Crores, Lakhs, Thousands, Hundreds, Paise).
 * 2. Permanent employee PDF payslip structure, pro-rated breakdown, and statutory notes.
 * 3. Bench employee PDF payslip structure: strictly NO HRA, DA, or Conveyance rows.
 * 4. Strict Confidentiality: Absence of medical / diagnostic information.
 * 5. File overwrite protection.
 * 6. JavaFX FXML loading and SalarySlipController verification on the FX Application Thread.
 */
public class SalarySlipGenerationTest {

    private static boolean fxInitialized = false;

    @BeforeAll
    static void initJavaFx() {
        if (!fxInitialized) {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
                // Toolkit already initialized
            }
            Platform.setImplicitExit(false);
            fxInitialized = true;
        }
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX Thread action timed out");
        if (error[0] != null) {
            if (error[0] instanceof RuntimeException) throw (RuntimeException) error[0];
            if (error[0] instanceof Error) throw (Error) error[0];
            throw new RuntimeException(error[0]);
        }
    }

    private static CompanySettings createSampleCompany() {
        CompanySettings company = new CompanySettings();
        company.setCompanyName("TechNova Solutions Pvt Ltd");
        company.setCompanyAddress("Plot 42, Hitech City, Hyderabad, Telangana - 500081");
        company.setContactEmail("payroll@technova.com");
        company.setContactPhone("+91 40 2345 6789");
        company.setPayslipTitle("CONFIDENTIAL SALARY STATEMENT");
        return company;
    }

    private static PayrollRecord createSamplePermanentRecord() {
        PayrollRecord rec = new PayrollRecord();
        rec.setRecordId("REC-202610-001");
        rec.setEmployeeId("EMP-1001");
        rec.setEmployeeNameSnapshot("Arjun Rao");
        rec.setEmployeeType("Permanent");
        rec.setDepartment("Engineering");
        rec.setDesignationSnapshot("Senior Software Engineer");
        rec.setWorkLocation("Hyderabad");
        rec.setPfAccountNumber("PF-1001-AP");
        rec.setUan("UAN-990011");
        rec.setDateOfJoining(LocalDate.of(2023, 5, 10));
        rec.setPayPeriod("10/2026");
        rec.setPayDate(LocalDate.of(2026, 10, 31));
        rec.setTotalWorkingDays(30);
        rec.setPresentDays(25);
        rec.setAbsentDays(5);
        rec.setBasicSalaryOrDailyRate(new BigDecimal("40000.00"));
        rec.setPayableBasic(new BigDecimal("33333.33"));
        rec.setPayableHra(new BigDecimal("3333.33"));
        rec.setPayableDa(new BigDecimal("2083.33"));
        rec.setPayableConveyance(new BigDecimal("1000.00"));
        rec.setOtherEarnings(BigDecimal.ZERO);
        rec.setGrossEarnings(new BigDecimal("39750.00"));
        rec.setPfEmployeeDeduction(new BigDecimal("4000.00"));
        rec.setPfEmployerContribution(new BigDecimal("4000.00"));
        rec.setProfessionalTax(new BigDecimal("200.00"));
        rec.setOtherDeductions(BigDecimal.ZERO);
        rec.setTotalDeductions(new BigDecimal("4200.00"));
        rec.setNetSalary(new BigDecimal("35550.00"));
        return rec;
    }

    private static PayrollRecord createSampleBenchRecord() {
        PayrollRecord rec = new PayrollRecord();
        rec.setRecordId("REC-202610-002");
        rec.setEmployeeId("EMP-2001");
        rec.setEmployeeNameSnapshot("Kavita Reddy");
        rec.setEmployeeType("Bench");
        rec.setDepartment("Consulting");
        rec.setDesignationSnapshot("Associate Consultant");
        rec.setWorkLocation("Andhra Pradesh");
        rec.setPfAccountNumber("");
        rec.setUan("");
        rec.setDateOfJoining(LocalDate.of(2024, 1, 15));
        rec.setPayPeriod("10/2026");
        rec.setPayDate(LocalDate.of(2026, 10, 31));
        rec.setBasicSalaryOrDailyRate(new BigDecimal("1500.00"));
        rec.setEligibleProjectDays(12);
        rec.setProjectName("Cloud Migration Initiative");
        rec.setPayableBasic(new BigDecimal("18000.00"));
        rec.setPayableHra(BigDecimal.ZERO);
        rec.setPayableDa(BigDecimal.ZERO);
        rec.setPayableConveyance(BigDecimal.ZERO);
        rec.setOtherEarnings(BigDecimal.ZERO);
        rec.setGrossEarnings(new BigDecimal("18000.00"));
        rec.setPfEmployeeDeduction(BigDecimal.ZERO);
        rec.setPfEmployerContribution(BigDecimal.ZERO);
        rec.setProfessionalTax(new BigDecimal("200.00"));
        rec.setOtherDeductions(BigDecimal.ZERO);
        rec.setTotalDeductions(new BigDecimal("200.00"));
        rec.setNetSalary(new BigDecimal("17800.00"));
        return rec;
    }

    @Nested
    @DisplayName("1. Indian Number to Words Converter Tests")
    class NumberToWordsTests {

        @Test
        @DisplayName("Zero monetary amount")
        void testZeroAmount() {
            assertEquals("Rupees Zero Only", NumberToWordsConverter.convertToIndianCurrencyWords(BigDecimal.ZERO));
            assertEquals("Rupees Zero Only", NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.00")));
        }

        @Test
        @DisplayName("Round amounts with thousands and hundreds")
        void testThousandsAndHundreds() {
            assertEquals("Rupees Thirty-Nine Thousand Seven Hundred Fifty Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("39750.00")));
            assertEquals("Rupees Eighteen Thousand Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("18000.00")));
            assertEquals("Rupees One Thousand Five Hundred Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("1500.00")));
        }

        @Test
        @DisplayName("Pro-rated amounts with Paise")
        void testAmountsWithPaise() {
            assertEquals("Rupees Thirty-Three Thousand Three Hundred Thirty-Three and Thirty-Three Paise Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("33333.33")));
            assertEquals("Rupees Two Thousand Eighty-Three and Thirty-Three Paise Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("2083.33")));
        }

        @Test
        @DisplayName("Large amounts (Lakhs and Crores)")
        void testLakhsAndCrores() {
            assertEquals("Rupees One Lakh Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("100000.00")));
            assertEquals("Rupees One Crore Twenty-Five Lakh Only",
                    NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("12500000.00")));
        }
    }

    @Nested
    @DisplayName("2. PDF Salary Slip Generation & Content Integrity Tests")
    class PdfGenerationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Permanent Employee PDF Salary Slip: Complete pro-rated breakdown & statutory separation")
        void testPermanentEmployeePdfGeneration() throws IOException {
            PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(tempDir);
            CompanySettings company = createSampleCompany();
            PayrollRecord permRecord = createSamplePermanentRecord();

            File generatedPdf = generator.generateSalarySlipPdf(permRecord, company, true);
            assertNotNull(generatedPdf);
            assertTrue(generatedPdf.exists(), "PDF file must exist on disk");
            assertTrue(generatedPdf.length() > 0, "PDF file must not be empty");

            // Inspect PDF text content using PDFBox PDFTextStripper
            try (PDDocument document = Loader.loadPDF(generatedPdf)) {
                assertEquals(1, document.getNumberOfPages(), "Salary slip should fit neatly on 1 A4 page");

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                // 1. Company branding
                assertTrue(text.contains("TechNova Solutions Pvt Ltd"), "Must contain company name");
                assertTrue(text.contains("Plot 42, Hitech City"), "Must contain company address");
                assertTrue(text.contains("payroll@technova.com"), "Must contain company email");
                assertTrue(text.contains("CONFIDENTIAL SALARY STATEMENT - 10/2026"), "Must contain custom title");

                // 2. Employee details
                assertTrue(text.contains("Arjun Rao"), "Must contain employee name");
                assertTrue(text.contains("EMP-1001"), "Must contain employee ID");
                assertTrue(text.contains("Permanent"), "Must contain category");
                assertTrue(text.contains("Senior Software Engineer"), "Must contain designation");
                assertTrue(text.contains("Engineering"), "Must contain department");
                assertTrue(text.contains("PF-1001-AP"), "Must contain PF account");
                assertTrue(text.contains("UAN-990011"), "Must contain UAN");

                // 3. Attendance breakdown
                assertTrue(text.contains("Total: 30"), "Must contain total days");
                assertTrue(text.contains("Present: 25"), "Must contain present days");
                assertTrue(text.contains("Absent / LOP: 5"), "Must contain absent days");

                // 4. Pro-rated Earnings
                assertTrue(text.contains("Basic Salary (Payable)"), "Must contain payable basic label");
                assertTrue(text.contains("33,333.33"), "Must contain payable basic value");
                assertTrue(text.contains("House Rent Allowance (HRA)"), "Must contain HRA label");
                assertTrue(text.contains("3,333.33"), "Must contain payable HRA value");
                assertTrue(text.contains("Dearness Allowance (DA)"), "Must contain DA label");
                assertTrue(text.contains("2,083.33"), "Must contain payable DA value");
                assertTrue(text.contains("Conveyance Allowance"), "Must contain Conveyance label");
                assertTrue(text.contains("1,000.00"), "Must contain payable Conveyance value");
                assertTrue(text.contains("39,750.00"), "Must contain Gross Earnings value");

                // 5. Deductions
                assertTrue(text.contains("Employee PF Contribution"), "Must contain PF deduction label");
                assertTrue(text.contains("4,000.00"), "Must contain PF deduction value");
                assertTrue(text.contains("Professional Tax (PT)"), "Must contain PT label");
                assertTrue(text.contains("200.00"), "Must contain PT value");
                assertTrue(text.contains("4,200.00"), "Must contain Total Deductions");

                // 6. Net Salary & In Words
                assertTrue(text.contains("35,550.00"), "Must contain Net Salary amount");
                assertTrue(text.contains("Rupees Thirty-Five Thousand Five Hundred Fifty Only"), "Must contain Net in Words");

                // 7. Employer PF Separation (Statutory Expense)
                assertTrue(text.contains("EMPLOYER PF CONTRIBUTION (COMPANY STATUTORY EXPENSE)"), "Must contain Employer PF header");
                assertTrue(text.contains("Statutory Notice: Employer PF is paid directly by the company"), "Must contain statutory notice");

                // 8. Confidentiality & Medical Privacy
                assertTrue(text.contains("Confidentiality:"), "Must contain confidentiality note");
                assertFalse(text.toLowerCase().contains("hypertension"), "Must not contain medical condition");
                assertFalse(text.toLowerCase().contains("medical condition"), "Must not contain medical condition label");
                assertFalse(text.toLowerCase().contains("blood group"), "Must not contain blood group");
            }
        }

        @Test
        @DisplayName("Bench Employee PDF Salary Slip: Daily rate, project allocation, STRICTLY NO HRA/DA/Conveyance")
        void testBenchEmployeePdfGeneration() throws IOException {
            PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(tempDir);
            CompanySettings company = createSampleCompany();
            PayrollRecord benchRecord = createSampleBenchRecord();

            File generatedPdf = generator.generateSalarySlipPdf(benchRecord, company, true);
            assertNotNull(generatedPdf);
            assertTrue(generatedPdf.exists());

            try (PDDocument document = Loader.loadPDF(generatedPdf)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                // Bench specific attributes
                assertTrue(text.contains("Kavita Reddy"), "Must contain bench employee name");
                assertTrue(text.contains("EMP-2001"), "Must contain bench employee ID");
                assertTrue(text.contains("Bench"), "Must state Bench category");
                assertTrue(text.contains("Cloud Migration Initiative"), "Must contain project name");
                assertTrue(text.contains("Eligible Days: 12"), "Must contain eligible project days");
                assertTrue(text.contains("1,500.00"), "Must contain daily rate");
                assertTrue(text.contains("18,000.00"), "Must contain Gross Earnings");
                assertTrue(text.contains("17,800.00"), "Must contain Net Salary");
                assertTrue(text.contains("Rupees Seventeen Thousand Eight Hundred Only"), "Must contain Net in Words");

                // STRICT BENCH RESTRICTIONS: Must NOT show HRA, DA, or Conveyance rows!
                assertFalse(text.contains("House Rent Allowance"), "Bench payslip must strictly omit House Rent Allowance");
                assertFalse(text.contains("Dearness Allowance"), "Bench payslip must strictly omit Dearness Allowance");
                assertFalse(text.contains("Conveyance Allowance"), "Bench payslip must strictly omit Conveyance Allowance");

                // Strictly NO medical information
                assertFalse(text.toLowerCase().contains("diabetes"), "Must not contain medical condition");
                assertFalse(text.toLowerCase().contains("medical condition"), "Must not contain medical info");
            }
        }

        @Test
        @DisplayName("File Overwrite Protection: Throws exception when allowOverwrite is false")
        void testOverwriteProtection() throws IOException {
            PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(tempDir);
            CompanySettings company = createSampleCompany();
            PayrollRecord record = createSamplePermanentRecord();
            record.setRecordId("REC-OVERWRITE-1");

            // First generation creates the file
            File file1 = generator.generateSalarySlipPdf(record, company, true);
            assertTrue(file1.exists());

            // Second generation with allowOverwrite=false must throw IllegalStateException
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                generator.generateSalarySlipPdf(record, company, false);
            }, "Should throw IllegalStateException when file exists and allowOverwrite is false");

            assertTrue(ex.getMessage().contains("already exists"), "Exception message should indicate file exists");
        }
    }

    @Nested
    @DisplayName("3. JavaFX Salary Slip Screen UI Verification Tests")
    class SalarySlipUiTests {

        @Test
        @DisplayName("Verify Salary Slip FXML loads without CSS/errors, and injects SalarySlipController")
        void testSalarySlipFxmlLoading() throws InterruptedException {
            runOnFxThread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/salary-slip.fxml"));
                    Parent root = loader.load();
                    assertNotNull(root, "salary-slip.fxml root must not be null");

                    SalarySlipController controller = loader.getController();
                    assertNotNull(controller, "SalarySlipController must be instantiated and injected");

                    // Verify controls presence
                    TextField searchField = (TextField) root.lookup("#searchField");
                    assertNotNull(searchField, "searchField must exist");

                    ComboBox<?> filterPeriod = (ComboBox<?>) root.lookup("#filterPeriod");
                    assertNotNull(filterPeriod, "filterPeriod ComboBox must exist");

                    ComboBox<?> filterCategory = (ComboBox<?>) root.lookup("#filterCategory");
                    assertNotNull(filterCategory, "filterCategory ComboBox must exist");

                    TableView<?> table = (TableView<?>) root.lookup("#payrollTable");
                    assertNotNull(table, "payrollTable must exist");
                    assertEquals(9, table.getColumns().size(), "payrollTable must have 9 columns");

                    Button btnBatch = (Button) root.lookup("#btnBatchGenerate");
                    assertNotNull(btnBatch, "btnBatchGenerate must exist");

                    Button btnFolder = (Button) root.lookup("#btnOpenFolder");
                    assertNotNull(btnFolder, "btnOpenFolder must exist");

                } catch (Exception e) {
                    throw new RuntimeException("Failed to load salary-slip.fxml: " + e.getMessage(), e);
                }
            });
        }
    }
}
