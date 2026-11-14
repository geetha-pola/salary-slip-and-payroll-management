package com.payroll.model;

import java.math.BigDecimal;

/**
 * Encapsulates designation-specific salary and deduction policies.
 * Permanent and Bench policies are distinct even when sharing the same designation name.
 * 
 * Rules:
 * - PF is NOT enabled by default.
 * - Contribution rates are not assumed statutory values; they must be explicitly configured by an administrator.
 * - Employee and Employer PF contribution rates are maintained separately.
 */
public class DesignationPolicy {

    private String designationName;
    private String employeeCategory; // "Permanent" or "Bench"

    // Permanent Employee Salary Components (Fixed Rupee Values)
    private BigDecimal monthlyBasicSalary = BigDecimal.ZERO;
    private BigDecimal hra = BigDecimal.ZERO;
    private BigDecimal da = BigDecimal.ZERO;
    private BigDecimal conveyance = BigDecimal.ZERO;
    private BigDecimal otherEarnings = BigDecimal.ZERO;

    // Bench Employee Daily Salary Rate (Fixed Rupee Value)
    private BigDecimal dailyRate = BigDecimal.ZERO;

    // Provident Fund (PF) Policy - Administrator Configurable
    private boolean pfApplicable = false; // NOT automatically enabled
    private BigDecimal employeePfRate = null; // Requires explicit admin configuration
    private BigDecimal employerPfRate = null; // Requires explicit admin configuration
    private String pfWageBase = "BASIC_DA"; // "BASIC", "BASIC_DA", or "GROSS"

    // Other Deductions
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    // Status
    private boolean active = true;

    public DesignationPolicy() {
    }

    public DesignationPolicy(String designationName, String employeeCategory,
                             BigDecimal monthlyBasicSalary, BigDecimal hra, BigDecimal da,
                             BigDecimal conveyance, BigDecimal otherEarnings, BigDecimal dailyRate,
                             boolean pfApplicable, BigDecimal employeePfRate, BigDecimal employerPfRate,
                             String pfWageBase, BigDecimal otherDeductions, boolean active) {
        this.designationName = designationName;
        this.employeeCategory = employeeCategory;
        this.monthlyBasicSalary = monthlyBasicSalary != null ? monthlyBasicSalary : BigDecimal.ZERO;
        this.hra = hra != null ? hra : BigDecimal.ZERO;
        this.da = da != null ? da : BigDecimal.ZERO;
        this.conveyance = conveyance != null ? conveyance : BigDecimal.ZERO;
        this.otherEarnings = otherEarnings != null ? otherEarnings : BigDecimal.ZERO;
        this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
        this.pfApplicable = pfApplicable;
        this.employeePfRate = employeePfRate;
        this.employerPfRate = employerPfRate;
        this.pfWageBase = pfWageBase != null ? pfWageBase : "BASIC_DA";
        this.otherDeductions = otherDeductions != null ? otherDeductions : BigDecimal.ZERO;
        this.active = active;
    }

    public boolean isPfConfigured() {
        return employeePfRate != null && employerPfRate != null;
    }

    public String getDesignationName() {
        return designationName;
    }

    public void setDesignationName(String designationName) {
        this.designationName = designationName;
    }

    public String getEmployeeCategory() {
        return employeeCategory;
    }

    public void setEmployeeCategory(String employeeCategory) {
        this.employeeCategory = employeeCategory;
    }

    public BigDecimal getMonthlyBasicSalary() {
        return monthlyBasicSalary;
    }

    public void setMonthlyBasicSalary(BigDecimal monthlyBasicSalary) {
        this.monthlyBasicSalary = monthlyBasicSalary;
    }

    public BigDecimal getHra() {
        return hra;
    }

    public void setHra(BigDecimal hra) {
        this.hra = hra;
    }

    public BigDecimal getDa() {
        return da;
    }

    public void setDa(BigDecimal da) {
        this.da = da;
    }

    public BigDecimal getConveyance() {
        return conveyance;
    }

    public void setConveyance(BigDecimal conveyance) {
        this.conveyance = conveyance;
    }

    public BigDecimal getOtherEarnings() {
        return otherEarnings;
    }

    public void setOtherEarnings(BigDecimal otherEarnings) {
        this.otherEarnings = otherEarnings;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public boolean isPfApplicable() {
        return pfApplicable;
    }

    public void setPfApplicable(boolean pfApplicable) {
        this.pfApplicable = pfApplicable;
    }

    public BigDecimal getEmployeePfRate() {
        return employeePfRate;
    }

    public void setEmployeePfRate(BigDecimal employeePfRate) {
        this.employeePfRate = employeePfRate;
    }

    public BigDecimal getEmployerPfRate() {
        return employerPfRate;
    }

    public void setEmployerPfRate(BigDecimal employerPfRate) {
        this.employerPfRate = employerPfRate;
    }

    public String getPfWageBase() {
        return pfWageBase;
    }

    public void setPfWageBase(String pfWageBase) {
        this.pfWageBase = pfWageBase;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "DesignationPolicy{" +
                "designationName='" + designationName + '\'' +
                ", category='" + employeeCategory + '\'' +
                ", monthlyBasic=" + monthlyBasicSalary +
                ", dailyRate=" + dailyRate +
                ", pfApplicable=" + pfApplicable +
                ", active=" + active +
                '}';
    }
}
