package com.payroll.service;

import com.payroll.model.DesignationPolicy;
import com.payroll.model.Employee;
import com.payroll.model.PFPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating Employee Provident Fund (PF) deductions and Employer contributions.
 * 
 * Strict Enforcement:
 * 1. PF is NOT automatically enabled.
 * 2. Statutory PF rates are NOT assumed or hardcoded.
 * 3. Whenever PF is marked applicable, valid administrator-configured contribution rates
 *    are MANDATORY. Calculating PF without explicit configuration throws an IllegalStateException.
 * 4. If PF is marked not applicable (either at employee level or policy level), deduction is ₹0.00.
 * 5. Employee PF deduction and Employer PF contribution are strictly separate. Employer PF
 *    never reduces employee net pay.
 */
public class PFCalculator {

    public static class PFResult {
        private final BigDecimal wageBase;
        private final BigDecimal employeeDeduction;
        private final BigDecimal employerContribution;
        private final boolean applicable;

        public PFResult(BigDecimal wageBase, BigDecimal employeeDeduction, BigDecimal employerContribution, boolean applicable) {
            this.wageBase = wageBase.setScale(2, RoundingMode.HALF_UP);
            this.employeeDeduction = employeeDeduction.setScale(2, RoundingMode.HALF_UP);
            this.employerContribution = employerContribution.setScale(2, RoundingMode.HALF_UP);
            this.applicable = applicable;
        }

        public BigDecimal getWageBase() {
            return wageBase;
        }

        public BigDecimal getEmployeeDeduction() {
            return employeeDeduction;
        }

        public BigDecimal getEmployerContribution() {
            return employerContribution;
        }

        public boolean isApplicable() {
            return applicable;
        }
    }

    /**
     * Calculates PF contributions for an employee.
     * Throws IllegalStateException if PF is applicable but contribution rates are unconfigured.
     */
    public PFResult calculate(Employee employee, DesignationPolicy policy,
                              BigDecimal payableBasic, BigDecimal payableDa, BigDecimal grossEarnings) {
        if (employee == null || policy == null) {
            throw new IllegalArgumentException("Employee and DesignationPolicy must not be null for PF calculation.");
        }

        // Rule: If marked not applicable at either employee level or policy level, PF is ₹0.00.
        if (!employee.isPfApplicable() || !policy.isPfApplicable()) {
            return new PFResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        // Rule: Require explicit administrator configuration whenever PF is applicable
        if (policy.getEmployeePfRate() == null || policy.getEmployerPfRate() == null) {
            throw new IllegalStateException("PF Configuration Error: PF is marked applicable for employee '" +
                    employee.getEmployeeId() + "', but administrator PF contribution rates have not been configured for designation '" +
                    policy.getDesignationName() + "' (" + policy.getEmployeeCategory() + "). Please configure PF rates in Settings before processing payroll.");
        }

        BigDecimal employeeRate = policy.getEmployeePfRate();
        BigDecimal employerRate = policy.getEmployerPfRate();

        if (employeeRate.compareTo(BigDecimal.ZERO) < 0 || employerRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("PF contribution rates cannot be negative. Employee rate: " + employeeRate + ", Employer rate: " + employerRate);
        }

        // Determine wage base according to configured policy
        BigDecimal wageBase = determineWageBase(policy.getPfWageBase(), payableBasic, payableDa, grossEarnings);

        BigDecimal employeeDeduction = wageBase.multiply(employeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerContribution = wageBase.multiply(employerRate).setScale(2, RoundingMode.HALF_UP);

        return new PFResult(wageBase, employeeDeduction, employerContribution, true);
    }

    /**
     * Overload using a global PFPolicy when designation policy rates are deferred to global config.
     */
    public PFResult calculate(Employee employee, PFPolicy globalPolicy,
                              BigDecimal payableBasic, BigDecimal payableDa, BigDecimal grossEarnings) {
        if (employee == null || globalPolicy == null) {
            throw new IllegalArgumentException("Employee and PFPolicy must not be null for PF calculation.");
        }

        if (!employee.isPfApplicable() || !globalPolicy.isDefaultApplicable()) {
            return new PFResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        if (!globalPolicy.isConfigured() || globalPolicy.getEmployeeContributionRate() == null || globalPolicy.getEmployerContributionRate() == null) {
            throw new IllegalStateException("Global PF Configuration Error: PF is applicable for employee '" +
                    employee.getEmployeeId() + "', but global PF contribution rates have not been configured by an administrator.");
        }

        BigDecimal wageBase = determineWageBase(globalPolicy.getWageBaseType(), payableBasic, payableDa, grossEarnings);

        if (globalPolicy.isEnforceWageCeiling() && globalPolicy.getWageCeiling() != null) {
            if (wageBase.compareTo(globalPolicy.getWageCeiling()) > 0) {
                wageBase = globalPolicy.getWageCeiling();
            }
        }

        BigDecimal employeeRate = globalPolicy.getEmployeeContributionRate();
        BigDecimal employerRate = globalPolicy.getEmployerContributionRate();

        BigDecimal employeeDeduction = wageBase.multiply(employeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerContribution = wageBase.multiply(employerRate).setScale(2, RoundingMode.HALF_UP);

        return new PFResult(wageBase, employeeDeduction, employerContribution, true);
    }

    /**
     * Computes the wage base based on the configured component selector.
     */
    public BigDecimal determineWageBase(String wageBaseType, BigDecimal basic, BigDecimal da, BigDecimal gross) {
        BigDecimal safeBasic = basic != null ? basic : BigDecimal.ZERO;
        BigDecimal safeDa = da != null ? da : BigDecimal.ZERO;
        BigDecimal safeGross = gross != null ? gross : BigDecimal.ZERO;

        if (wageBaseType == null) {
            return safeBasic.add(safeDa);
        }

        return switch (wageBaseType.trim().toUpperCase()) {
            case "BASIC" -> safeBasic;
            case "GROSS" -> safeGross;
            case "BASIC_DA" -> safeBasic.add(safeDa);
            default -> safeBasic.add(safeDa);
        };
    }
}
