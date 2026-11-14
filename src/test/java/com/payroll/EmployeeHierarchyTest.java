package com.payroll;

import com.payroll.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeHierarchyTest {

    @Test
    @DisplayName("OOP Concept 1: INHERITANCE verification")
    void testInheritanceHierarchy() {
        PermanentEmployee perm = new PermanentEmployee();
        BenchEmployee bench = new BenchEmployee();

        // Both subclasses inherit all base Employee members
        assertTrue(perm instanceof Employee, "PermanentEmployee must be an instance of Employee");
        assertTrue(bench instanceof Employee, "BenchEmployee must be an instance of Employee");

        perm.setFirstName("John");
        perm.setLastName("Doe");
        assertEquals("John Doe", perm.getFullName(), "Subclass inherits getFullName() from Employee");

        bench.setFirstName("Jane");
        bench.setLastName("Smith");
        assertEquals("Jane Smith", bench.getFullName(), "Subclass inherits getFullName() from Employee");
    }

    @Test
    @DisplayName("OOP Concept 2: METHOD OVERRIDING verification")
    void testMethodOverriding() {
        PermanentEmployee perm = new PermanentEmployee();
        BenchEmployee bench = new BenchEmployee();

        // Overridden getEmployeeCategory()
        assertEquals("Permanent", perm.getEmployeeCategory());
        assertEquals("Bench", bench.getEmployeeCategory());

        // Overridden HRA calculation: Permanent calculates HRA, Bench ALWAYS returns 0
        DesignationPolicy policy = new DesignationPolicy();
        policy.setHra(new BigDecimal("5000.00"));
        policy.setMonthlyBasicSalary(new BigDecimal("30000.00"));
        policy.setDailyRate(new BigDecimal("1000.00"));

        AttendanceRecord att = new AttendanceRecord("E1", "10-2023", 20, 20);

        // Permanent gets full HRA
        assertEquals(new BigDecimal("5000.00"), perm.calculatePayableHra(att, policy));

        // Bench STRICTLY overrides HRA to zero
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), bench.calculatePayableHra(att, policy));
    }

    @Test
    @DisplayName("OOP Concept 3: RUNTIME POLYMORPHISM verification")
    void testRuntimePolymorphism() {
        // Polymorphic collection: parent Employee reference holding different concrete instances
        Employee permEmp = new PermanentEmployee(
                "Arun", "Kumar", "EMP01", LocalDate.of(1992, 1, 1),
                "arun@corp.com", "9876543210", "Married", "Hyderabad",
                "IT", "Senior Developer", "Telangana", LocalDate.of(2019, 1, 1),
                true, "PF01", "UAN01", true, false, null
        );

        Employee benchEmp = new BenchEmployee(
                "Priya", "Nair", "BENCH01", LocalDate.of(1995, 3, 3),
                "priya@corp.com", "9123456789", "Single", "Vijayawada",
                "IT", "Senior Developer", "Andhra Pradesh", LocalDate.of(2022, 2, 2),
                true, "PF02", "UAN02", true, false, null
        );

        List<Employee> employeeList = List.of(permEmp, benchEmp);

        DesignationPolicy permPolicy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"), new BigDecimal("4000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"), "BASIC", BigDecimal.ZERO, true
        );

        DesignationPolicy benchPolicy = new DesignationPolicy(
                "Senior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("1500.00"),
                true, new BigDecimal("0.12"), new BigDecimal("0.12"), "BASIC", BigDecimal.ZERO, true
        );

        AttendanceRecord attendance = new AttendanceRecord("EMP01", "10-2023", 20, 20);
        ProjectRecord project = new ProjectRecord("BENCH01", "10-2023", "App Alpha", 10);

        // Polymorphic invocation of calculateGrossEarnings on Employee reference:
        // JVM dynamically dispatches to PermanentEmployee for index 0 and BenchEmployee for index 1
        BigDecimal gross0 = employeeList.get(0).calculateGrossEarnings(attendance, null, permPolicy);
        BigDecimal gross1 = employeeList.get(1).calculateGrossEarnings(null, project, benchPolicy);

        // Permanent: 40,000 (basic) + 4,000 (HRA) = 44,000.00
        assertEquals(new BigDecimal("44000.00"), gross0);

        // Bench: 10 days * 1,500 = 15,000.00
        assertEquals(new BigDecimal("15000.00"), gross1);
    }
}

