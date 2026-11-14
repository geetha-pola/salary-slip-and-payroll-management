package com.payroll;

import com.payroll.model.BenchEmployee;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.Employee;
import com.payroll.model.PermanentEmployee;
import com.payroll.service.DesignationPolicyManager;
import com.payroll.service.EmployeeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeValidationTest {

    private DesignationPolicyManager policyManager;
    private EmployeeManager employeeManager;

    @BeforeEach
    void setUp() {
        policyManager = new DesignationPolicyManager();
        employeeManager = new EmployeeManager(policyManager);
    }

    private PermanentEmployee createValidPermanent(String id) {
        return new PermanentEmployee(
                "Venkatesh", "Prasad", id, LocalDate.of(1996, 4, 12),
                "venkatesh@company.com", "9876543210", "Married", "Hyderabad",
                "Engineering", "Senior Developer", "Telangana",
                LocalDate.of(2020, 1, 10), true, "PF001", "UAN001",
                true, false, null
        );
    }

    private BenchEmployee createValidBench(String id) {
        return new BenchEmployee(
                "Sneha", "Kaur", id, LocalDate.of(1998, 9, 15),
                "sneha@company.com", "9123456780", "Single", "Vijayawada",
                "Engineering", "Junior Developer", "Andhra Pradesh",
                LocalDate.of(2022, 3, 1), true, "PF002", "UAN002",
                true, false, null
        );
    }

    @Test
    @DisplayName("Successfully register valid Permanent and Bench employees")
    void testSuccessfulRegistration() {
        PermanentEmployee perm = createValidPermanent("EMP100");
        BenchEmployee bench = createValidBench("BENCH100");

        employeeManager.registerEmployee(perm);
        employeeManager.registerEmployee(bench);

        assertEquals(2, employeeManager.getAllEmployees().size());
        assertTrue(employeeManager.findById("EMP100").isPresent());
        assertTrue(employeeManager.findById("BENCH100").isPresent());
    }

    @Test
    @DisplayName("Registration rejects duplicate Employee ID")
    void testDuplicateEmployeeIdRejection() {
        employeeManager.registerEmployee(createValidPermanent("EMP100"));

        PermanentEmployee duplicate = createValidPermanent("EMP100");
        duplicate.setFirstName("Other");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeManager.registerEmployee(duplicate);
        });
        assertTrue(ex.getMessage().contains("Duplicate Employee ID"));
    }

    @Test
    @DisplayName("Registration rejects invalid email format")
    void testInvalidEmailRejection() {
        PermanentEmployee emp = createValidPermanent("EMP101");
        emp.setEmail("not-an-email");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeManager.registerEmployee(emp);
        });
        assertTrue(ex.getMessage().contains("Invalid email"));
    }

    @Test
    @DisplayName("Registration rejects invalid mobile number")
    void testInvalidMobileRejection() {
        PermanentEmployee emp = createValidPermanent("EMP102");
        emp.setMobileNumber("12345"); // Invalid length and prefix

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeManager.registerEmployee(emp);
        });
        assertTrue(ex.getMessage().contains("Invalid Indian 10-digit mobile"));
    }

    @Test
    @DisplayName("Registration rejects underage employee (less than 18 years)")
    void testUnderageDateOfBirthRejection() {
        PermanentEmployee emp = createValidPermanent("EMP103");
        emp.setDateOfBirth(LocalDate.now().minusYears(16)); // 16 years old

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeManager.registerEmployee(emp);
        });
        assertTrue(ex.getMessage().contains("Employee must be at least 18 years old"));
    }

    @Test
    @DisplayName("Registration rejects inactive designation")
    void testInactiveDesignationRejection() {
        // Deactivate Senior Developer for Permanent
        policyManager.deactivatePolicy("Senior Developer", "Permanent");

        PermanentEmployee emp = createValidPermanent("EMP104");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeManager.registerEmployee(emp);
        });
        assertTrue(ex.getMessage().contains("currently inactive"));
    }

    @Test
    @DisplayName("Directory search and filtering by query, department and location")
    void testDirectoryFiltering() {
        employeeManager.registerEmployee(createValidPermanent("EMP101"));
        employeeManager.registerEmployee(createValidBench("BENCH102"));

        // Search by query
        List<Employee> queryResult = employeeManager.filterEmployees("Venkatesh", "All", "All", "All", "All");
        assertEquals(1, queryResult.size());
        assertEquals("EMP101", queryResult.get(0).getEmployeeId());

        // Filter by location
        List<Employee> apResult = employeeManager.filterEmployees("", "All", "All", "All", "Andhra Pradesh");
        assertEquals(1, apResult.size());
        assertEquals("BENCH102", apResult.get(0).getEmployeeId());
    }
}

