package com.payroll.service;

import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.util.CurrencyFormatter;
import com.payroll.util.NumberToWordsConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Service for generating professional, printable PDF salary slips using Apache PDFBox 3.0.2.
 * 
 * Strict Guarantees:
 * 1. Professional corporate visual hierarchy with company branding, employee details, earnings & deductions.
 * 2. Permanent employees: full pro-rated breakdown (Basic, HRA, DA, Conveyance, Gross, Deductions, Net).
 * 3. Bench employees: daily basic rate, eligible project days, project name. STRICTLY NO HRA, DA, or Conveyance rows.
 * 4. Net salary rendered numerically and in Indian Rupee words (Crores, Lakhs, Thousands, Hundreds, Paise).
 * 5. Employer PF contribution noted separately as a company statutory expense, never reducing net pay.
 * 6. STRICT CONFIDENTIALITY: Medical condition, medical description, and sensitive health info are NEVER included.
 * 7. File overwrite protection: prevents silent replacement without explicit permission.
 */
public class PdfSalarySlipGenerator {

    private final Path outputDir;

    public PdfSalarySlipGenerator() {
        this(Paths.get("payslips"));
    }

    public PdfSalarySlipGenerator(Path outputDir) {
        this.outputDir = outputDir;
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create salary slips directory: " + e.getMessage());
        }
    }

    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * Resolves default target file for a payroll record.
     */
    public File getDefaultTargetFile(PayrollRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Payroll record cannot be null.");
        }
        String safePeriod = record.getPayPeriod().replace("/", "-").trim();
        if (record.isSuperseded() || "SUPERSEDED".equalsIgnoreCase(record.getStatus())) {
            File specificFile = outputDir.resolve("SalarySlip_" + record.getEmployeeId() + "_" + safePeriod + "_SUPERSEDED.pdf").toFile();
            if (specificFile.exists()) return specificFile;
            File standardFile = outputDir.resolve("SalarySlip_" + record.getEmployeeId() + "_" + safePeriod + ".pdf").toFile();
            if (standardFile.exists()) return standardFile;
            return specificFile;
        } else if (record.isCorrected() || "CORRECTED".equalsIgnoreCase(record.getStatus())) {
            File corrFile = outputDir.resolve("SalarySlip_" + record.getEmployeeId() + "_" + safePeriod + "_CORRECTED.pdf").toFile();
            if (corrFile.exists()) return corrFile;
            File standardFile = outputDir.resolve("SalarySlip_" + record.getEmployeeId() + "_" + safePeriod + ".pdf").toFile();
            if (standardFile.exists()) return standardFile;
            return corrFile;
        }
        String fileName = "SalarySlip_" + record.getEmployeeId() + "_" + safePeriod + ".pdf";
        return outputDir.resolve(fileName).toFile();
    }

    /**
     * Generates a salary slip PDF for a given PayrollRecord and CompanySettings into the default output directory.
     * Prevents overwriting if file already exists.
     */
    public File generateSalarySlipPdf(PayrollRecord record, CompanySettings company) throws IOException {
        return generateSalarySlipPdf(record, company, false);
    }

    /**
     * Generates a salary slip PDF for a given PayrollRecord with explicit overwrite control.
     */
    public File generateSalarySlipPdf(PayrollRecord record, CompanySettings company, boolean allowOverwrite) throws IOException {
        File targetFile = getDefaultTargetFile(record);
        return generateSalarySlipPdf(record, company, targetFile, allowOverwrite);
    }

    /**
     * Generates a salary slip PDF and writes it to a specific target file.
     * Checks overwrite permissions if target file exists.
     */
    public File generateSalarySlipPdf(PayrollRecord record, CompanySettings company, File targetFile, boolean allowOverwrite) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("Payroll record cannot be null for PDF generation.");
        }
        if (company == null) {
            company = new CompanySettings();
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Target file destination cannot be null.");
        }

        if (targetFile.exists() && !allowOverwrite) {
            throw new IllegalStateException("Salary slip already exists: " + targetFile.getName() + ". Overwrite not allowed without user confirmation.");
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4); // Standard A4: 595.28 x 841.89 points
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                renderDocument(cs, record, company);
            }

            document.save(targetFile);
        }

        return targetFile;
    }

    private void renderDocument(PDPageContentStream cs, PayrollRecord rec, CompanySettings company) throws IOException {
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        float pageWidth = PDRectangle.A4.getWidth();
        float margin = 40f;
        float contentWidth = pageWidth - (2 * margin);
        float y = 800f;

        // 1. Company Header Box & Title
        drawFilledRect(cs, margin, y - 56, contentWidth, 62, new Color(248, 250, 252));
        drawRectBorder(cs, margin, y - 56, contentWidth, 62, new Color(226, 232, 240), 1.0f);

        // Company Name
        drawText(cs, fontBold, 14, margin + 14, y - 14, company.getCompanyName(), new Color(30, 41, 59));

        // Company Address
        drawText(cs, fontRegular, 8f, margin + 14, y - 26, company.getCompanyAddress(), new Color(100, 116, 139));

        // Company Contacts
        String contactLine = "Email: " + company.getContactEmail() + "  |  Phone: " + company.getContactPhone();
        drawText(cs, fontRegular, 8f, margin + 14, y - 37, contactLine, new Color(100, 116, 139));

        // Payslip Title
        String title = (company.getPayslipTitle() != null ? company.getPayslipTitle() : "MONTHLY SALARY SLIP") +
                " - " + rec.getPayPeriod().toUpperCase();
        drawText(cs, fontBold, 9.5f, margin + 14, y - 49, title, new Color(37, 99, 235));

        y -= 72;

        // 2. Employee Information Grid Box
        drawFilledRect(cs, margin, y - 95, contentWidth, 95, new Color(255, 255, 255));
        drawRectBorder(cs, margin, y - 95, contentWidth, 95, new Color(203, 213, 225), 1.0f);

        // Header strip for employee details
        drawFilledRect(cs, margin, y - 18, contentWidth, 18, new Color(241, 245, 249));
        drawLine(cs, margin, y - 18, margin + contentWidth, y - 18, new Color(203, 213, 225), 1.0f);
        drawText(cs, fontBold, 9.5f, margin + 10, y - 13, "EMPLOYEE IDENTIFICATION & ATTENDANCE SUMMARY", new Color(30, 41, 59));

        float col1LabelX = margin + 12;
        float col1ValX = margin + 105;
        float col2LabelX = margin + 270;
        float col2ValX = margin + 375;
        float rowSpacing = 14f;
        float curY = y - 32;

        // Row 1
        drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Employee Name:", new Color(100, 116, 139));
        drawText(cs, fontBold, 8.5f, col1ValX, curY, rec.getEmployeeNameSnapshot(), new Color(15, 23, 42));

        drawText(cs, fontRegular, 8.5f, col2LabelX, curY, "Pay Period / Date:", new Color(100, 116, 139));
        String payDateStr = rec.getPayDate() != null ? rec.getPayDate().toString() : "N/A";
        drawText(cs, fontBold, 8.5f, col2ValX, curY, rec.getPayPeriod() + " (" + payDateStr + ")", new Color(15, 23, 42));

        curY -= rowSpacing;

        // Row 2
        drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Employee ID:", new Color(100, 116, 139));
        drawText(cs, fontBold, 8.5f, col1ValX, curY, rec.getEmployeeId(), new Color(15, 23, 42));

        drawText(cs, fontRegular, 8.5f, col2LabelX, curY, "Category / Location:", new Color(100, 116, 139));
        drawText(cs, fontBold, 8.5f, col2ValX, curY, rec.getEmployeeType() + " | " + rec.getWorkLocation(), new Color(15, 23, 42));

        curY -= rowSpacing;

        // Row 3
        drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Designation:", new Color(100, 116, 139));
        drawText(cs, fontRegular, 8.5f, col1ValX, curY, rec.getDesignationSnapshot(), new Color(15, 23, 42));

        drawText(cs, fontRegular, 8.5f, col2LabelX, curY, "PF A/C & UAN:", new Color(100, 116, 139));
        String pfUan = (rec.getPfAccountNumber() != null && !rec.getPfAccountNumber().isEmpty() ? rec.getPfAccountNumber() : "None") +
                " / " + (rec.getUan() != null && !rec.getUan().isEmpty() ? rec.getUan() : "None");
        drawText(cs, fontRegular, 8.5f, col2ValX, curY, pfUan, new Color(15, 23, 42));

        curY -= rowSpacing;

        // Row 4
        drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Department:", new Color(100, 116, 139));
        drawText(cs, fontRegular, 8.5f, col1ValX, curY, rec.getDepartment() != null ? rec.getDepartment() : "General", new Color(15, 23, 42));

        drawText(cs, fontRegular, 8.5f, col2LabelX, curY, "Date of Joining:", new Color(100, 116, 139));
        drawText(cs, fontRegular, 8.5f, col2ValX, curY, rec.getDateOfJoining() != null ? rec.getDateOfJoining().toString() : "N/A", new Color(15, 23, 42));

        curY -= rowSpacing;

        // Row 5: Attendance (Permanent vs Bench)
        boolean isBench = "Bench".equalsIgnoreCase(rec.getEmployeeType());
        if (!isBench) {
            drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Attendance Days:", new Color(100, 116, 139));
            String attText = "Total: " + rec.getTotalWorkingDays() + "  |  Present: " + rec.getPresentDays() + "  |  Absent / LOP: " + rec.getAbsentDays();
            drawText(cs, fontBold, 8.5f, col1ValX, curY, attText, new Color(15, 23, 42));
        } else {
            drawText(cs, fontRegular, 8.5f, col1LabelX, curY, "Project Allocation:", new Color(100, 116, 139));
            String projName = (rec.getProjectName() != null && !rec.getProjectName().isEmpty()) ? rec.getProjectName() : "Unallocated";
            String projText = projName + "  |  Eligible Days: " + rec.getEligibleProjectDays() +
                    "  |  Daily Rate: " + formatPdfRupees(rec.getBasicSalaryOrDailyRate());
            drawText(cs, fontBold, 8.5f, col1ValX, curY, projText, new Color(15, 23, 42));
        }

        y -= 110;

        // 3. Earnings & Deductions Table
        float tableHeight = 165f;
        drawFilledRect(cs, margin, y - tableHeight, contentWidth, tableHeight, new Color(255, 255, 255));
        drawRectBorder(cs, margin, y - tableHeight, contentWidth, tableHeight, new Color(203, 213, 225), 1.0f);

        // Middle vertical separator dividing Earnings and Deductions
        float midX = margin + (contentWidth / 2f);
        drawLine(cs, midX, y, midX, y - tableHeight, new Color(203, 213, 225), 1.0f);

        // Table Header
        drawFilledRect(cs, margin, y - 20, contentWidth, 20, new Color(241, 245, 249));
        drawLine(cs, margin, y - 20, margin + contentWidth, y - 20, new Color(203, 213, 225), 1.0f);

        drawText(cs, fontBold, 9f, margin + 12, y - 14, "EARNINGS", new Color(30, 41, 59));
        drawTextRight(cs, fontBold, 9f, midX - 14, y - 14, "AMOUNT (INR)", new Color(30, 41, 59));

        drawText(cs, fontBold, 9f, midX + 14, y - 14, "DEDUCTIONS", new Color(30, 41, 59));
        drawTextRight(cs, fontBold, 9f, margin + contentWidth - 14, y - 14, "AMOUNT (INR)", new Color(30, 41, 59));

        float earnRowY = y - 36;
        float rowH = 18f;

        if (!isBench) {
            // PERMANENT EMPLOYEE: Full pro-rated breakdown
            // Row 1: Basic
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY, "Basic Salary (Payable)", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY, formatPdfRupees(rec.getPayableBasic()), new Color(15, 23, 42));

            // Row 2: HRA
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY - rowH, "House Rent Allowance (HRA)", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY - rowH, formatPdfRupees(rec.getPayableHra()), new Color(15, 23, 42));

            // Row 3: DA
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY - (2 * rowH), "Dearness Allowance (DA)", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY - (2 * rowH), formatPdfRupees(rec.getPayableDa()), new Color(15, 23, 42));

            // Row 4: Conveyance
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY - (3 * rowH), "Conveyance Allowance", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY - (3 * rowH), formatPdfRupees(rec.getPayableConveyance()), new Color(15, 23, 42));

            // Row 5: Other Earnings
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY - (4 * rowH), "Other Earnings", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY - (4 * rowH), formatPdfRupees(rec.getOtherEarnings()), new Color(15, 23, 42));

        } else {
            // BENCH EMPLOYEE: Daily rate basic pay ONLY. Strictly NO HRA, DA, or Conveyance rows.
            // Row 1: Basic
            String benchBasicLabel = "Basic Pay (Daily Rate x Eligible Days)";
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY, benchBasicLabel, new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY, formatPdfRupees(rec.getPayableBasic()), new Color(15, 23, 42));

            // Row 2: Project Deployment Details
            String benchProj = "Project: " + (rec.getProjectName() != null && !rec.getProjectName().isEmpty() ? rec.getProjectName() : "Unassigned");
            drawText(cs, fontOblique, 8f, margin + 12, earnRowY - rowH, benchProj, new Color(100, 116, 139));
            drawTextRight(cs, fontRegular, 8f, midX - 14, earnRowY - rowH, rec.getEligibleProjectDays() + " days @ " + formatPdfRupees(rec.getBasicSalaryOrDailyRate()), new Color(100, 116, 139));

            // Row 3: Other Earnings
            drawText(cs, fontRegular, 8.5f, margin + 12, earnRowY - (2 * rowH), "Other Earnings", new Color(51, 65, 85));
            drawTextRight(cs, fontRegular, 8.5f, midX - 14, earnRowY - (2 * rowH), formatPdfRupees(rec.getOtherEarnings()), new Color(15, 23, 42));

            // Row 4: Notice of Bench Policy
            drawText(cs, fontOblique, 7.5f, margin + 12, earnRowY - (3 * rowH), "[Bench Category: Allowances Not Applicable]", new Color(148, 163, 184));
        }

        // Deductions Column (Same structure for both)
        // Deductions Row 1: Employee PF
        drawText(cs, fontRegular, 8.5f, midX + 14, earnRowY, "Employee PF Contribution", new Color(51, 65, 85));
        drawTextRight(cs, fontRegular, 8.5f, margin + contentWidth - 14, earnRowY, formatPdfRupees(rec.getPfEmployeeDeduction()), new Color(15, 23, 42));

        // Deductions Row 2: PT
        drawText(cs, fontRegular, 8.5f, midX + 14, earnRowY - rowH, "Professional Tax (PT)", new Color(51, 65, 85));
        drawTextRight(cs, fontRegular, 8.5f, margin + contentWidth - 14, earnRowY - rowH, formatPdfRupees(rec.getProfessionalTax()), new Color(15, 23, 42));

        // Deductions Row 3: Other Deductions
        drawText(cs, fontRegular, 8.5f, midX + 14, earnRowY - (2 * rowH), "Other Deductions", new Color(51, 65, 85));
        drawTextRight(cs, fontRegular, 8.5f, margin + contentWidth - 14, earnRowY - (2 * rowH), formatPdfRupees(rec.getOtherDeductions()), new Color(15, 23, 42));

        // Totals Footer Row in Table
        float footerTop = y - tableHeight + 24f;
        drawFilledRect(cs, margin, y - tableHeight, contentWidth, 24, new Color(248, 250, 252));
        drawLine(cs, margin, footerTop, margin + contentWidth, footerTop, new Color(203, 213, 225), 1.0f);

        drawText(cs, fontBold, 9f, margin + 12, y - tableHeight + 8, "GROSS EARNINGS:", new Color(30, 41, 59));
        drawTextRight(cs, fontBold, 9.5f, midX - 14, y - tableHeight + 8, formatPdfRupees(rec.getGrossEarnings()), new Color(37, 99, 235));

        drawText(cs, fontBold, 9f, midX + 14, y - tableHeight + 8, "TOTAL DEDUCTIONS:", new Color(30, 41, 59));
        drawTextRight(cs, fontBold, 9.5f, margin + contentWidth - 14, y - tableHeight + 8, formatPdfRupees(rec.getTotalDeductions()), new Color(220, 38, 38));

        y -= (tableHeight + 16);

        // 4. Net Salary Highlight Box
        float netBoxHeight = 52f;
        drawFilledRect(cs, margin, y - netBoxHeight, contentWidth, netBoxHeight, new Color(239, 246, 255));
        drawRectBorder(cs, margin, y - netBoxHeight, contentWidth, netBoxHeight, new Color(191, 219, 254), 1.5f);

        drawText(cs, fontBold, 11f, margin + 16, y - 20, "NET PAYABLE SALARY:", new Color(30, 58, 138));
        String netFormatted = formatPdfRupees(rec.getNetSalary());
        drawTextRight(cs, fontBold, 17f, margin + contentWidth - 18, y - 25, netFormatted, new Color(30, 58, 138));

        // Net salary in words
        String inWords = NumberToWordsConverter.convertToIndianCurrencyWords(rec.getNetSalary());
        drawText(cs, fontOblique, 8.5f, margin + 16, y - 38, "In Words: " + inWords, new Color(71, 85, 105));

        y -= (netBoxHeight + 16);

        // 5. Employer Statutory Contribution Box (Strict Separation Requirement)
        float empPfHeight = 36f;
        drawFilledRect(cs, margin, y - empPfHeight, contentWidth, empPfHeight, new Color(248, 250, 252));
        drawRectBorder(cs, margin, y - empPfHeight, contentWidth, empPfHeight, new Color(226, 232, 240), 1.0f);

        drawText(cs, fontBold, 9f, margin + 14, y - 16, "EMPLOYER PF CONTRIBUTION (COMPANY STATUTORY EXPENSE):", new Color(51, 65, 85));
        drawTextRight(cs, fontBold, 10f, margin + contentWidth - 16, y - 16, formatPdfRupees(rec.getPfEmployerContribution()), new Color(30, 41, 59));
        drawText(cs, fontRegular, 8f, margin + 14, y - 28, "Statutory Notice: Employer PF is paid directly by the company and is never deducted from employee net salary.", new Color(100, 116, 139));

        y -= (empPfHeight + 35);

        // 6. Footer & Legal Disclaimer
        drawLine(cs, margin, y + 10, margin + contentWidth, y + 10, new Color(226, 232, 240), 1.0f);

        String genDate = rec.getGenerationDate() != null ? rec.getGenerationDate().toString() : LocalDate.now().toString();
        drawText(cs, fontRegular, 7.5f, margin, y - 4, "System Record ID: " + rec.getRecordId() + "  |  Generated: " + genDate, new Color(148, 163, 184));
        drawTextRight(cs, fontRegular, 7.5f, margin + contentWidth, y - 4, "Computer-Generated Statement (No Physical Signature Required)", new Color(148, 163, 184));

        // Confidentiality Notice (Explicitly confirms absence of medical details)
        drawText(cs, fontOblique, 7f, margin, y - 16, "Confidentiality: For employee recipient only. Diagnostic and health data are strictly excluded.", new Color(148, 163, 184));
        drawTextRight(cs, fontRegular, 7f, margin + contentWidth, y - 16, "Page 1 of 1", new Color(148, 163, 184));
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String text, Color color) throws IOException {
        if (text == null) text = "";
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawTextRight(PDPageContentStream cs, PDType1Font font, float fontSize, float rightX, float y, String text, Color color) throws IOException {
        if (text == null) text = "";
        float stringWidth = font.getStringWidth(text) / 1000f * fontSize;
        drawText(cs, font, fontSize, rightX - stringWidth, y, text, color);
    }

    private void drawFilledRect(PDPageContentStream cs, float x, float y, float width, float height, Color fillColor) throws IOException {
        cs.saveGraphicsState();
        cs.setNonStrokingColor(fillColor);
        cs.addRect(x, y, width, height);
        cs.fill();
        cs.restoreGraphicsState();
    }

    private void drawRectBorder(PDPageContentStream cs, float x, float y, float width, float height, Color borderColor, float lineWidth) throws IOException {
        cs.saveGraphicsState();
        cs.setStrokingColor(borderColor);
        cs.setLineWidth(lineWidth);
        cs.addRect(x, y, width, height);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color strokeColor, float lineWidth) throws IOException {
        cs.saveGraphicsState();
        cs.setStrokingColor(strokeColor);
        cs.setLineWidth(lineWidth);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    /**
     * Formats currency using "Rs. " for clean WinAnsiEncoding rendering in PDFBox standard fonts.
     */
    private String formatPdfRupees(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return "Rs. " + CurrencyFormatter.format(amount);
    }
}
