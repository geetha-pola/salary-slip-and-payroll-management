package com.payroll.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Utility class providing validation routines for employee registration, payroll, and contact information.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$"); // Indian 10-digit mobile number
    private static final Pattern EMP_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,20}$");

    private ValidationUtil() {
    }

    public static boolean isValidEmployeeId(String empId) {
        return empId != null && EMP_ID_PATTERN.matcher(empId.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidMobile(String mobile) {
        if (mobile == null) return false;
        String cleaned = mobile.replaceAll("[^0-9]", "");
        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2);
        }
        return MOBILE_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isValidDateOfBirth(LocalDate dob) {
        if (dob == null) return false;
        LocalDate today = LocalDate.now();
        if (dob.isAfter(today)) return false;
        // Require employee to be at least 18 years old
        return Period.between(dob, today).getYears() >= 18;
    }

    public static boolean isValidDateOfJoining(LocalDate doj, LocalDate dob) {
        if (doj == null) return false;
        if (dob != null && doj.isBefore(dob.plusYears(18))) {
            return false; // Joining before 18th birthday is invalid
        }
        return true;
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
}

