package com.payroll.storage;

import com.payroll.model.BenchEmployee;
import com.payroll.model.Employee;
import com.payroll.model.PermanentEmployee;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe CSV persistence repository for Employee records.
 * Uses FileHandler for atomic file operations and RFC-4180 parsing.
 * 
 * Strict Enforcement:
 * - Initializes an EMPTY CSV with headers when missing or empty.
 * - NEVER silently or automatically inserts demonstration employees.
 * - Demo data is strictly an explicit optional method (seedSampleEmployees).
 */
public class EmployeeRepository {

    public static final List<String> HEADERS = List.of(
            "employee_id", "first_name", "last_name", "category", "dob", "email",
            "mobile", "marital_status", "address", "department", "designation",
            "work_location", "doj", "active", "pf_account_number", "uan",
            "pf_applicable", "medical_condition", "medical_description"
    );

    private final Path filePath;
    private final Map<String, Employee> cache = new ConcurrentHashMap<>();

    public EmployeeRepository() {
        this(Paths.get("data", "employees.csv"));
    }

    public EmployeeRepository(Path filePath) {
        this.filePath = filePath;
        initFile();
    }

    private void initFile() {
        try {
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                // Initialize an empty employee CSV file with headers only
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                FileHandler.writeCsvAtomic(filePath, HEADERS, Collections.emptyList());
                cache.clear();
            } else {
                loadAll();
            }
        } catch (IOException e) {
            System.err.println("Warning: Unable to initialize employee repository: " + e.getMessage());
        }
    }

    /**
     * Optional explicit action: Seeds demonstration employees only when deliberately requested.
     * NEVER invoked automatically or silently during initialization.
     */
    public synchronized void seedSampleEmployees() {
        // 1. Permanent Senior Developer (Telangana)
        PermanentEmployee permDev = new PermanentEmployee(
                "Rahul", "Sharma", "EMP1001", LocalDate.of(1993, 4, 15),
                "rahul.sharma@acmetech.com", "9876543210", "Married", "Hitech City, Hyderabad",
                "Engineering", "Senior Developer", "Telangana",
                LocalDate.of(2020, 3, 1), true, "PF-TS-1001", "UAN-100123456789",
                false, false, ""
        );

        // 2. Permanent Manager (Andhra Pradesh)
        PermanentEmployee permMgr = new PermanentEmployee(
                "Priya", "Iyer", "EMP1002", LocalDate.of(1989, 7, 22),
                "priya.iyer@acmetech.com", "9845012345", "Married", "Benz Circle, Vijayawada",
                "Management", "Manager", "Andhra Pradesh",
                LocalDate.of(2018, 6, 15), true, "PF-AP-1002", "UAN-100234567890",
                false, false, ""
        );

        // 3. Bench Senior Developer (Telangana)
        BenchEmployee benchSrDev = new BenchEmployee(
                "Vikram", "Varma", "BENCH2001", LocalDate.of(1995, 11, 8),
                "vikram.varma@acmetech.com", "9988776655", "Single", "Gachibowli, Hyderabad",
                "Engineering", "Senior Developer", "Telangana",
                LocalDate.of(2022, 1, 10), true, "PF-TS-2001", "UAN-200145678901",
                false, false, ""
        );

        // 4. Bench Junior Developer (Andhra Pradesh)
        BenchEmployee benchJrDev = new BenchEmployee(
                "Sneha", "Reddy", "BENCH2002", LocalDate.of(1999, 2, 18),
                "sneha.reddy@acmetech.com", "9123456780", "Single", "MVP Colony, Visakhapatnam",
                "Engineering", "Junior Developer", "Andhra Pradesh",
                LocalDate.of(2023, 7, 1), true, "PF-AP-2002", "UAN-200256789012",
                false, false, ""
        );

        cache.put(permDev.getEmployeeId().toUpperCase(), permDev);
        cache.put(permMgr.getEmployeeId().toUpperCase(), permMgr);
        cache.put(benchSrDev.getEmployeeId().toUpperCase(), benchSrDev);
        cache.put(benchJrDev.getEmployeeId().toUpperCase(), benchJrDev);

        saveAllToDisk();
    }

    public synchronized List<Employee> loadAll() {
        try {
            List<List<String>> rows = FileHandler.readCsv(filePath);
            cache.clear();

            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                // Skip header line if present
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("employee_id")) {
                    continue;
                }
                if (row.size() < 17) continue; // Malformed row safeguard

                Employee emp = mapRowToEmployee(row);
                if (emp != null) {
                    cache.put(emp.getEmployeeId().toUpperCase(), emp);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading employees from CSV: " + e.getMessage());
        }
        return new ArrayList<>(cache.values());
    }

    public Optional<Employee> findById(String employeeId) {
        if (employeeId == null) return Optional.empty();
        return Optional.ofNullable(cache.get(employeeId.trim().toUpperCase()));
    }

    public synchronized void save(Employee employee) throws IOException {
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee cannot be null and must have an ID.");
        }
        String key = employee.getEmployeeId().trim().toUpperCase();
        if (cache.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate Employee ID: '" + employee.getEmployeeId() + "' already exists.");
        }
        cache.put(key, employee);
        saveAllToDisk();
    }

    public synchronized void update(Employee employee) throws IOException {
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee cannot be null and must have an ID.");
        }
        String key = employee.getEmployeeId().trim().toUpperCase();
        if (!cache.containsKey(key)) {
            throw new IllegalArgumentException("Cannot update non-existent Employee ID: '" + employee.getEmployeeId() + "'.");
        }
        cache.put(key, employee);
        saveAllToDisk();
    }

    public synchronized void deactivate(String employeeId) throws IOException {
        Optional<Employee> empOpt = findById(employeeId);
        if (empOpt.isPresent()) {
            empOpt.get().setActive(false);
            saveAllToDisk();
        }
    }

    private void saveAllToDisk() {
        List<List<String>> rows = new ArrayList<>();
        for (Employee emp : cache.values()) {
            rows.add(mapEmployeeToRow(emp));
        }
        try {
            FileHandler.writeCsvAtomic(filePath, HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist employees to CSV: " + e.getMessage(), e);
        }
    }

    private Employee mapRowToEmployee(List<String> row) {
        try {
            String empId = row.get(0).trim();
            String firstName = row.get(1).trim();
            String lastName = row.get(2).trim();
            String category = row.get(3).trim();
            LocalDate dob = LocalDate.parse(row.get(4).trim());
            String email = row.get(5).trim();
            String mobile = row.get(6).trim();
            String maritalStatus = row.get(7).trim();
            String address = row.get(8).trim();
            String department = row.get(9).trim();
            String designation = row.get(10).trim();
            String workLocation = row.get(11).trim();
            LocalDate doj = LocalDate.parse(row.get(12).trim());
            boolean active = Boolean.parseBoolean(row.get(13).trim());
            String pfAcc = row.get(14).trim();
            String uan = row.get(15).trim();
            boolean pfApplicable = Boolean.parseBoolean(row.get(16).trim());

            boolean medicalCondition = false;
            String medicalDesc = "";
            if (row.size() > 17) {
                medicalCondition = Boolean.parseBoolean(row.get(17).trim());
            }
            if (row.size() > 18) {
                medicalDesc = row.get(18).trim();
            }

            if ("Bench".equalsIgnoreCase(category)) {
                return new BenchEmployee(
                        firstName, lastName, empId, dob, email, mobile, maritalStatus,
                        address, department, designation, workLocation, doj, active,
                        pfAcc, uan, pfApplicable, medicalCondition, medicalDesc
                );
            } else {
                return new PermanentEmployee(
                        firstName, lastName, empId, dob, email, mobile, maritalStatus,
                        address, department, designation, workLocation, doj, active,
                        pfAcc, uan, pfApplicable, medicalCondition, medicalDesc
                );
            }
        } catch (Exception e) {
            System.err.println("Warning: Skipping malformed employee record: " + row + " (Error: " + e.getMessage() + ")");
            return null;
        }
    }

    private List<String> mapEmployeeToRow(Employee emp) {
        List<String> row = new ArrayList<>();
        row.add(emp.getEmployeeId());
        row.add(emp.getFirstName());
        row.add(emp.getLastName());
        row.add(emp.getEmployeeCategory());
        row.add(emp.getDateOfBirth() != null ? emp.getDateOfBirth().toString() : "");
        row.add(emp.getEmail());
        row.add(emp.getMobileNumber());
        row.add(emp.getMaritalStatus());
        row.add(emp.getAddress());
        row.add(emp.getDepartment());
        row.add(emp.getDesignation());
        row.add(emp.getWorkLocation());
        row.add(emp.getDateOfJoining() != null ? emp.getDateOfJoining().toString() : "");
        row.add(String.valueOf(emp.isActive()));
        row.add(emp.getPfAccountNumber());
        row.add(emp.getUan());
        row.add(String.valueOf(emp.isPfApplicable()));
        row.add(String.valueOf(emp.isMedicalCondition()));
        row.add(emp.getMedicalDescription() != null ? emp.getMedicalDescription() : "");
        return row;
    }

    public Path getFilePath() {
        return filePath;
    }
}
