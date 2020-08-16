package com.vinson.addev.model;

public class Region {
    private int ID;
    private String province;
    private String city;
    private String district;

    public Region(int ID, String province, String city, String district) {
        this.ID = ID;
        this.province = province;
        this.city = city;
        this.district = district;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }
}
