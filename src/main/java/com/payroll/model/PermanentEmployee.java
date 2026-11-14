package com.payroll.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Concrete Employee subclass representing a Permanent Employee.
 * Demonstrates:
 * 1. INHERITANCE: Extends abstract Employee.
 * 2. METHOD OVERRIDING: Implements monthly attendance-factored compensation.
 * 3. POLYMORPHISM: Can be referenced via Employee and dispatched dynamically.
 */
public class PermanentEmployee extends Employee {

    public PermanentEmployee() {
        super();
    }

    public PermanentEmployee(String firstName, String lastName, String employeeId, LocalDate dateOfBirth,
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
        return "Permanent";
    }

    @Override
    public void validateAttendanceOrProject(AttendanceRecord attendance, ProjectRecord project) {
        if (attendance == null) {
            throw new IllegalArgumentException("Attendance record is required for Permanent employee payroll processing.");
        }
        attendance.validate();
    }

    @Override
    public BigDecimal calculatePayableBasic(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        BigDecimal attendanceFactor = attendance.getAttendanceFactor();
        return policy.getMonthlyBasicSalary()
                .multiply(attendanceFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculatePayableHra(AttendanceRecord attendance, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        BigDecimal attendanceFactor = attendance.getAttendanceFactor();
        return policy.getHra()
                .multiply(attendanceFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculatePayableDa(AttendanceRecord attendance, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        BigDecimal attendanceFactor = attendance.getAttendanceFactor();
        return policy.getDa()
                .multiply(attendanceFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculatePayableConveyance(AttendanceRecord attendance, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        BigDecimal attendanceFactor = attendance.getAttendanceFactor();
        return policy.getConveyance()
                .multiply(attendanceFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateGrossEarnings(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Designation policy is required.");
        }
        BigDecimal totalFixed = policy.getMonthlyBasicSalary()
                .add(policy.getHra() != null ? policy.getHra() : BigDecimal.ZERO)
                .add(policy.getDa() != null ? policy.getDa() : BigDecimal.ZERO)
                .add(policy.getConveyance() != null ? policy.getConveyance() : BigDecimal.ZERO)
                .add(policy.getOtherEarnings() != null ? policy.getOtherEarnings() : BigDecimal.ZERO);
        BigDecimal attendanceFactor = attendance.getAttendanceFactor();
        return totalFixed.multiply(attendanceFactor).setScale(2, RoundingMode.HALF_UP);
    }
}

