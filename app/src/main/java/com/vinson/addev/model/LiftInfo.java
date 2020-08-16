package com.vinson.addev.model;

import java.util.Objects;


public class LiftInfo {
    private int ID;
    private String nickName;
    private String code;
    private Company installer;
    private Company maintainer;
    private Company checker;
    private Company owner;
    private String factoryTime;
    private String installTime;
    private String checkTime;
    private LiftModel liftModel;
    private Category category;
    private Address address;
    private String location;
    private int floorCount;
    private String building;
    private int cell;
    private AdDevice adDevice;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiftInfo liftInfo = (LiftInfo) o;
        return ID == liftInfo.ID &&
                nickName.equals(liftInfo.nickName) &&
                code.equals(liftInfo.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, nickName, code);
    }

    public LiftInfo(int ID, String nickName, String code, Company installer, Company maintainer, Company checker, Company owner, String factoryTime, String installTime, String checkTime, LiftModel liftModel, Category category, Address address, String location, int floorCount, String building, int cell, AdDevice adDevice) {
        this.ID = ID;
        this.nickName = nickName;
        this.code = code;
        this.installer = installer;
        this.maintainer = maintainer;
        this.checker = checker;
        this.owner = owner;
        this.factoryTime = factoryTime;
        this.installTime = installTime;
        this.checkTime = checkTime;
        this.liftModel = liftModel;
        this.category = category;
        this.address = address;
        this.location = location;
        this.floorCount = floorCount;
        this.building = building;
        this.cell = cell;
        this.adDevice = adDevice;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(int floorCount) {
        this.floorCount = floorCount;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public int getCell() {
        return cell;
    }

    public void setCell(int cell) {
        this.cell = cell;
    }

    public AdDevice getAdDevice() {
        return adDevice;
    }

    public void setAdDevice(AdDevice adDevice) {
        this.adDevice = adDevice;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Company getInstaller() {
        return installer;
    }

    public void setInstaller(Company installer) {
        this.installer = installer;
    }

    public Company getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(Company maintainer) {
        this.maintainer = maintainer;
    }

    public Company getChecker() {
        return checker;
    }

    public void setChecker(Company checker) {
        this.checker = checker;
    }

    public Company getOwner() {
        return owner;
    }

    public void setOwner(Company owner) {
        this.owner = owner;
    }

    public String getFactoryTime() {
        return factoryTime;
    }

    public void setFactoryTime(String factoryTime) {
        this.factoryTime = factoryTime;
    }

    public String getInstallTime() {
        return installTime;
    }

    public void setInstallTime(String installTime) {
        this.installTime = installTime;
    }

    public String getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(String checkTime) {
        this.checkTime = checkTime;
    }

    public LiftModel getLiftModel() {
        return liftModel;
    }

    public void setLiftModel(LiftModel liftModel) {
        this.liftModel = liftModel;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    @Override
    public String toString() {
        return "LiftInfo{" +
                "ID=" + ID +
                ", nickName='" + nickName + '\'' +
                ", code='" + code + '\'' +
                ", installer=" + installer +
                ", maintainer=" + maintainer +
                ", checker=" + checker +
                ", owner=" + owner +
                ", factoryTime='" + factoryTime + '\'' +
                ", installTime='" + installTime + '\'' +
                ", checkTime='" + checkTime + '\'' +
                ", liftModel=" + liftModel +
                ", category=" + category +
                ", address=" + address +
                ", location='" + location + '\'' +
                ", floorCount=" + floorCount +
                ", building='" + building + '\'' +
                ", cell=" + cell +
                ", adDevice=" + adDevice +
                '}';
    }
}
