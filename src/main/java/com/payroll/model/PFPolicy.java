package com.payroll.model;

import java.math.BigDecimal;

/**
 * Encapsulates administrator-configured Provident Fund (PF) parameters.
 * 
 * Rules:
 * - PF is NOT automatically enabled by default.
 * - No assumed statutory rates (e.g. 12%) are hardcoded.
 * - Requires explicit administrator configuration before calculating PF when marked applicable.
 */
public class PFPolicy {

    private boolean defaultApplicable = false;
    private BigDecimal employeeContributionRate = null; // Must be configured by administrator
    private BigDecimal employerContributionRate = null; // Must be configured by administrator
    private String wageBaseType = "BASIC_DA"; // "BASIC", "BASIC_DA", or "GROSS"
    private BigDecimal wageCeiling = null;
    private boolean enforceWageCeiling = false;
    private boolean configured = false;

    public PFPolicy() {
    }

    public PFPolicy(boolean defaultApplicable, BigDecimal employeeContributionRate,
                    BigDecimal employerContributionRate, String wageBaseType,
                    BigDecimal wageCeiling, boolean enforceWageCeiling) {
        this.defaultApplicable = defaultApplicable;
        this.employeeContributionRate = employeeContributionRate;
        this.employerContributionRate = employerContributionRate;
        this.wageBaseType = wageBaseType != null ? wageBaseType : "BASIC_DA";
        this.wageCeiling = wageCeiling;
        this.enforceWageCeiling = enforceWageCeiling;
        this.configured = employeeContributionRate != null && employerContributionRate != null;
    }

    public boolean isConfigured() {
        return configured || (employeeContributionRate != null && employerContributionRate != null);
    }

    public boolean isDefaultApplicable() {
        return defaultApplicable;
    }

    public void setDefaultApplicable(boolean defaultApplicable) {
        this.defaultApplicable = defaultApplicable;
    }

    public BigDecimal getEmployeeContributionRate() {
        return employeeContributionRate;
    }

    public void setEmployeeContributionRate(BigDecimal employeeContributionRate) {
        this.employeeContributionRate = employeeContributionRate;
        this.configured = (this.employeeContributionRate != null && this.employerContributionRate != null);
    }

    public BigDecimal getEmployerContributionRate() {
        return employerContributionRate;
    }

    public void setEmployerContributionRate(BigDecimal employerContributionRate) {
        this.employerContributionRate = employerContributionRate;
        this.configured = (this.employeeContributionRate != null && this.employerContributionRate != null);
    }

    public String getWageBaseType() {
        return wageBaseType;
    }

    public void setWageBaseType(String wageBaseType) {
        this.wageBaseType = wageBaseType;
    }

    public BigDecimal getWageCeiling() {
        return wageCeiling;
    }

    public void setWageCeiling(BigDecimal wageCeiling) {
        this.wageCeiling = wageCeiling;
    }

    public boolean isEnforceWageCeiling() {
        return enforceWageCeiling;
    }

    public void setEnforceWageCeiling(boolean enforceWageCeiling) {
        this.enforceWageCeiling = enforceWageCeiling;
    }

    @Override
    public String toString() {
        return "PFPolicy{" +
                "applicable=" + defaultApplicable +
                ", configured=" + isConfigured() +
                ", employeeRate=" + employeeContributionRate +
                ", employerRate=" + employerContributionRate +
                ", wageBaseType='" + wageBaseType + '\'' +
                '}';
    }
}
