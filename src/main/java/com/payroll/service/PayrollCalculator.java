package com.payroll.service;

import com.payroll.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Primary calculation engine for monthly payroll processing.
 * Demonstrates:
 * 1. RUNTIME POLYMORPHISM: Operates on an abstract Employee reference without instanceof checks.
 * 2. ACCURACY: Uses BigDecimal throughout with RoundingMode.HALF_UP.
 * 3. HISTORICAL INTEGRITY: Constructs a self-contained PayrollRecord preserving snapshots of all inputs.
 */
public class PayrollCalculator {

    private final PFCalculator pfCalculator;
    private final ProfessionalTaxCalculator ptCalculator;

    public PayrollCalculator() {
        this.pfCalculator = new PFCalculator();
        this.ptCalculator = new ProfessionalTaxCalculator();
    }

    public PayrollCalculator(PFCalculator pfCalculator, ProfessionalTaxCalculator ptCalculator) {
        this.pfCalculator = pfCalculator != null ? pfCalculator : new PFCalculator();
        this.ptCalculator = ptCalculator != null ? ptCalculator : new ProfessionalTaxCalculator();
    }

    /**
     * Polymorphically processes payroll for any Employee (Permanent or Bench).
     *
     * @param employee The employee instance (PermanentEmployee or BenchEmployee)
     * @param policy The applicable designation policy
     * @param attendance The attendance record (mandatory for Permanent, optional for Bench)
     * @param project The project record (mandatory for Bench, optional for Permanent)
     * @param payPeriod Pay period string (e.g., "2026-11" or "10-2023")
     * @param payDate Date of disbursement
     * @return Fully calculated, snapshot-preserved PayrollRecord
     */
    public PayrollRecord calculatePayroll(Employee employee, DesignationPolicy policy,
                                          AttendanceRecord attendance, ProjectRecord project,
                                          String payPeriod, LocalDate payDate) {
        return calculatePayroll(employee, policy, attendance, project, payPeriod, payDate, null);
    }

