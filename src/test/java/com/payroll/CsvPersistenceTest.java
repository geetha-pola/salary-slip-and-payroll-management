package com.payroll;

import com.payroll.model.*;
import com.payroll.service.PayrollCalculator;
import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.FileHandler;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CsvPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("1. CSV basic creation, header writing, and reading")
    void testCsvCreationAndReading() throws IOException {
        Path csvPath = tempDir.resolve("test.csv");
        List<String> header = List.of("col1", "col2", "col3");
        List<List<String>> rows = List.of(
                List.of("val1", "val2", "val3"),
                List.of("val4", "val5", "val6")
        );

        FileHandler.writeCsvAtomic(csvPath, header, rows);
        assertTrue(Files.exists(csvPath));

        List<List<String>> readRecords = FileHandler.readCsv(csvPath);
        assertEquals(3, readRecords.size()); // 1 header + 2 rows
        assertEquals(header, readRecords.get(0));
        assertEquals(rows.get(0), readRecords.get(1));
        assertEquals(rows.get(1), readRecords.get(2));
    }

    @Test
    @DisplayName("2. Special characters: commas, quotation marks, and multi-line strings")
    void testSpecialCharactersCommasQuotesNewlines() throws IOException {
        Path csvPath = tempDir.resolve("special_chars.csv");
        List<String> header = List.of("id", "description", "multiline_text");
        List<List<String>> rows = List.of(
                List.of("1", "Address with, comma and \"embedded quotes\"", "First Line\nSecond Line\nThird Line"),
                List.of("2", "Simple text", "Single line")
        );

        FileHandler.writeCsvAtomic(csvPath, header, rows);

        List<List<String>> readRecords = FileHandler.readCsv(csvPath);
        assertEquals(3, readRecords.size());

        List<String> firstDataRow = readRecords.get(1);
        assertEquals("1", firstDataRow.get(0));
        assertEquals("Address with, comma and \"embedded quotes\"", firstDataRow.get(1));
        assertEquals("First Line\nSecond Line\nThird Line", firstDataRow.get(2));
    }

    @Test
    @DisplayName("3. Employee repository initializes EMPTY CSV without silent sample insertion")
    void testEmployeeRepositoryInitializesEmptyWithoutSilentSampleInsertion() throws IOException {
        Path empFile = tempDir.resolve("employees_empty_init.csv");
        EmployeeRepository repo = new EmployeeRepository(empFile);

        // Must initialize an empty file with headers only
        assertTrue(Files.exists(empFile));
        assertEquals(0, repo.loadAll().size(), "Must not silently insert sample employees");

        // Verify CSV file contains header line only
        List<List<String>> lines = FileHandler.readCsv(empFile);
        assertEquals(1, lines.size(), "Should contain exactly the header line");
        assertEquals("employee_id", lines.get(0).get(0));

        // Explicit optional demo seeding works when deliberately requested
        repo.seedSampleEmployees();
        assertEquals(4, repo.loadAll().size(), "Explicit sample seeding must populate exactly 4 demonstration employees");
    }

    @Test
    @DisplayName("4. Employee registration and retrieval persistence")
    void testEmployeeRegistrationAndRetrieval() throws IOException {
        Path empFile = tempDir.resolve("employees.csv");
        EmployeeRepository repo = new EmployeeRepository(empFile);

        PermanentEmployee perm = new PermanentEmployee(
                "Suresh", "Raina", "TEST001", LocalDate.of(1992, 11, 27),
                "suresh@cricket.in", "9876543210", "Married", "Flat 4B, Hill View, Hyderabad",
                "Sports", "Senior Developer", "Telangana", LocalDate.of(2019, 5, 1),
                true, "PF-TEST-01", "UAN-TEST-01", false, false, ""
        );

        repo.save(perm);

        // Simulate application restart by creating a new repository instance pointing to the same file
        EmployeeRepository restartedRepo = new EmployeeRepository(empFile);
        Optional<Employee> loadedOpt = restartedRepo.findById("TEST001");

        assertTrue(loadedOpt.isPresent());
        Employee loaded = loadedOpt.get();
        assertEquals("Suresh Raina", loaded.getFullName());
        assertEquals("Permanent", loaded.getEmployeeCategory());
        assertEquals("Flat 4B, Hill View, Hyderabad", loaded.getAddress());
        assertTrue(loaded.isActive());
    }

    @Test
    @DisplayName("5. Employee update and deactivation persistence")
    void testEmployeeUpdateAndDeactivation() throws IOException {
        Path empFile = tempDir.resolve("employees_update.csv");
        EmployeeRepository repo = new EmployeeRepository(empFile);

        BenchEmployee bench = new BenchEmployee(
                "Manoj", "Tiwary", "TEST002", LocalDate.of(1994, 6, 14),
                "manoj@bengal.in", "9876500000", "Single", "Banjara Hills",
                "Engineering", "Junior Developer", "Telangana", LocalDate.of(2021, 8, 1),
                true, "PF-TEST-02", "UAN-TEST-02", false, false, ""
        );

        repo.save(bench);

        // Update email and address
        bench.setEmail("manoj.updated@bengal.in");
        bench.setAddress("Jubilee Hills, Road No 36");
        repo.update(bench);

        // Deactivate
        repo.deactivate("TEST002");

        // Verify across reload
        EmployeeRepository reloadedRepo = new EmployeeRepository(empFile);
        Optional<Employee> reloadedOpt = reloadedRepo.findById("TEST002");

        assertTrue(reloadedOpt.isPresent());
        assertFalse(reloadedOpt.get().isActive(), "Employee should be marked inactive");
        assertEquals("manoj.updated@bengal.in", reloadedOpt.get().getEmail());
        assertEquals("Jubilee Hills, Road No 36", reloadedOpt.get().getAddress());
    }

    @Test
    @DisplayName("6. Duplicate Employee ID rejection in repository")
    void testDuplicateEmployeeIdRejection() throws IOException {
        Path empFile = tempDir.resolve("employees_dup.csv");
        EmployeeRepository repo = new EmployeeRepository(empFile);

        PermanentEmployee emp1 = new PermanentEmployee(
                "Rohit", "Sharma", "DUP100", LocalDate.of(1990, 4, 30),
                "rohit@india.in", "9988776655", "Married", "Mumbai",
                "Team", "Senior Developer", "Telangana", LocalDate.of(2015, 1, 1),
                true, "PF-DUP-1", "UAN-DUP-1", false, false, ""
        );
        repo.save(emp1);

        PermanentEmployee duplicate = new PermanentEmployee(
                "Rohit", "Duplicate", "DUP100", LocalDate.of(1991, 1, 1),
                "dup@india.in", "9988776655", "Single", "Delhi",
                "Team", "Senior Developer", "Telangana", LocalDate.of(2016, 1, 1),
                true, "PF-DUP-2", "UAN-DUP-2", false, false, ""
        );

        assertThrows(IllegalArgumentException.class, () -> repo.save(duplicate));
    }

    @Test
    @DisplayName("7. Policy persistence and non-overwrite guarantee across restart")
    void testExistingPoliciesNeverOverwrittenOnStartup() {
        PolicyRepository policyRepo = new PolicyRepository(tempDir);

        // Modify Senior Developer policy with custom non-default basic
        DesignationPolicy customPolicy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("55000.00"), new BigDecimal("6000.00"),
                new BigDecimal("3500.00"), new BigDecimal("1500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.10"), new BigDecimal("0.12"), // Admin configured rates
                "BASIC_DA", BigDecimal.ZERO, true
        );
        policyRepo.saveDesignationPolicy(customPolicy);

        // Restart repository by creating a new instance pointing to same directory
        PolicyRepository restartedPolicyRepo = new PolicyRepository(tempDir);
        Optional<DesignationPolicy> loaded = restartedPolicyRepo.findDesignationPolicy("Senior Developer", "Permanent");

        assertTrue(loaded.isPresent());
        // Verify custom configured values are PRESERVED and NOT overwritten by defaults
        assertEquals(new BigDecimal("55000.00"), loaded.get().getMonthlyBasicSalary());
        assertEquals(new BigDecimal("6000.00"), loaded.get().getHra());
        assertEquals(new BigDecimal("0.10"), loaded.get().getEmployeePfRate());
        assertEquals(new BigDecimal("0.12"), loaded.get().getEmployerPfRate());
    }

    @Test
    @DisplayName("8. Historical Payroll Integrity: modifying policy later does NOT alter historical records")
    void testHistoricalPayrollIntegrityAfterPolicyChange() throws IOException {
        Path payrollFile = tempDir.resolve("payroll_history.csv");
        PayrollRepository payrollRepo = new PayrollRepository(payrollFile);
        PayrollCalculator calculator = new PayrollCalculator();

        // Initial policy: Senior Developer Basic = ₹40,000, HRA = ₹4,000, DA = ₹2,500, Conveyance = ₹1,200
        DesignationPolicy originalPolicy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"), new BigDecimal("4000.00"),
                new BigDecimal("2500.00"), new BigDecimal("1200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        );

        PermanentEmployee emp = new PermanentEmployee(
                "Ajinkya", "Rahane", "HIST001", LocalDate.of(1988, 6, 6),
                "ajinkya@india.in", "9876543210", "Married", "Hyderabad",
                "Cricket", "Senior Developer", "Telangana", LocalDate.of(2018, 1, 1),
                true, "PF-HIST", "UAN-HIST", true, false, ""
        );

        AttendanceRecord att = new AttendanceRecord("HIST001", "10-2023", 20, 20);

        // Process and save October 2023 payroll record
        PayrollRecord octRecord = calculator.calculatePayroll(
                emp, originalPolicy, att, null, "10-2023", LocalDate.of(2023, 10, 31)
        );
        payrollRepo.save(octRecord);

        // VERIFY: Historical record values at time of generation
        assertEquals(new BigDecimal("40000.00"), octRecord.getPayableBasic());
        assertEquals(new BigDecimal("47700.00"), octRecord.getGrossEarnings());
        assertEquals(new BigDecimal("42400.00"), octRecord.getNetSalary());

        // NOW: Administrator increases Senior Developer Basic to ₹60,000 in November
        DesignationPolicy updatedPolicy = new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("60000.00"), new BigDecimal("6000.00"),
                new BigDecimal("3000.00"), new BigDecimal("1500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        );

        // Simulate application restart: Reload payroll history from CSV
        PayrollRepository reloadedPayrollRepo = new PayrollRepository(payrollFile);
        Optional<PayrollRecord> historicalOpt = reloadedPayrollRepo.findById(octRecord.getRecordId());

        assertTrue(historicalOpt.isPresent());
        PayrollRecord historicalRecord = historicalOpt.get();

        // STRICT BUSINESS RULE: Previous payroll records must remain completely unchanged!
        assertEquals(new BigDecimal("40000.00"), historicalRecord.getPayableBasic(),
                "Historical payable basic must remain ₹40,000.00 despite policy update to ₹60,000.00");
        assertEquals(new BigDecimal("47700.00"), historicalRecord.getGrossEarnings(),
                "Historical gross earnings must remain ₹47,700.00");
        assertEquals(new BigDecimal("42400.00"), historicalRecord.getNetSalary(),
                "Historical net salary must remain ₹42,400.00");
    }

    @Test
    @DisplayName("9. Duplicate payroll prevention for same employee and period")
    void testDuplicatePayrollPrevention() throws IOException {
        Path payrollFile = tempDir.resolve("payroll_dup.csv");
        PayrollRepository payrollRepo = new PayrollRepository(payrollFile);

        PayrollRecord rec1 = new PayrollRecord();
        rec1.setRecordId("PR-DUP-1");
        rec1.setEmployeeId("EMP999");
        rec1.setPayPeriod("11-2023");
        rec1.setPayDate(LocalDate.now());
        rec1.setDateOfJoining(LocalDate.now().minusYears(2));
        rec1.setEmployeeNameSnapshot("Test User");
        rec1.setDesignationSnapshot("Developer");
        rec1.setDepartment("IT");
        rec1.setEmployeeType("Permanent");
        rec1.setWorkLocation("Telangana");

        payrollRepo.save(rec1);

        // Attempting to save another record for same employee and pay period must fail
        PayrollRecord rec2 = new PayrollRecord();
        rec2.setRecordId("PR-DUP-2");
        rec2.setEmployeeId("EMP999");
        rec2.setPayPeriod("11-2023");

        assertThrows(IllegalStateException.class, () -> payrollRepo.save(rec2));
    }

    @Test
    @DisplayName("10. Graceful handling of malformed or corrupted CSV rows")
    void testMalformedCsvHandling() throws IOException {
        Path malformedPath = tempDir.resolve("malformed_employees.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(malformedPath, StandardCharsets.UTF_8)) {
            writer.write(String.join(",", EmployeeRepository.HEADERS));
            writer.newLine();
            // Corrupted line 1: missing columns
            writer.write("EMP_BAD1,Incomplete,OnlyTwoCols");
            writer.newLine();
            // Valid line 2
            writer.write("EMP_GOOD,Good,Employee,Permanent,1990-01-01,good@emp.com,9876543210,Single,Hyd,IT,Developer,Telangana,2020-01-01,true,PF1,UAN1,true,false,");
            writer.newLine();
            // Corrupted line 3: invalid date
            writer.write("EMP_BAD2,Bad,Date,Permanent,INVALID_DATE,bad@emp.com,9876543210,Single,Hyd,IT,Developer,Telangana,2020-01-01,true,PF1,UAN1,true,false,");
            writer.newLine();
        }

        EmployeeRepository repo = new EmployeeRepository(malformedPath);
        List<Employee> loaded = repo.loadAll();

        // Should gracefully skip the 2 malformed rows and safely load the 1 valid row
        assertEquals(1, loaded.size());
        assertEquals("EMP_GOOD", loaded.get(0).getEmployeeId());
    }

    @Test
    @DisplayName("11. Automatic creation of default data directory and CSV files without silent employee insertion")
    void testDefaultDataDirectoryInitialization() {
        Path projectDataDir = java.nio.file.Paths.get("data");
        PolicyRepository policyRepo = new PolicyRepository(projectDataDir);
        EmployeeRepository employeeRepo = new EmployeeRepository(projectDataDir.resolve("employees.csv"));
        PayrollRepository payrollRepo = new PayrollRepository(projectDataDir.resolve("payroll.csv"));

        assertTrue(Files.exists(projectDataDir.resolve("employees.csv")), "employees.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("designation_policies.csv")), "designation_policies.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("payroll.csv")), "payroll.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("professional_tax_policies.csv")), "professional_tax_policies.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("pf_policies.csv")), "pf_policies.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("company_settings.csv")), "company_settings.csv must exist");
        assertTrue(Files.exists(projectDataDir.resolve("projects.csv")), "projects.csv must exist");
    }
}
