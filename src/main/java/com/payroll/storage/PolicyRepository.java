package com.payroll.storage;

import com.payroll.model.CompanySettings;
import com.payroll.model.DesignationPolicy;
import com.payroll.model.PFPolicy;
import com.payroll.model.ProfessionalTaxPolicy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe CSV persistence repository for Policies, Slabs, Company Settings, and Projects.
 * 
 * Strict Enforcement:
 * 1. Never overwrites existing user-configured policies on startup.
 * 2. PF rates are NOT assumed or seeded as statutory values; explicit administrator setup is required.
 * 3. AP and Telangana professional tax slabs are stored as configurable demonstration sample values,
 *    not hardcoded or claimed as official statutory guarantees.
 */
public class PolicyRepository {

    public static final List<String> DESIGNATION_HEADERS = List.of(
            "designation_name", "category", "monthly_basic", "hra", "da", "conveyance",
            "other_earnings", "daily_rate", "pf_applicable", "employee_pf_rate",
            "employer_pf_rate", "pf_wage_base", "other_deductions", "active"
    );

    public static final List<String> PT_HEADERS = List.of(
            "state", "min_salary", "max_salary", "tax_amount"
    );

    public static final List<String> PF_HEADERS = List.of(
            "default_applicable", "employee_rate", "employer_rate", "wage_base_type", "wage_ceiling", "enforce_ceiling"
    );

    public static final List<String> COMPANY_HEADERS = List.of(
            "company_name", "company_address", "payslip_title", "contact_email", "contact_phone"
    );

    public static final List<String> PROJECT_HEADERS = List.of(
            "project_id", "project_name", "client_or_description", "active"
    );

    private final Path baseDir;
    private final Path designationPath;
    private final Path ptPath;
    private final Path pfPath;
    private final Path companyPath;
    private final Path projectsPath;

    private final Map<String, DesignationPolicy> designationPolicies = new ConcurrentHashMap<>();
    private final Map<String, ProfessionalTaxPolicy> ptPolicies = new ConcurrentHashMap<>();
    private PFPolicy pfPolicy = new PFPolicy();
    private CompanySettings companySettings = new CompanySettings();
    private final List<String> projects = new ArrayList<>();

    public PolicyRepository() {
        this(Paths.get("data"));
    }

    public PolicyRepository(Path baseDir) {
        this.baseDir = baseDir;
        this.designationPath = baseDir.resolve("designation_policies.csv");
        this.ptPath = baseDir.resolve("professional_tax_policies.csv");
        this.pfPath = baseDir.resolve("pf_policies.csv");
        this.companyPath = baseDir.resolve("company_settings.csv");
        this.projectsPath = baseDir.resolve("projects.csv");

        initAll();
    }

