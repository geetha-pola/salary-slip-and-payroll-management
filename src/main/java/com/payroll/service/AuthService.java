package com.payroll.service;

/**
 * Centralized authentication service managing demo credentials and session state.
 * 
 * Demonstration Credentials:
 * - Employee ID: 1234
 * - Password:    5678
 * 
 * Note: These are fixed demonstration credentials configured for the PS-10 hackathon.
 */
public class AuthService {

    public static final String DEMO_EMPLOYEE_ID = "1234";
    public static final String DEMO_PASSWORD = "5678";

    private boolean loggedIn = false;
    private String currentUser = null;

    public static class AuthResult {
        private final boolean success;
        private final String message;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Authenticates credentials against the centralized demo credentials.
     */
    public synchronized AuthResult authenticate(String employeeId, String password) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return new AuthResult(false, "Employee ID cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            return new AuthResult(false, "Password cannot be empty.");
        }

        String cleanId = employeeId.trim();
        if (!DEMO_EMPLOYEE_ID.equals(cleanId)) {
            return new AuthResult(false, "Invalid Employee ID. Please enter authorized credentials.");
        }

        if (!DEMO_PASSWORD.equals(password)) {
            return new AuthResult(false, "Incorrect password. Please verify and try again.");
        }

        this.loggedIn = true;
        this.currentUser = cleanId;
        return new AuthResult(true, "Authentication successful. Welcome to the portal.");
    }

    public synchronized void logout() {
        this.loggedIn = false;
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getCurrentUser() {
        return currentUser;
    }
}

