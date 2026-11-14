package com.payroll.service;

import com.payroll.model.ProfessionalTaxPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating Professional Tax based on work location and monthly gross wages.
 * Maintains state-wise policies (Andhra Pradesh and Telangana) and allows dynamic modification.
 */
public class ProfessionalTaxCalculator {

    private final Map<String, ProfessionalTaxPolicy> statePolicies = new HashMap<>();

    public ProfessionalTaxCalculator() {
        // Initialize with default demonstration policies as required by Section 10
        registerPolicy(ProfessionalTaxPolicy.createDefaultPolicy("Andhra Pradesh"));
        registerPolicy(ProfessionalTaxPolicy.createDefaultPolicy("Telangana"));
    }

    public void registerPolicy(ProfessionalTaxPolicy policy) {
        if (policy != null && policy.getState() != null) {
            statePolicies.put(policy.getState().trim().toLowerCase(), policy);
        }
    }

    public ProfessionalTaxPolicy getPolicy(String state) {
        if (state == null) return null;
        return statePolicies.get(state.trim().toLowerCase());
    }

    /**
     * Calculates professional tax for an employee based on their work location and assessment wage.
     *
     * @param workLocation The employee's work location ("Andhra Pradesh" or "Telangana")
     * @param monthlyWages The gross wages earned in the month (actual earnings for bench employees)
     * @return Applicable Professional Tax rounded to 2 decimal places
     */
    public BigDecimal calculateTax(String workLocation, BigDecimal monthlyWages) {
        if (workLocation == null || monthlyWages == null || monthlyWages.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        ProfessionalTaxPolicy policy = getPolicy(workLocation);
        if (policy == null) {
            throw new IllegalArgumentException("No professional tax policy configured for state: '" + workLocation + "'. Available: " + statePolicies.keySet());
        }

        BigDecimal tax = policy.calculateTax(monthlyWages);
        return (tax != null ? tax : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}

