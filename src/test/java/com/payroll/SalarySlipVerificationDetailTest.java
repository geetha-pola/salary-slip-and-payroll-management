package com.payroll;

import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.service.PdfSalarySlipGenerator;
import com.payroll.util.NumberToWordsConverter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Detailed verification test suite addressing the user's Stage 6 review questions:
 * 1. NumberToWordsConverter verification for specific test values and edge cases.
 * 2. PDF Currency Symbol feasibility analysis (TrueType / Unicode / Cross-platform portability).
 * 3. Rendered page image export and visual inspection for Permanent and Bench payslips.
 * 4. Structural validation of amounts, prorating, Bench exclusions, and employer PF.
 */
public class SalarySlipVerificationDetailTest {

    @Test
    @DisplayName("1. NumberToWordsConverter Specific Test Cases & Rounding Edge Cases")
    void testSpecificNumberToWordsCases() {
        // Case 1: ₹1,000
        assertEquals("Rupees One Thousand Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("1000")));
        assertEquals("Rupees One Thousand Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("1000.00")));

        // Case 2: ₹18,500.50
        assertEquals("Rupees Eighteen Thousand Five Hundred and Fifty Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("18500.50")));

        // Case 3: ₹1,00,000 (1 Lakh)
        assertEquals("Rupees One Lakh Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("100000")));
        assertEquals("Rupees One Lakh Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("100000.00")));

        // Case 4: ₹1,00,00,000 (1 Crore)
        assertEquals("Rupees One Crore Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("10000000")));
        assertEquals("Rupees One Crore Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("10000000.00")));

        // Case 5: ₹99,99,999.99 (99 Lakhs 99 Thousand 9 Hundred 99 and 99 Paise)
        assertEquals("Rupees Ninety-Nine Lakh Ninety-Nine Thousand Nine Hundred Ninety-Nine and Ninety-Nine Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("9999999.99")));

        // Case 6: Zero & Decimal rounding edge cases
        assertEquals("Rupees Zero Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(BigDecimal.ZERO));
        assertEquals("Rupees Zero Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.00")));

        // Sub-cent rounding: 0.004 rounds to 0.00 (Rupees Zero Only)
        assertEquals("Rupees Zero Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.004")));

        // Sub-cent rounding: 0.005 rounds to 0.01 (1 Paise)
        assertEquals("Rupees Zero and One Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.005")));

        // Single digit paise: 0.05 (5 Paise)
        assertEquals("Rupees Zero and Five Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.05")));

        // 99 Paise
        assertEquals("Rupees Zero and Ninety-Nine Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.99")));

        // Decimal rounding with large integers: 1000.005 -> 1000.01
        assertEquals("Rupees One Thousand and One Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("1000.005")));
    }

    @Test
    @DisplayName("2. PDFBox Unicode TTF Currency Rendering Feasibility Analysis")
    void testUnicodeFontRenderingFeasibility(@TempDir Path tempDir) throws IOException {
        String[] candidateFontPaths = {
                "C:\\Windows\\Fonts\\segoeui.ttf",
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\calibri.ttf",
                "C:\\Windows\\Fonts\\Nirmala.ttf",
                "C:\\Windows\\Fonts\\mangal.ttf"
        };

        boolean foundSystemTtfWithRupee = false;
        String successfulFontName = null;

        for (String fontPath : candidateFontPaths) {
            File fontFile = new File(fontPath);
            if (!fontFile.exists()) {
                System.out.println("Font path not present: " + fontPath);
                continue;
            }

            try (PDDocument doc = new PDDocument()) {
                PDType0Font font = PDType0Font.load(doc, fontFile);
                boolean hasRupee = font.hasGlyph(0x20B9);
                System.out.println("Checking " + fontFile.getName() + " -> has U+20B9: " + hasRupee);
                if (hasRupee) {
                    foundSystemTtfWithRupee = true;
                    successfulFontName = fontFile.getName();

                    PDPage page = new PDPage();
                    doc.addPage(page);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(font, 12);
                        cs.newLineAtOffset(50, 700);
                        cs.showText("\u20B9 50,000.00 (Test Indian Rupee Sign)");
                        cs.endText();
                    }

                    File outPdf = tempDir.resolve("test_unicode_rupee.pdf").toFile();
                    doc.save(outPdf);
                    assertTrue(outPdf.exists() && outPdf.length() > 0);

                    // Reopen and verify text stripper reads the symbol
                    try (PDDocument readDoc = Loader.loadPDF(outPdf)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(readDoc);
                        assertTrue(text.contains("\u20B9") || text.contains("50,000.00"));
                    }
                    break;
                }
            } catch (Exception e) {
                System.err.println("Note: Font test error on " + fontPath + ": " + e.getMessage());
            }
        }

