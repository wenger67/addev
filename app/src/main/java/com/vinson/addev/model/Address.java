package com.vinson.addev.model;

public class Address {
    private int ID;
    private Region region;
    private String addressName;
    private String location;
    private int userAmount;

    public Address(int ID, Region region, String addressName, String location, int userAmount) {
        this.ID = ID;
        this.region = region;
        this.addressName = addressName;
        this.location = location;
        this.userAmount = userAmount;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getUserAmount() {
        return userAmount;
    }

    public void setUserAmount(int userAmount) {
        this.userAmount = userAmount;
    }
}
