package com.payroll;

import com.payroll.model.CompanySettings;
import com.payroll.model.PayrollRecord;
import com.payroll.service.PdfSalarySlipGenerator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class GenerateInspectionArtifacts {

    @Test
    void generateAndRenderBothSlips() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        File outputDir = new File("scratch_payslips");
        if (!outputDir.exists()) outputDir.mkdirs();

        PdfSalarySlipGenerator generator = new PdfSalarySlipGenerator(outputDir.toPath());

        CompanySettings company = new CompanySettings();
        company.setCompanyName("GlobalTech Software India Pvt Ltd");
        company.setCompanyAddress("Ascendas IT Park, Taramani, Chennai, Tamil Nadu / Hyderabad Hub - 500081");
        company.setContactEmail("payroll@globaltech.com");
        company.setContactPhone("+91 40 4899 1200");
        company.setPayslipTitle("CONFIDENTIAL SALARY STATEMENT");

        // Permanent Employee (Monthly Basic: 40,000, 25/30 days present)
        PayrollRecord perm = new PayrollRecord();
        perm.setRecordId("REC-PERM-VERIF-01");
        perm.setEmployeeId("EMP-1001");
        perm.setEmployeeNameSnapshot("Vikram Aditya");
        perm.setEmployeeType("Permanent");
        perm.setDepartment("Product Engineering");
        perm.setDesignationSnapshot("Senior Software Engineer");
        perm.setWorkLocation("Telangana");
        perm.setPfAccountNumber("TS/HYD/10045/01");
        perm.setUan("100911223344");
        perm.setDateOfJoining(LocalDate.of(2022, 6, 1));
        perm.setPayPeriod("10/2026");
        perm.setPayDate(LocalDate.of(2026, 10, 31));
        perm.setTotalWorkingDays(30);
        perm.setPresentDays(25);
        perm.setAbsentDays(5);
        perm.setBasicSalaryOrDailyRate(new BigDecimal("40000.00"));
        perm.setPayableBasic(new BigDecimal("33333.33"));
        perm.setPayableHra(new BigDecimal("3333.33"));
        perm.setPayableDa(new BigDecimal("2083.33"));
        perm.setPayableConveyance(new BigDecimal("1000.00"));
        perm.setOtherEarnings(BigDecimal.ZERO);
        perm.setGrossEarnings(new BigDecimal("39750.00"));
        perm.setPfEmployeeDeduction(new BigDecimal("4000.00"));
        perm.setPfEmployerContribution(new BigDecimal("4000.00"));
        perm.setProfessionalTax(new BigDecimal("200.00"));
        perm.setOtherDeductions(BigDecimal.ZERO);
        perm.setTotalDeductions(new BigDecimal("4200.00"));
        perm.setNetSalary(new BigDecimal("35550.00"));

        File permPdf = generator.generateSalarySlipPdf(perm, company, true);
        System.out.println("Generated Permanent PDF: " + permPdf.getAbsolutePath() + " (" + permPdf.length() + " bytes)");

        // Bench Employee (1,500/day, 12 days, Project: Core Banking Modernization)
        PayrollRecord bench = new PayrollRecord();
        bench.setRecordId("REC-BENCH-VERIF-02");
        bench.setEmployeeId("EMP-2001");
        bench.setEmployeeNameSnapshot("Meera Nambiar");
        bench.setEmployeeType("Bench");
        bench.setDepartment("Enterprise Systems");
        bench.setDesignationSnapshot("Associate Consultant");
        bench.setWorkLocation("Andhra Pradesh");
        bench.setPfAccountNumber("");
        bench.setUan("");
        bench.setDateOfJoining(LocalDate.of(2023, 10, 15));
        bench.setPayPeriod("10/2026");
        bench.setPayDate(LocalDate.of(2026, 10, 31));
        bench.setBasicSalaryOrDailyRate(new BigDecimal("1500.00"));
        bench.setEligibleProjectDays(12);
        bench.setProjectName("Core Banking Modernization");
        bench.setPayableBasic(new BigDecimal("18000.00"));
        bench.setPayableHra(BigDecimal.ZERO);
        bench.setPayableDa(BigDecimal.ZERO);
        bench.setPayableConveyance(BigDecimal.ZERO);
        bench.setOtherEarnings(BigDecimal.ZERO);
        bench.setGrossEarnings(new BigDecimal("18000.00"));
        bench.setPfEmployeeDeduction(BigDecimal.ZERO);
        bench.setPfEmployerContribution(BigDecimal.ZERO);
        bench.setProfessionalTax(new BigDecimal("200.00"));
        bench.setOtherDeductions(BigDecimal.ZERO);
        bench.setTotalDeductions(new BigDecimal("200.00"));
        bench.setNetSalary(new BigDecimal("17800.00"));

        File benchPdf = generator.generateSalarySlipPdf(bench, company, true);
        System.out.println("Generated Bench PDF: " + benchPdf.getAbsolutePath() + " (" + benchPdf.length() + " bytes)");

        // Render both to high-resolution PNG images for visual inspection
        try (PDDocument doc = Loader.loadPDF(permPdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage bim = renderer.renderImageWithDPI(0, 150);
            File png = new File(outputDir, "Permanent_SalarySlip_Rendered.png");
            ImageIO.write(bim, "PNG", png);
            System.out.println("Saved Rendered Image: " + png.getAbsolutePath() + " (" + bim.getWidth() + "x" + bim.getHeight() + ")");
        }

        try (PDDocument doc = Loader.loadPDF(benchPdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage bim = renderer.renderImageWithDPI(0, 150);
            File png = new File(outputDir, "Bench_SalarySlip_Rendered.png");
            ImageIO.write(bim, "PNG", png);
            System.out.println("Saved Rendered Image: " + png.getAbsolutePath() + " (" + bim.getWidth() + "x" + bim.getHeight() + ")");
        }
    }
}