    public synchronized void initAll() {
        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            initDesignationPolicies();
            initPtPolicies();
            initPfPolicy();
            initCompanySettings();
            initProjects();
        } catch (IOException e) {
            System.err.println("Warning: Error initializing policy repository: " + e.getMessage());
        }
    }

    // --- Designation Policies ---

    private void initDesignationPolicies() throws IOException {
        if (!Files.exists(designationPath) || Files.size(designationPath) == 0) {
            seedInitialDesignationPolicies();
        } else {
            loadDesignationPolicies();
        }
    }

    /**
     * Seeds initial designation structures without assumed statutory PF rates.
     * PF is disabled (false) and rates are unconfigured (null) until set by administrator.
     */
    public synchronized void seedInitialDesignationPolicies() {
        designationPolicies.clear();

        // 1. Permanent Senior Developer
        saveDesignationPolicyInMemory(new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"), new BigDecimal("4000.00"),
                new BigDecimal("2500.00"), new BigDecimal("1200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, null, null, // PF not automatically enabled, no hardcoded statutory rates
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 2. Permanent Manager
        saveDesignationPolicyInMemory(new DesignationPolicy(
                "Manager", "Permanent",
                new BigDecimal("65000.00"), new BigDecimal("7500.00"),
                new BigDecimal("4000.00"), new BigDecimal("2000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, null, null,
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 3. Bench Senior Developer
        saveDesignationPolicyInMemory(new DesignationPolicy(
                "Senior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("1500.00"),
                false, null, null,
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 4. Bench Junior Developer
        saveDesignationPolicyInMemory(new DesignationPolicy(
                "Junior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("800.00"),
                false, null, null,
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        saveDesignationPoliciesToDisk();
    }

    public synchronized List<DesignationPolicy> loadDesignationPolicies() {
        try {
            List<List<String>> rows = FileHandler.readCsv(designationPath);
            designationPolicies.clear();
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("designation_name")) continue;
                if (row.size() < 14) continue;

                String empPfStr = row.get(9).trim();
                String empiPfStr = row.get(10).trim();
                BigDecimal empPfRate = (!empPfStr.isEmpty() && !empPfStr.equalsIgnoreCase("null"))
                        ? new BigDecimal(empPfStr) : null;
                BigDecimal empiPfRate = (!empiPfStr.isEmpty() && !empiPfStr.equalsIgnoreCase("null"))
                        ? new BigDecimal(empiPfStr) : null;

                DesignationPolicy p = new DesignationPolicy(
                        row.get(0).trim(),
                        row.get(1).trim(),
                        new BigDecimal(row.get(2).trim()),
                        new BigDecimal(row.get(3).trim()),
                        new BigDecimal(row.get(4).trim()),
                        new BigDecimal(row.get(5).trim()),
                        new BigDecimal(row.get(6).trim()),
                        new BigDecimal(row.get(7).trim()),
                        Boolean.parseBoolean(row.get(8).trim()),
                        empPfRate,
                        empiPfRate,
                        row.get(11).trim(),
                        new BigDecimal(row.get(12).trim()),
                        Boolean.parseBoolean(row.get(13).trim())
                );
                saveDesignationPolicyInMemory(p);
            }
        } catch (IOException e) {
            System.err.println("Error reading designation policies: " + e.getMessage());
        }
        return new ArrayList<>(designationPolicies.values());
    }

    public synchronized void saveDesignationPolicy(DesignationPolicy policy) {
        saveDesignationPolicyInMemory(policy);
        saveDesignationPoliciesToDisk();
    }

    private void saveDesignationPolicyInMemory(DesignationPolicy p) {
        String key = p.getDesignationName().trim().toLowerCase() + "#" + p.getEmployeeCategory().trim().toLowerCase();
        designationPolicies.put(key, p);
    }

    private void saveDesignationPoliciesToDisk() {
        List<List<String>> rows = new ArrayList<>();
        for (DesignationPolicy p : designationPolicies.values()) {
            rows.add(List.of(
                    p.getDesignationName(),
                    p.getEmployeeCategory(),
                    p.getMonthlyBasicSalary().toString(),
                    p.getHra().toString(),
                    p.getDa().toString(),
                    p.getConveyance().toString(),
                    p.getOtherEarnings().toString(),
                    p.getDailyRate().toString(),
                    String.valueOf(p.isPfApplicable()),
                    p.getEmployeePfRate() != null ? p.getEmployeePfRate().toString() : "",
                    p.getEmployerPfRate() != null ? p.getEmployerPfRate().toString() : "",
                    p.getPfWageBase() != null ? p.getPfWageBase() : "BASIC_DA",
                    p.getOtherDeductions().toString(),
                    String.valueOf(p.isActive())
            ));
        }
        try {
            FileHandler.writeCsvAtomic(designationPath, DESIGNATION_HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist designation policies: " + e.getMessage(), e);
        }
    }

    public Optional<DesignationPolicy> findDesignationPolicy(String name, String category) {
        if (name == null || category == null) return Optional.empty();
        String key = name.trim().toLowerCase() + "#" + category.trim().toLowerCase();
        return Optional.ofNullable(designationPolicies.get(key));
    }

    public List<DesignationPolicy> getAllDesignationPolicies() {
        return new ArrayList<>(designationPolicies.values());
    }

    // --- Professional Tax Policies ---

    private void initPtPolicies() throws IOException {
        if (!Files.exists(ptPath) || Files.size(ptPath) == 0) {
            seedDefaultPtPolicies();
        } else {
            loadPtPolicies();
        }
    }

    /**
     * Seeds initial demonstration slabs supplied for the project (Section 10).
     * Clearly documented as user-provided configurable sample policies.
     */
    public synchronized void seedDefaultPtPolicies() {
        ptPolicies.clear();
        ptPolicies.put("andhra pradesh", ProfessionalTaxPolicy.createDefaultPolicy("Andhra Pradesh"));
        ptPolicies.put("telangana", ProfessionalTaxPolicy.createDefaultPolicy("Telangana"));
        savePtPoliciesToDisk();
    }

    public synchronized Map<String, ProfessionalTaxPolicy> loadPtPolicies() {
        try {
            List<List<String>> rows = FileHandler.readCsv(ptPath);
            ptPolicies.clear();
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("state")) continue;
                if (row.size() < 4) continue;

                String state = row.get(0).trim();
                BigDecimal min = new BigDecimal(row.get(1).trim());
                BigDecimal max = new BigDecimal(row.get(2).trim());
                BigDecimal tax = new BigDecimal(row.get(3).trim());

                ptPolicies.computeIfAbsent(state.toLowerCase(), k -> new ProfessionalTaxPolicy(state, new ArrayList<>()))
                        .addSlab(new ProfessionalTaxPolicy.TaxSlab(min, max, tax));
            }
        } catch (IOException e) {
            System.err.println("Error reading professional tax policies: " + e.getMessage());
        }
        return ptPolicies;
    }

    public synchronized void savePtPolicy(ProfessionalTaxPolicy policy) {
        if (policy != null && policy.getState() != null) {
            ptPolicies.put(policy.getState().trim().toLowerCase(), policy);
            savePtPoliciesToDisk();
        }
    }

    private void savePtPoliciesToDisk() {
        List<List<String>> rows = new ArrayList<>();
        for (ProfessionalTaxPolicy policy : ptPolicies.values()) {
            for (ProfessionalTaxPolicy.TaxSlab slab : policy.getSlabs()) {
                rows.add(List.of(
                        policy.getState(),
                        slab.getMinSalary().toString(),
                        slab.getMaxSalary().toString(),
                        slab.getTaxAmount().toString()
                ));
            }
        }
        try {
            FileHandler.writeCsvAtomic(ptPath, PT_HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist PT policies: " + e.getMessage(), e);
        }
    }

    public ProfessionalTaxPolicy getPtPolicy(String state) {
        if (state == null) return null;
        return ptPolicies.get(state.trim().toLowerCase());
    }

    // --- PF Global Policy ---

    private void initPfPolicy() throws IOException {
        if (!Files.exists(pfPath) || Files.size(pfPath) == 0) {
            // Default: unconfigured and not applicable until administrator explicitly sets it
            pfPolicy = new PFPolicy(false, null, null, "BASIC_DA", null, false);
            savePfPolicyToDisk();
        } else {
            loadPfPolicy();
        }
    }

    public synchronized PFPolicy loadPfPolicy() {
        try {
            List<List<String>> rows = FileHandler.readCsv(pfPath);
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("default_applicable")) continue;
                if (row.size() >= 6) {
                    boolean app = Boolean.parseBoolean(row.get(0).trim());
                    String empRateStr = row.get(1).trim();
                    String empiRateStr = row.get(2).trim();
                    BigDecimal empRate = (!empRateStr.isEmpty() && !empRateStr.equalsIgnoreCase("null"))
                            ? new BigDecimal(empRateStr) : null;
                    BigDecimal empiRate = (!empiRateStr.isEmpty() && !empiRateStr.equalsIgnoreCase("null"))
                            ? new BigDecimal(empiRateStr) : null;

                    String wageBase = row.get(3).trim();
                    String ceilingStr = row.get(4).trim();
                    BigDecimal ceiling = (!ceilingStr.isEmpty() && !ceilingStr.equalsIgnoreCase("null"))
                            ? new BigDecimal(ceilingStr) : null;
                    boolean enforceCeiling = Boolean.parseBoolean(row.get(5).trim());

                    pfPolicy = new PFPolicy(app, empRate, empiRate, wageBase, ceiling, enforceCeiling);
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading PF policy: " + e.getMessage());
        }
        return pfPolicy;
    }

    public synchronized void savePfPolicy(PFPolicy policy) {
        if (policy != null) {
            this.pfPolicy = policy;
            savePfPolicyToDisk();
        }
    }

    private void savePfPolicyToDisk() {
        List<List<String>> rows = List.of(List.of(
                String.valueOf(pfPolicy.isDefaultApplicable()),
                pfPolicy.getEmployeeContributionRate() != null ? pfPolicy.getEmployeeContributionRate().toString() : "",
                pfPolicy.getEmployerContributionRate() != null ? pfPolicy.getEmployerContributionRate().toString() : "",
                pfPolicy.getWageBaseType() != null ? pfPolicy.getWageBaseType() : "BASIC_DA",
                pfPolicy.getWageCeiling() != null ? pfPolicy.getWageCeiling().toString() : "",
                String.valueOf(pfPolicy.isEnforceWageCeiling())
        ));
        try {
            FileHandler.writeCsvAtomic(pfPath, PF_HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist PF policy: " + e.getMessage(), e);
        }
    }

    public PFPolicy getPfPolicy() {
        return pfPolicy;
    }

    // --- Company Settings ---

    private void initCompanySettings() throws IOException {
        if (!Files.exists(companyPath) || Files.size(companyPath) == 0) {
            companySettings = new CompanySettings();
            saveCompanySettingsToDisk();
        } else {
            loadCompanySettings();
        }
    }

    public synchronized CompanySettings loadCompanySettings() {
        try {
            List<List<String>> rows = FileHandler.readCsv(companyPath);
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("company_name")) continue;
                if (row.size() >= 5) {
                    companySettings = new CompanySettings(
                            row.get(0).trim(),
                            row.get(1).trim(),
                            row.get(2).trim(),
                            row.get(3).trim(),
                            row.get(4).trim()
                    );
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading company settings: " + e.getMessage());
        }
        return companySettings;
    }

    public synchronized void saveCompanySettings(CompanySettings settings) {
        if (settings != null) {
            this.companySettings = settings;
            saveCompanySettingsToDisk();
        }
    }

    private void saveCompanySettingsToDisk() {
        List<List<String>> rows = List.of(List.of(
                companySettings.getCompanyName(),
                companySettings.getCompanyAddress(),
                companySettings.getPayslipTitle(),
                companySettings.getContactEmail(),
                companySettings.getContactPhone()
        ));
        try {
            FileHandler.writeCsvAtomic(companyPath, COMPANY_HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist company settings: " + e.getMessage(), e);
        }
    }

    public CompanySettings getCompanySettings() {
        return companySettings;
    }

    // --- Projects ---

    private void initProjects() throws IOException {
        if (!Files.exists(projectsPath) || Files.size(projectsPath) == 0) {
            seedDefaultProjects();
        } else {
            loadProjects();
        }
    }

    public synchronized void seedDefaultProjects() {
        projects.clear();
        projects.addAll(List.of(
                "Internal ERP Modernization",
                "Cloud Migration Initiative",
                "Client Billing Engine",
                "Security & Identity Platform"
        ));
        saveProjectsToDisk();
    }

    public synchronized List<String> loadProjects() {
        try {
            List<List<String>> rows = FileHandler.readCsv(projectsPath);
            projects.clear();
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("project_id")) continue;
                if (row.size() >= 2) {
                    projects.add(row.get(1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading projects: " + e.getMessage());
        }
        return new ArrayList<>(projects);
    }

    public synchronized void addProject(String projectName) {
        if (projectName != null && !projectName.trim().isEmpty() && !projects.contains(projectName.trim())) {
            projects.add(projectName.trim());
            saveProjectsToDisk();
        }
    }

    private void saveProjectsToDisk() {
        List<List<String>> rows = new ArrayList<>();
        int id = 1;
        for (String p : projects) {
            rows.add(List.of("PRJ-" + String.format("%03d", id++), p, "Active client project", "true"));
        }
        try {
            FileHandler.writeCsvAtomic(projectsPath, PROJECT_HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist projects: " + e.getMessage(), e);
        }
    }

    public List<String> getProjects() {
        return new ArrayList<>(projects);
    }
}
