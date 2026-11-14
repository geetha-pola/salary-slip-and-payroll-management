package com.payroll.model;

/**
 * Model representing company details for payroll processing and payslip headers.
 */
public class CompanySettings {

    private String companyName = "Acme Technologies Private Limited";
    private String companyAddress = "Plot 42, Hitech City, Hyderabad, Telangana 500081";
    private String payslipTitle = "MONTHLY SALARY SLIP";
    private String contactEmail = "hr@acmetech.com";
    private String contactPhone = "+91 40 1234 5678";

    public CompanySettings() {
    }

    public CompanySettings(String companyName, String companyAddress, String payslipTitle, String contactEmail, String contactPhone) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.payslipTitle = payslipTitle;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getPayslipTitle() {
        return payslipTitle;
    }

    public void setPayslipTitle(String payslipTitle) {
        this.payslipTitle = payslipTitle;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}

