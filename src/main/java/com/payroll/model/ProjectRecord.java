package com.payroll.model;

/**
 * Encapsulates project authorization and working days for Bench employees.
 */
public class ProjectRecord {

    private String employeeId;
    private String payPeriod;
    private String projectName; // Project name or project reference
    private int eligibleProjectDays;

    public ProjectRecord() {
    }

    public ProjectRecord(String employeeId, String payPeriod, String projectName, int eligibleProjectDays) {
        this.employeeId = employeeId;
        this.payPeriod = payPeriod;
        this.projectName = projectName;
        this.eligibleProjectDays = eligibleProjectDays;
        validate();
    }

    public void validate() {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name or reference is required for bench payroll processing.");
        }
        if (eligibleProjectDays < 0 || eligibleProjectDays > 31) {
            throw new IllegalArgumentException("Eligible project days must be between 0 and 31 days. Provided: " + eligibleProjectDays);
        }
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
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
        validate();
    }

    @Override
    public String toString() {
        return "ProjectRecord{" +
                "employeeId='" + employeeId + '\'' +
                ", payPeriod='" + payPeriod + '\'' +
                ", projectName='" + projectName + '\'' +
                ", eligibleProjectDays=" + eligibleProjectDays +
                '}';
    }
}

