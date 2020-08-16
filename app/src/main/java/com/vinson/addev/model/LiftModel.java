package com.vinson.addev.model;

public class LiftModel {
    private int ID;
    private Company company;
    private String brand;
    private String modal;
    private int load;

    public LiftModel(int ID, Company company, String brand, String modal, int load) {
        this.ID = ID;
        this.company = company;
        this.brand = brand;
        this.modal = modal;
        this.load = load;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(String modal) {
        this.modal = modal;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }
}
