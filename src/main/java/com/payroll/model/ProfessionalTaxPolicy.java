package com.payroll.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates state-specific Professional Tax (PT) policies and slabs.
 * Stored persistently in configuration files and modifiable by administrators.
 */
public class ProfessionalTaxPolicy {

    private String state; // "Andhra Pradesh" or "Telangana"
    private List<TaxSlab> slabs = new ArrayList<>();

    public ProfessionalTaxPolicy() {
    }

    public ProfessionalTaxPolicy(String state, List<TaxSlab> slabs) {
        this.state = state;
        if (slabs != null) {
            this.slabs = new ArrayList<>(slabs);
        }
    }

    /**
     * Creates default initial demonstration slabs for Andhra Pradesh or Telangana:
     * - Up to ₹15,000: ₹0
     * - ₹15,001 to ₹20,000: ₹150
     * - Above ₹20,000: ₹200
     */
    public static ProfessionalTaxPolicy createDefaultPolicy(String state) {
        List<TaxSlab> defaultSlabs = new ArrayList<>();
        defaultSlabs.add(new TaxSlab(BigDecimal.ZERO, new BigDecimal("15000.00"), BigDecimal.ZERO));
        defaultSlabs.add(new TaxSlab(new BigDecimal("15000.01"), new BigDecimal("20000.00"), new BigDecimal("150.00")));
        defaultSlabs.add(new TaxSlab(new BigDecimal("20000.01"), new BigDecimal("999999999.00"), new BigDecimal("200.00")));
        return new ProfessionalTaxPolicy(state, defaultSlabs);
    }

    public BigDecimal calculateTax(BigDecimal monthlyWages) {
        if (monthlyWages == null || monthlyWages.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        for (TaxSlab slab : slabs) {
            if (slab.matches(monthlyWages)) {
                return slab.getTaxAmount();
            }
        }
        return BigDecimal.ZERO;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<TaxSlab> getSlabs() {
        return slabs;
    }

    public void setSlabs(List<TaxSlab> slabs) {
        this.slabs = slabs;
    }

    public void addSlab(TaxSlab slab) {
        this.slabs.add(slab);
    }

    /**
     * Inner class representing a salary slab and its corresponding fixed tax amount.
     */
    public static class TaxSlab {
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private BigDecimal taxAmount;

        public TaxSlab() {
        }

        public TaxSlab(BigDecimal minSalary, BigDecimal maxSalary, BigDecimal taxAmount) {
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
            this.taxAmount = taxAmount;
        }

        public boolean matches(BigDecimal amount) {
            if (amount == null) return false;
            boolean greaterOrEqualMin = minSalary == null || amount.compareTo(minSalary) >= 0;
            boolean lessOrEqualMax = maxSalary == null || amount.compareTo(maxSalary) <= 0;
            return greaterOrEqualMin && lessOrEqualMax;
        }

        public BigDecimal getMinSalary() {
            return minSalary;
        }

        public void setMinSalary(BigDecimal minSalary) {
            this.minSalary = minSalary;
        }

        public BigDecimal getMaxSalary() {
            return maxSalary;
        }

        public void setMaxSalary(BigDecimal maxSalary) {
            this.maxSalary = maxSalary;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public void setTaxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
        }

        @Override
        public String toString() {
            return "TaxSlab{" + minSalary + " - " + maxSalary + " => ₹" + taxAmount + "}";
        }
    }
}

