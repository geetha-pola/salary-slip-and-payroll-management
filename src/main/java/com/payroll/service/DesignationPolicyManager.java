package com.payroll.service;

import com.payroll.model.DesignationPolicy;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing designation-wise salary and deduction policies.
 * Ensures strict separation between Permanent and Bench policies even when sharing the same designation name.
 */
public class DesignationPolicyManager {

    private final Map<String, DesignationPolicy> policyMap = new ConcurrentHashMap<>();

    public DesignationPolicyManager() {
        loadDemonstrationPolicies();
    }

    private String buildKey(String designationName, String employeeCategory) {
        if (designationName == null || employeeCategory == null) return "";
        return designationName.trim().toLowerCase() + "#" + employeeCategory.trim().toLowerCase();
    }

    /**
     * Seeds initial demonstration policies as specified in Section 4 & Section 20.
     * Demonstrates separately configured employee and employer PF rates.
     */
    public void loadDemonstrationPolicies() {
        // 1. Permanent Senior Developer
        savePolicy(new DesignationPolicy(
                "Senior Developer", "Permanent",
                new BigDecimal("40000.00"), new BigDecimal("4000.00"),
                new BigDecimal("2500.00"), new BigDecimal("1200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 2. Permanent Manager
        savePolicy(new DesignationPolicy(
                "Manager", "Permanent",
                new BigDecimal("65000.00"), new BigDecimal("7500.00"),
                new BigDecimal("4000.00"), new BigDecimal("2000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 3. Bench Senior Developer (Daily Rate ₹1,500, HRA=0, DA=0, Conveyance=0)
        savePolicy(new DesignationPolicy(
                "Senior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("1500.00"),
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        ));

        // 4. Bench Junior Developer (Daily Rate ₹800, HRA=0, DA=0, Conveyance=0)
        savePolicy(new DesignationPolicy(
                "Junior Developer", "Bench",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("800.00"),
                true, new BigDecimal("0.12"), new BigDecimal("0.12"),
                "BASIC_DA", BigDecimal.ZERO, true
        ));
    }

    public synchronized void savePolicy(DesignationPolicy policy) {
        if (policy == null || policy.getDesignationName() == null || policy.getEmployeeCategory() == null) {
            throw new IllegalArgumentException("Policy designation name and category are required.");
        }
        policyMap.put(buildKey(policy.getDesignationName(), policy.getEmployeeCategory()), policy);
    }

    public Optional<DesignationPolicy> findPolicy(String designationName, String employeeCategory) {
        if (designationName == null || employeeCategory == null) return Optional.empty();
        return Optional.ofNullable(policyMap.get(buildKey(designationName, employeeCategory)));
    }

    public List<DesignationPolicy> getAllPolicies() {
        return new ArrayList<>(policyMap.values());
    }

    public List<String> getActiveDesignationNames(String employeeCategory) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (DesignationPolicy policy : policyMap.values()) {
            if (policy.isActive()) {
                if (employeeCategory == null || policy.getEmployeeCategory().equalsIgnoreCase(employeeCategory)) {
                    names.add(policy.getDesignationName());
                }
            }
        }
        return new ArrayList<>(names);
    }

    public void deactivatePolicy(String designationName, String employeeCategory) {
        findPolicy(designationName, employeeCategory).ifPresent(p -> p.setActive(false));
    }

    public void clear() {
        policyMap.clear();
    }
}
