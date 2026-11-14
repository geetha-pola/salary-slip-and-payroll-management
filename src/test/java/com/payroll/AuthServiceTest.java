package com.payroll;

import com.payroll.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    @DisplayName("Authentication succeeds with valid demonstration credentials")
    void testValidCredentials() {
        AuthService.AuthResult result = authService.authenticate("1234", "5678");
        assertTrue(result.isSuccess());
        assertTrue(authService.isLoggedIn());
        assertEquals("1234", authService.getCurrentUser());
    }

    @Test
    @DisplayName("Authentication fails with invalid Employee ID")
    void testInvalidEmployeeId() {
        AuthService.AuthResult result = authService.authenticate("9999", "5678");
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertTrue(result.getMessage().contains("Invalid Employee ID"));
    }

    @Test
    @DisplayName("Authentication fails with invalid password")
    void testInvalidPassword() {
        AuthService.AuthResult result = authService.authenticate("1234", "wrongpass");
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertTrue(result.getMessage().contains("Incorrect password"));
    }

    @Test
    @DisplayName("Authentication fails when Employee ID is empty")
    void testEmptyEmployeeId() {
        AuthService.AuthResult result = authService.authenticate("", "5678");
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertTrue(result.getMessage().contains("Employee ID cannot be empty"));
    }

    @Test
    @DisplayName("Authentication fails when password is empty")
    void testEmptyPassword() {
        AuthService.AuthResult result = authService.authenticate("1234", "   ");
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertTrue(result.getMessage().contains("Password cannot be empty"));
    }

    @Test
    @DisplayName("Logout clears session and user state")
    void testLogoutClearsSession() {
        authService.authenticate("1234", "5678");
        assertTrue(authService.isLoggedIn());

        authService.logout();
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUser());
    }
}

