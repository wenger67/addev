package com.vinson.addev.model;


public class Company {
    private String fullName;
    private String alias;
    private String legalPerson;
    private String phone;
    private String status;
    private String regCode;
    private String orgCode;
    private String creditCode;
    private String taxCode;
    private String address;
    private Category category;

    public Company(String fullName, String alias, String legalPerson, String phone, String status, String regCode, String orgCode, String creditCode, String taxCode, String address, Category category) {
        this.fullName = fullName;
        this.alias = alias;
        this.legalPerson = legalPerson;
        this.phone = phone;
        this.status = status;
        this.regCode = regCode;
        this.orgCode = orgCode;
        this.creditCode = creditCode;
        this.taxCode = taxCode;
        this.address = address;
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegCode() {
        return regCode;
    }

    public void setRegCode(String regCode) {
        this.regCode = regCode;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getCreditCode() {
        return creditCode;
    }

    public void setCreditCode(String creditCode) {
        this.creditCode = creditCode;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
