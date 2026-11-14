package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstract base class representing an Employee in the system.
 * Demonstrates the Object-Oriented concept of INHERITANCE by encapsulating
 * shared attributes and defining polymorphic contracts for concrete employee types.
 */
public abstract class Employee {

    // Personal Information
    private String firstName;
    private String lastName;
    private String employeeId; // Unique identifier
    private LocalDate dateOfBirth;
    private String email;
    private String mobileNumber;
    private String maritalStatus;
    private String address;

    // Employment Information
    private String department;
    private String designation;
    private String workLocation; // "Andhra Pradesh" or "Telangana"
    private LocalDate dateOfJoining;
    private boolean active = true;

    // Payroll Identification Information
    private String pfAccountNumber;
    private String uan;
    private boolean pfApplicable = true;

    // Sensitive Additional Information (Restricted to Admins, never printed on payslips)
    private boolean medicalCondition;
    private String medicalDescription;

    public Employee() {
    }

    public Employee(String firstName, String lastName, String employeeId, LocalDate dateOfBirth,
                    String email, String mobileNumber, String maritalStatus, String address,
                    String department, String designation, String workLocation,
                    LocalDate dateOfJoining, boolean active, String pfAccountNumber,
                    String uan, boolean pfApplicable, boolean medicalCondition,
                    String medicalDescription) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.employeeId = employeeId;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.maritalStatus = maritalStatus;
        this.address = address;
        this.department = department;
        this.designation = designation;
        this.workLocation = workLocation;
        this.dateOfJoining = dateOfJoining;
        this.active = active;
        this.pfAccountNumber = pfAccountNumber;
        this.uan = uan;
        this.pfApplicable = pfApplicable;
        this.medicalCondition = medicalCondition;
        this.medicalDescription = medicalDescription;
    }

    /**
     * Automatically composes full name from first and last names.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) return "";
        if (firstName == null) return lastName.trim();
        if (lastName == null) return firstName.trim();
        return (firstName.trim() + " " + lastName.trim()).trim();
    }

    // --- Abstract Methods (POLYMORPHISM & METHOD OVERRIDING) ---

    /**
     * Returns the employee category identifier ("Permanent" or "Bench").
     */
    public abstract String getEmployeeCategory();

    /**
     * Polymorphically calculates payable basic salary based on attendance/project and policy.
     */
    public abstract BigDecimal calculatePayableBasic(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy);

    /**
     * Polymorphically calculates payable HRA based on attendance and policy.
     */
    public abstract BigDecimal calculatePayableHra(AttendanceRecord attendance, DesignationPolicy policy);

    /**
     * Polymorphically calculates payable DA based on attendance and policy.
     */
    public abstract BigDecimal calculatePayableDa(AttendanceRecord attendance, DesignationPolicy policy);

    /**
     * Polymorphically calculates payable conveyance based on attendance and policy.
     */
    public abstract BigDecimal calculatePayableConveyance(AttendanceRecord attendance, DesignationPolicy policy);

    /**
     * Polymorphically calculates total gross earnings.
     */
    public abstract BigDecimal calculateGrossEarnings(AttendanceRecord attendance, ProjectRecord project, DesignationPolicy policy);

    /**
     * Validates that the supplied attendance or project record matches this employee category's business rules.
     */
    public abstract void validateAttendanceOrProject(AttendanceRecord attendance, ProjectRecord project);

    // --- Getters and Setters ---

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public boolean isPfApplicable() {
        return pfApplicable;
    }

    public void setPfApplicable(boolean pfApplicable) {
        this.pfApplicable = pfApplicable;
    }

    public boolean isMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(boolean medicalCondition) {
        this.medicalCondition = medicalCondition;
    }

    public String getMedicalDescription() {
        return medicalDescription;
    }

    public void setMedicalDescription(String medicalDescription) {
        this.medicalDescription = medicalDescription;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + employeeId + '\'' +
                ", name='" + getFullName() + '\'' +
                ", type='" + getEmployeeCategory() + '\'' +
                ", designation='" + designation + '\'' +
                ", active=" + active +
                '}';
    }
}

