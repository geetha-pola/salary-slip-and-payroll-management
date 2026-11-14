package com.payroll.service;

import com.payroll.storage.EmployeeRepository;
import com.payroll.storage.PayrollRepository;
import com.payroll.storage.PolicyRepository;

import java.nio.file.Paths;

/**
 * Central application registry coordinating singleton repositories and business services.
 * Ensures consistent in-memory state and persistent CSV synchronization across all controllers.
 */
public class AppContext {

    private static AppContext instance;

    private final PolicyRepository policyRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;

    private final DesignationPolicyManager policyManager;
    private final EmployeeManager employeeManager;
    private final PayrollCalculator payrollCalculator;
    private final AuthService authService;
    private final PdfSalarySlipGenerator pdfSalarySlipGenerator;

    private AppContext() {
        this.policyRepository = new PolicyRepository(Paths.get("data"));
        this.employeeRepository = new EmployeeRepository(Paths.get("data", "employees.csv"));
        this.payrollRepository = new PayrollRepository(Paths.get("data", "payroll.csv"));

        this.policyManager = new DesignationPolicyManager();
        // Synchronize policyManager with persistent policies
        this.policyRepository.getAllDesignationPolicies().forEach(this.policyManager::savePolicy);

        this.employeeManager = new EmployeeManager(this.policyManager);
        // Synchronize employeeManager with persistent employees
        this.employeeRepository.loadAll().forEach(emp -> {
            try {
                this.employeeManager.registerEmployee(emp);
            } catch (Exception ignored) {
            }
        });

        this.payrollCalculator = new PayrollCalculator();
        this.authService = new AuthService();
        this.pdfSalarySlipGenerator = new PdfSalarySlipGenerator();
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public PolicyRepository getPolicyRepository() {
        return policyRepository;
    }

    public EmployeeRepository getEmployeeRepository() {
        return employeeRepository;
    }

    public PayrollRepository getPayrollRepository() {
        return payrollRepository;
    }

    public DesignationPolicyManager getPolicyManager() {
        return policyManager;
    }

    public EmployeeManager getEmployeeManager() {
        return employeeManager;
    }

    public PayrollCalculator getPayrollCalculator() {
        return payrollCalculator;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public PdfSalarySlipGenerator getPdfSalarySlipGenerator() {
        return pdfSalarySlipGenerator;
    }
}

