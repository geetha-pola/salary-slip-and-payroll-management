package com.payroll.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Encapsulates attendance details for a monthly pay period.
 * Used primarily for Permanent employee compensation calculations.
 */
public class AttendanceRecord {

    private String employeeId;
    private String payPeriod; // Format: "MM-yyyy" or "Month yyyy"
    private int totalWorkingDays;
    private int presentDays;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String employeeId, String payPeriod, int totalWorkingDays, int presentDays) {
        this.employeeId = employeeId;
        this.payPeriod = payPeriod;
        this.totalWorkingDays = totalWorkingDays;
        this.presentDays = presentDays;
        validate();
    }

    public void validate() {
        if (totalWorkingDays <= 0) {
            throw new IllegalArgumentException("Total working days must be greater than zero. Provided: " + totalWorkingDays);
        }
        if (presentDays < 0) {
            throw new IllegalArgumentException("Present days cannot be negative. Provided: " + presentDays);
        }
        if (presentDays > totalWorkingDays) {
            throw new IllegalArgumentException("Present days (" + presentDays + ") cannot exceed total working days (" + totalWorkingDays + ")");
        }
    }

    public int getAbsentDays() {
        return totalWorkingDays - presentDays;
    }

    /**
     * Calculates the attendance factor (Present Days / Total Working Days).
     * Scale set to 10 decimal places with HALF_UP rounding for intermediate factor accuracy.
     */
    public BigDecimal getAttendanceFactor() {
        if (totalWorkingDays == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(presentDays)
                .divide(BigDecimal.valueOf(totalWorkingDays), 10, RoundingMode.HALF_UP);
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

    public int getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(int totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
        validate();
    }

    public int getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
        validate();
    }

    @Override
    public String toString() {
        return "AttendanceRecord{" +
                "employeeId='" + employeeId + '\'' +
                ", payPeriod='" + payPeriod + '\'' +
                ", totalWorkingDays=" + totalWorkingDays +
                ", presentDays=" + presentDays +
                ", absentDays=" + getAbsentDays() +
                '}';
    }
}