    /**
     * Overloaded payroll calculation supporting global PF policy evaluation.
     */
    public PayrollRecord calculatePayroll(Employee employee, DesignationPolicy policy,
                                          AttendanceRecord attendance, ProjectRecord project,
                                          String payPeriod, LocalDate payDate, PFPolicy globalPf) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee record cannot be null for payroll calculation.");
        }
        if (employee.getEmployeeId() == null || employee.getEmployeeId().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be null or empty for payroll calculation.");
        }
        if (!employee.isActive()) {
            throw new IllegalStateException("Cannot calculate payroll for inactive employee: " + employee.getEmployeeId());
        }
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy cannot be null for payroll calculation.");
        }
        if (payPeriod == null || payPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException("Pay period cannot be empty.");
        }
        if (payDate == null) {
            throw new IllegalArgumentException("Pay date cannot be null.");
        }

        // 1. Polymorphic validation: Employee subclass verifies attendance/project rules
        employee.validateAttendanceOrProject(attendance, project);

        // 2. Polymorphic earnings calculation via overridden methods on Employee
        BigDecimal payableBasic = employee.calculatePayableBasic(attendance, project, policy);
        BigDecimal payableHra = employee.calculatePayableHra(attendance, policy);
        BigDecimal payableDa = employee.calculatePayableDa(attendance, policy);
        BigDecimal payableConveyance = employee.calculatePayableConveyance(attendance, policy);
        BigDecimal grossEarnings = employee.calculateGrossEarnings(attendance, project, policy);

        BigDecimal otherEarnings = policy.getOtherEarnings() != null ? policy.getOtherEarnings() : BigDecimal.ZERO;

        // 3. PF Calculation (respects global activation, designation eligibility, and employee applicability)
        PFCalculator.PFResult pfResult;
        if (globalPf != null) {
            if (!globalPf.isDefaultApplicable() || !employee.isPfApplicable() || !policy.isPfApplicable()) {
                pfResult = new PFCalculator.PFResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
            } else {
                if (!globalPf.isConfigured() || globalPf.getEmployeeContributionRate() == null || globalPf.getEmployerContributionRate() == null) {
                    throw new IllegalStateException("PF Configuration Error: PF is applicable for employee '" +
                            employee.getEmployeeId() + "', but administrator PF contribution rates have not been configured.");
                }
                pfResult = pfCalculator.calculate(employee, globalPf, payableBasic, payableDa, grossEarnings);
            }
        } else {
            pfResult = pfCalculator.calculate(
                    employee, policy, payableBasic, payableDa, grossEarnings
            );
        }
        BigDecimal employeePf = pfResult.getEmployeeDeduction();
        BigDecimal employerPf = pfResult.getEmployerContribution();

        // 4. Professional Tax Calculation (assessed on actual monthly wages according to state policy)
        BigDecimal pt = ptCalculator.calculateTax(employee.getWorkLocation(), grossEarnings);

        // 5. Other Deductions from Policy
        BigDecimal otherDeductions = policy.getOtherDeductions() != null ? policy.getOtherDeductions() : BigDecimal.ZERO;

        // 6. Total Deductions = Employee PF + Professional Tax + Other Deductions
        // Note: Employer PF is tracked separately and NEVER reduces employee net salary
        BigDecimal totalDeductions = employeePf.add(pt).add(otherDeductions).setScale(2, RoundingMode.HALF_UP);

        // 7. Net Salary = Gross Earnings - Total Deductions
        BigDecimal netSalary = grossEarnings.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

        // 8. Construct immutable Historical Payroll Record Snapshot
        PayrollRecord record = new PayrollRecord();
        String recordId = "PR-" + employee.getEmployeeId() + "-" + payPeriod.replace("/", "-");
        record.setRecordId(recordId);

        // Employee Snapshots
        record.setEmployeeId(employee.getEmployeeId());
        record.setEmployeeNameSnapshot(employee.getFullName());
        record.setDesignationSnapshot(employee.getDesignation());
        record.setDepartment(employee.getDepartment());
        record.setEmployeeType(employee.getEmployeeCategory());
        record.setWorkLocation(employee.getWorkLocation());
        record.setDateOfJoining(employee.getDateOfJoining());
        record.setPfAccountNumber(employee.getPfAccountNumber());
        record.setUan(employee.getUan());

        // Period & Date
        record.setPayPeriod(payPeriod);
        record.setPayDate(payDate);
        record.setGenerationDate(LocalDate.now());

        // Attendance snapshot
        if (attendance != null) {
            record.setTotalWorkingDays(attendance.getTotalWorkingDays());
            record.setPresentDays(attendance.getPresentDays());
            record.setAbsentDays(attendance.getAbsentDays());
        }

        // Project snapshot
        if (project != null) {
            record.setProjectName(project.getProjectName());
            record.setEligibleProjectDays(project.getEligibleProjectDays());
        }

        // Earnings snapshot
        BigDecimal baseRate = "Bench".equalsIgnoreCase(employee.getEmployeeCategory())
                ? policy.getDailyRate()
                : policy.getMonthlyBasicSalary();
        record.setBasicSalaryOrDailyRate(baseRate);
        record.setPayableBasic(payableBasic);
        record.setPayableHra(payableHra);
        record.setPayableDa(payableDa);
        record.setPayableConveyance(payableConveyance);
        record.setOtherEarnings(otherEarnings);
        record.setGrossEarnings(grossEarnings);

        // Deductions snapshot
        record.setPfEmployeeDeduction(employeePf);
        record.setPfEmployerContribution(employerPf);
        record.setProfessionalTax(pt);
        record.setOtherDeductions(otherDeductions);
        record.setTotalDeductions(totalDeductions);

        // Net Salary
        record.setNetSalary(netSalary);

        // Policy Snapshot summary string for historical auditing
        String policySnapshot = String.format("Designation=%s; Type=%s; BaseRate=%s; HRA=%s; DA=%s; Conveyance=%s; PF_App=%s; PFRate=%s; PT_State=%s",
                policy.getDesignationName(), employee.getEmployeeCategory(), baseRate, policy.getHra(), policy.getDa(),
                policy.getConveyance(), employee.isPfApplicable() && policy.isPfApplicable(),
                policy.getEmployeePfRate(), employee.getWorkLocation());
        record.setPolicySnapshot(policySnapshot);

        return record;
    }

    public PFCalculator getPfCalculator() {
        return pfCalculator;
    }

    public ProfessionalTaxCalculator getPtCalculator() {
        return ptCalculator;
    }
}

