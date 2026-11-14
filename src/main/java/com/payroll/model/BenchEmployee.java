package com.payroll.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Concrete Employee subclass representing a Bench Employee.
 * Demonstrates:
 * 1. INHERITANCE: Extends abstract Employee.
 * 2. METHOD OVERRIDING: Implements daily-rate earnings for eligible project days only.
 *    Strictly overrides HRA, DA, and Conveyance to ZERO as required by business rules.
 * 3. POLYMORPHISM: Can be processed through an Employee reference without instanceof checks.
 */
public class BenchEmployee extends Employee {

    public BenchEmployee() {
        super();
    }

    public BenchEmployee(String firstName, String lastName, String employeeId, LocalDate dateOfBirth,
                         String email, String mobileNumber, String maritalStatus, String address,
                         String department, String designation, String workLocation,
                         LocalDate dateOfJoining, boolean active, String pfAccountNumber,
                         String uan, boolean pfApplicable, boolean medicalCondition,
                         String medicalDescription) {
        super(firstName, lastName, employeeId, dateOfBirth, email, mobileNumber, maritalStatus,
                address, department, designation, workLocation, dateOfJoining, active,
                pfAccountNumber, uan, pfApplicable, medicalCondition, medicalDescription);
    }

    @Override
    public String getEmployeeCategory() {
        return "Bench";
    }

    @Override
    public void validateAttendanceOrProject(AttendanceRecord attendance, ProjectRecord project) {
        if (project == null) {
            throw new IllegalArgumentException("Project record is required for Bench employee payroll processing.");
        }
        project.validate();
    }

    @Override
    public BigDecimal calculatePayableBasic(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        if (project == null) {
            throw new IllegalArgumentException("Project record is required.");
        }
        BigDecimal dailyRate = policy.getDailyRate() != null ? policy.getDailyRate() : BigDecimal.ZERO;
        return dailyRate.multiply(BigDecimal.valueOf(project.getEligibleProjectDays()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Strict Business Rule: Bench employees receive NO HRA. Always returns ZERO.
     */
    @Override
    public BigDecimal calculatePayableHra(AttendanceRecord attendance, DesignationPolicy policy) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Strict Business Rule: Bench employees receive NO DA. Always returns ZERO.
     */
    @Override
    public BigDecimal calculatePayableDa(AttendanceRecord attendance, DesignationPolicy policy) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Strict Business Rule: Bench employees receive NO Conveyance. Always returns ZERO.
     */
    @Override
    public BigDecimal calculatePayableConveyance(AttendanceRecord attendance, DesignationPolicy policy) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateGrossEarnings(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy) {
        // Bench gross is strictly daily basic rate * eligible project days
        return calculatePayableBasic(attendance, project, policy);
    }
}

