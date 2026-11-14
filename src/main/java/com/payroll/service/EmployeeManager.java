package com.payroll.service;

import com.payroll.model.BenchEmployee;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.Employee;
import com.payroll.model.PermanentEmployee;
import com.payroll.util.ValidationUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing employee lifecycle, registration validation, and directory queries.
 */
public class EmployeeManager {

    private final Map<String, Employee> employeeMap = new ConcurrentHashMap<>();
    private final DesignationPolicyManager policyManager;

    public EmployeeManager(DesignationPolicyManager policyManager) {
        this.policyManager = policyManager != null ? policyManager : new DesignationPolicyManager();
    }

    /**
     * Validates and registers a new employee.
     * Enforces all 10 registration business rules from Section 5.
     */
    public synchronized void registerEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }

        // Rule 1: Employee ID must be unique and valid
        String empId = employee.getEmployeeId();
        if (!ValidationUtil.isValidEmployeeId(empId)) {
            throw new IllegalArgumentException("Invalid Employee ID: '" + empId + "'. Must be 2-20 alphanumeric characters.");
        }
        if (employeeMap.containsKey(empId.trim().toUpperCase())) {
            throw new IllegalArgumentException("Duplicate Employee ID: '" + empId + "' already exists in the system.");
        }

        // Rule 2 & 3: Mandatory fields & contact validation
        if (!ValidationUtil.isNotEmpty(employee.getFirstName())) {
            throw new IllegalArgumentException("First name is mandatory.");
        }
        if (!ValidationUtil.isNotEmpty(employee.getLastName())) {
            throw new IllegalArgumentException("Last name is mandatory.");
        }
        if (!ValidationUtil.isValidEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + employee.getEmail());
        }
        if (!ValidationUtil.isValidMobile(employee.getMobileNumber())) {
            throw new IllegalArgumentException("Invalid Indian 10-digit mobile number: " + employee.getMobileNumber());
        }

        // Rule 4 & 5: Date validations
        if (!ValidationUtil.isValidDateOfBirth(employee.getDateOfBirth())) {
            throw new IllegalArgumentException("Invalid Date of Birth. Employee must be at least 18 years old.");
        }
        if (!ValidationUtil.isValidDateOfJoining(employee.getDateOfJoining(), employee.getDateOfBirth())) {
            throw new IllegalArgumentException("Date of joining cannot be prior to the employee's 18th birthday.");
        }

        // Rule 6 & 10: Designation must exist in active policy configuration
        String designation = employee.getDesignation();
        String category = employee.getEmployeeCategory();
        if (!ValidationUtil.isNotEmpty(designation)) {
            throw new IllegalArgumentException("Designation is required.");
        }

        Optional<DesignationPolicy> policyOpt = policyManager.findPolicy(designation, category);
        if (policyOpt.isEmpty()) {
            throw new IllegalArgumentException("No policy found for designation: '" + designation + "' and category: '" + category + "'.");
        }
        DesignationPolicy policy = policyOpt.get();
        if (!policy.isActive()) {
            throw new IllegalArgumentException("Designation '" + designation + "' is currently inactive. Inactive designations cannot be assigned to new employees.");
        }

        // Rule 7: Employee type validation
        if (!"Permanent".equalsIgnoreCase(category) && !"Bench".equalsIgnoreCase(category)) {
            throw new IllegalArgumentException("Employee category must be 'Permanent' or 'Bench'. Found: " + category);
        }

        // Location validation
        String location = employee.getWorkLocation();
        if (location == null || (!location.equalsIgnoreCase("Andhra Pradesh") && !location.equalsIgnoreCase("Telangana"))) {
            throw new IllegalArgumentException("Work location must be 'Andhra Pradesh' or 'Telangana'. Provided: " + location);
        }

        // Save
        employeeMap.put(empId.trim().toUpperCase(), employee);
    }

    public synchronized void updateEmployee(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalArgumentException("Valid Employee with ID is required for update.");
        }
        String key = employee.getEmployeeId().trim().toUpperCase();
        if (!employeeMap.containsKey(key)) {
            throw new IllegalArgumentException("Employee ID '" + employee.getEmployeeId() + "' not found for update.");
        }
        employeeMap.put(key, employee);
    }

    public Optional<Employee> findById(String employeeId) {
        if (employeeId == null) return Optional.empty();
        return Optional.ofNullable(employeeMap.get(employeeId.trim().toUpperCase()));
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employeeMap.values());
    }

    public List<Employee> getActiveEmployees() {
        return employeeMap.values().stream()
                .filter(Employee::isActive)
                .collect(Collectors.toList());
    }

    public List<Employee> filterEmployees(String query, String department, String designation,
                                          String category, String workLocation) {
        return employeeMap.values().stream()
                .filter(emp -> {
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.trim().toLowerCase();
                        boolean matchesId = emp.getEmployeeId().toLowerCase().contains(q);
                        boolean matchesName = emp.getFullName().toLowerCase().contains(q);
                        if (!matchesId && !matchesName) return false;
                    }
                    if (department != null && !department.equalsIgnoreCase("All") && !department.trim().isEmpty()) {
                        if (!department.equalsIgnoreCase(emp.getDepartment())) return false;
                    }
                    if (designation != null && !designation.equalsIgnoreCase("All") && !designation.trim().isEmpty()) {
                        if (!designation.equalsIgnoreCase(emp.getDesignation())) return false;
                    }
                    if (category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty()) {
                        if (!category.equalsIgnoreCase(emp.getEmployeeCategory())) return false;
                    }
                    if (workLocation != null && !workLocation.equalsIgnoreCase("All") && !workLocation.trim().isEmpty()) {
                        if (!workLocation.equalsIgnoreCase(emp.getWorkLocation())) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public void deactivateEmployee(String employeeId) {
        findById(employeeId).ifPresent(emp -> emp.setActive(false));
    }

    public void clear() {
        employeeMap.clear();
    }
}