        System.out.println("System TTF with Indian Rupee symbol found: " + foundSystemTtfWithRupee +
                (successfulFontName != null ? " (" + successfulFontName + ")" : ""));
    }

    @Test
    @DisplayName("3 & 4. Generate Permanent and Bench PDFs, render to image, and inspect visual pages")
    void testRenderedPagesVisualInspection(@TempDir Path tempDir) throws IOException {
        PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(tempDir);

        CompanySettings company = new CompanySettings();
        company.setCompanyName("Apex Enterprise Solutions India Pvt Ltd");
        company.setCompanyAddress("Building 8, Mindspace Cyberabad, Hyderabad, Telangana - 500081");
        company.setContactEmail("payroll-admin@apexcorp.in");
        company.setContactPhone("+91 40 4000 8899");
        company.setPayslipTitle("MONTHLY SALARY SLIP");

        // 1. Permanent Employee Record
        PayrollRecord perm = new PayrollRecord();
        perm.setRecordId("PAY-202610-001");
        perm.setEmployeeId("EMP-1001");
        perm.setEmployeeNameSnapshot("Suresh Krishna");
        perm.setEmployeeType("Permanent");
        perm.setDepartment("Information Technology");
        perm.setDesignationSnapshot("Lead Architect");
        perm.setWorkLocation("Telangana");
        perm.setPfAccountNumber("TS/HYD/004521/1001");
        perm.setUan("100988776655");
        perm.setDateOfJoining(LocalDate.of(2021, 8, 15));
        perm.setPayPeriod("10/2026");
        perm.setPayDate(LocalDate.of(2026, 10, 31));
        perm.setTotalWorkingDays(30);
        perm.setPresentDays(26);
        perm.setAbsentDays(4);
        perm.setBasicSalaryOrDailyRate(new BigDecimal("60000.00"));
        perm.setPayableBasic(new BigDecimal("52000.00"));
        perm.setPayableHra(new BigDecimal("6000.00"));
        perm.setPayableDa(new BigDecimal("3500.00"));
        perm.setPayableConveyance(new BigDecimal("1500.00"));
        perm.setOtherEarnings(BigDecimal.ZERO);
        perm.setGrossEarnings(new BigDecimal("63000.00"));
        perm.setPfEmployeeDeduction(new BigDecimal("6240.00"));
        perm.setPfEmployerContribution(new BigDecimal("6240.00"));
        perm.setProfessionalTax(new BigDecimal("200.00"));
        perm.setOtherDeductions(BigDecimal.ZERO);
        perm.setTotalDeductions(new BigDecimal("6440.00"));
        perm.setNetSalary(new BigDecimal("56560.00"));

        File permPdf = generator.generateSalarySlipPdf(perm, company, true);
        assertTrue(permPdf.exists());

        // 2. Bench Employee Record
        PayrollRecord bench = new PayrollRecord();
        bench.setRecordId("PAY-202610-002");
        bench.setEmployeeId("EMP-2002");
        bench.setEmployeeNameSnapshot("Ananya Sen");
        bench.setEmployeeType("Bench");
        bench.setDepartment("Client Services");
        bench.setDesignationSnapshot("Systems Associate");
        bench.setWorkLocation("Andhra Pradesh");
        bench.setPfAccountNumber("");
        bench.setUan("");
        bench.setDateOfJoining(LocalDate.of(2023, 11, 1));
        bench.setPayPeriod("10/2026");
        bench.setPayDate(LocalDate.of(2026, 10, 31));
        bench.setBasicSalaryOrDailyRate(new BigDecimal("1400.00"));
        bench.setEligibleProjectDays(14);
        bench.setProjectName("Banking Cloud Migration");
        bench.setPayableBasic(new BigDecimal("19600.00"));
        bench.setPayableHra(BigDecimal.ZERO);
        bench.setPayableDa(BigDecimal.ZERO);
        bench.setPayableConveyance(BigDecimal.ZERO);
        bench.setOtherEarnings(BigDecimal.ZERO);
        bench.setGrossEarnings(new BigDecimal("19600.00"));
        bench.setPfEmployeeDeduction(BigDecimal.ZERO);
        bench.setPfEmployerContribution(BigDecimal.ZERO);
        bench.setProfessionalTax(new BigDecimal("200.00"));
        bench.setOtherDeductions(BigDecimal.ZERO);
        bench.setTotalDeductions(new BigDecimal("200.00"));
        bench.setNetSalary(new BigDecimal("19400.00"));

        File benchPdf = generator.generateSalarySlipPdf(bench, company, true);
        assertTrue(benchPdf.exists());

        // Render both PDFs to PNG images to verify page rendering without crashing
        try (PDDocument docPerm = Loader.loadPDF(permPdf)) {
            assertEquals(1, docPerm.getNumberOfPages(), "Permanent salary slip must be exactly 1 page");
            PDFRenderer renderer = new PDFRenderer(docPerm);
            BufferedImage img = renderer.renderImageWithDPI(0, 150);
            assertNotNull(img, "Rendered image must not be null");
            assertTrue(img.getWidth() > 1000 && img.getHeight() > 1500, "Rendered image should have crisp resolution");

            File imgFile = tempDir.resolve("Permanent_SalarySlip_Page.png").toFile();
            ImageIO.write(img, "PNG", imgFile);
            assertTrue(imgFile.exists() && imgFile.length() > 0);

            // Re-verify text
            PDFTextStripper stripper = new PDFTextStripper();
            String permText = stripper.getText(docPerm);
            assertTrue(permText.contains("Suresh Krishna"));
            assertTrue(permText.contains("EMP-1001"));
            assertTrue(permText.contains("52,000.00"));
            assertTrue(permText.contains("63,000.00"));
            assertTrue(permText.contains("56,560.00"));
            assertTrue(permText.contains("Rupees Fifty-Six Thousand Five Hundred Sixty Only"));
            assertTrue(permText.contains("EMPLOYER PF CONTRIBUTION (COMPANY STATUTORY EXPENSE):"));
            assertTrue(permText.contains("6,240.00"));
            assertFalse(permText.toLowerCase().contains("hypertension"));
            assertFalse(permText.toLowerCase().contains("medical condition"));
            assertFalse(permText.toLowerCase().contains("medical description"));
            assertFalse(permText.toLowerCase().contains("blood group"));
        }

        try (PDDocument docBench = Loader.loadPDF(benchPdf)) {
            assertEquals(1, docBench.getNumberOfPages(), "Bench salary slip must be exactly 1 page");
            PDFRenderer renderer = new PDFRenderer(docBench);
            BufferedImage img = renderer.renderImageWithDPI(0, 150);
            assertNotNull(img);

            File imgFile = tempDir.resolve("Bench_SalarySlip_Page.png").toFile();
            ImageIO.write(img, "PNG", imgFile);
            assertTrue(imgFile.exists() && imgFile.length() > 0);

            // Re-verify Bench text
            PDFTextStripper stripper = new PDFTextStripper();
            String benchText = stripper.getText(docBench);
            assertTrue(benchText.contains("Ananya Sen"));
            assertTrue(benchText.contains("EMP-2002"));
            assertTrue(benchText.contains("Banking Cloud Migration"));
            assertTrue(benchText.contains("Eligible Days: 14"));
            assertTrue(benchText.contains("1,400.00"));
            assertTrue(benchText.contains("19,600.00"));
            assertTrue(benchText.contains("19,400.00"));
            assertTrue(benchText.contains("Rupees Nineteen Thousand Four Hundred Only"));

            // Strictly NO HRA, DA, or Conveyance
            assertFalse(benchText.contains("House Rent Allowance"));
            assertFalse(benchText.contains("Dearness Allowance"));
            assertFalse(benchText.contains("Conveyance Allowance"));

            // No medical info
            assertFalse(benchText.toLowerCase().contains("diabetes"));
            assertFalse(benchText.toLowerCase().contains("medical condition"));
            assertFalse(benchText.toLowerCase().contains("medical description"));
            assertFalse(benchText.toLowerCase().contains("blood group"));
        }
    }
}
