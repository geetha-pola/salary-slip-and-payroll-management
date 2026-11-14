package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable/Preserved snapshot of an executed payroll transaction.
 * Demonstrates historical integrity: subsequent edits to employee details or designation
 * policies will NEVER alter historical records.
 */
public class PayrollRecord {

    private String recordId;
    private String employeeId;
    private String employeeNameSnapshot;
    private String designationSnapshot;
    private String department;
    private String employeeType; // "Permanent" or "Bench"
    private String workLocation; // "Andhra Pradesh" or "Telangana"
    private LocalDate dateOfJoining;
    private String pfAccountNumber;
    private String uan;

    private String payPeriod; // "MM-yyyy"
    private LocalDate payDate;

    // Attendance Info (Permanent)
    private int totalWorkingDays;
    private int presentDays;
    private int absentDays;

    // Project Info (Bench)
    private String projectName;
    private int eligibleProjectDays;

    // Earnings
    private BigDecimal basicSalaryOrDailyRate = BigDecimal.ZERO;
    private BigDecimal payableBasic = BigDecimal.ZERO;
    private BigDecimal payableHra = BigDecimal.ZERO;
    private BigDecimal payableDa = BigDecimal.ZERO;
    private BigDecimal payableConveyance = BigDecimal.ZERO;
    private BigDecimal otherEarnings = BigDecimal.ZERO;
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    // Deductions
    private BigDecimal pfEmployeeDeduction = BigDecimal.ZERO;
    private BigDecimal pfEmployerContribution = BigDecimal.ZERO; // Kept separate, NOT deducted from net
    private BigDecimal professionalTax = BigDecimal.ZERO;
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Net Payable
    private BigDecimal netSalary = BigDecimal.ZERO;

    // Metadata & Audit Trail
    private String policySnapshot;
    private LocalDate generationDate = LocalDate.now();
    private String status = "ORIGINAL"; // "ORIGINAL", "CORRECTED", "SUPERSEDED"
    private String originalRecordId = "";
    private String correctionReason = "";
    private String correctionTimestamp = "";

    public PayrollRecord() {
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeNameSnapshot() {
        return employeeNameSnapshot;
    }

    public void setEmployeeNameSnapshot(String employeeNameSnapshot) {
        this.employeeNameSnapshot = employeeNameSnapshot;
    }

    public String getDesignationSnapshot() {
        return designationSnapshot;
    }

    public void setDesignationSnapshot(String designationSnapshot) {
        this.designationSnapshot = designationSnapshot;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getPfAccountNumber() {
        return pfAccountNumber;
    }

    public void setPfAccountNumber(String pfAccountNumber) {
        this.pfAccountNumber = pfAccountNumber;
    }

    public String getUan() {
        return uan;
    }

    public void setUan(String uan) {
        this.uan = uan;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }

    public int getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(int totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }

    public int getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(int absentDays) {
        this.absentDays = absentDays;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getEligibleProjectDays() {
        return eligibleProjectDays;
    }

    public void setEligibleProjectDays(int eligibleProjectDays) {
        this.eligibleProjectDays = eligibleProjectDays;
    }

    public BigDecimal getBasicSalaryOrDailyRate() {
        return basicSalaryOrDailyRate;
    }

    public void setBasicSalaryOrDailyRate(BigDecimal basicSalaryOrDailyRate) {
        this.basicSalaryOrDailyRate = basicSalaryOrDailyRate;
    }

    public BigDecimal getPayableBasic() {
        return payableBasic;
    }

    public void setPayableBasic(BigDecimal payableBasic) {
        this.payableBasic = payableBasic;
    }

    public BigDecimal getPayableHra() {
        return payableHra;
    }

    public void setPayableHra(BigDecimal payableHra) {
        this.payableHra = payableHra;
    }

    public BigDecimal getPayableDa() {
        return payableDa;
    }

    public void setPayableDa(BigDecimal payableDa) {
        this.payableDa = payableDa;
    }

    public BigDecimal getPayableConveyance() {
        return payableConveyance;
    }

    public void setPayableConveyance(BigDecimal payableConveyance) {
        this.payableConveyance = payableConveyance;
    }

    public BigDecimal getOtherEarnings() {
        return otherEarnings;
    }

    public void setOtherEarnings(BigDecimal otherEarnings) {
        this.otherEarnings = otherEarnings;
    }

    public BigDecimal getGrossEarnings() {
        return grossEarnings;
    }

    public void setGrossEarnings(BigDecimal grossEarnings) {
        this.grossEarnings = grossEarnings;
    }

    public BigDecimal getPfEmployeeDeduction() {
        return pfEmployeeDeduction;
    }

    public void setPfEmployeeDeduction(BigDecimal pfEmployeeDeduction) {
        this.pfEmployeeDeduction = pfEmployeeDeduction;
    }

    public BigDecimal getPfEmployerContribution() {
        return pfEmployerContribution;
    }

    public void setPfEmployerContribution(BigDecimal pfEmployerContribution) {
        this.pfEmployerContribution = pfEmployerContribution;
    }

    public BigDecimal getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(BigDecimal professionalTax) {
        this.professionalTax = professionalTax;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public String getPolicySnapshot() {
        return policySnapshot;
    }

    public void setPolicySnapshot(String policySnapshot) {
        this.policySnapshot = policySnapshot;
    }

    public LocalDate getGenerationDate() {
        return generationDate;
    }

    public void setGenerationDate(LocalDate generationDate) {
        this.generationDate = generationDate;
    }

    public String getStatus() {
        return status != null ? status : "ORIGINAL";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOriginal() {
        return "ORIGINAL".equalsIgnoreCase(getStatus());
    }

    public boolean isCorrected() {
        return "CORRECTED".equalsIgnoreCase(getStatus());
    }

    public boolean isSuperseded() {
        return "SUPERSEDED".equalsIgnoreCase(getStatus());
    }

    public String getOriginalRecordId() {
        return originalRecordId != null ? originalRecordId : "";
    }

    public void setOriginalRecordId(String originalRecordId) {
        this.originalRecordId = originalRecordId;
    }

    public String getCorrectionReason() {
        return correctionReason != null ? correctionReason : "";
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }

    public String getCorrectionTimestamp() {
        return correctionTimestamp != null ? correctionTimestamp : "";
    }

    public void setCorrectionTimestamp(String correctionTimestamp) {
        this.correctionTimestamp = correctionTimestamp;
    }

    @Override
    public String toString() {
        return "PayrollRecord{" +
                "id='" + recordId + '\'' +
                ", empId='" + employeeId + '\'' +
                ", period='" + payPeriod + '\'' +
                ", gross=" + grossEarnings +
                ", deductions=" + totalDeductions +
                ", net=" + netSalary +
                '}';
    }
}

