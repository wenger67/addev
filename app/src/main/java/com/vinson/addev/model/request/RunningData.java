package com.vinson.addev.model.request;

public class RunningData {
    int deviceId;
    int troubleId;  // 145:normal
    float accx;
    float accy;
    float accz;
    float degx;
    float degy;
    float degz;
    float speedz;
    float floor;
    int doorStateId; // 158:normal
    boolean peopleInSide;

    public RunningData(int deviceId, int troubleId, float accx, float accy, float accz,
                       float degx, float degy, float degz, float speedz, float floor,
                       int doorStateId, boolean peopleInSide) {
        this.deviceId = deviceId;
        this.troubleId = troubleId;
        this.accx = accx;
        this.accy = accy;
        this.accz = accz;
        this.degx = degx;
        this.degy = degy;
        this.degz = degz;
        this.speedz = speedz;
        this.floor = floor;
        this.doorStateId = doorStateId;
        this.peopleInSide = peopleInSide;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getTroubleId() {
        return troubleId;
    }

    public void setTroubleId(int troubleId) {
        this.troubleId = troubleId;
    }

    public float getAccx() {
        return accx;
    }

    public void setAccx(float accx) {
        this.accx = accx;
    }

    public float getAccy() {
        return accy;
    }

    public void setAccy(float accy) {
        this.accy = accy;
    }

    public float getAccz() {
        return accz;
    }

    public void setAccz(float accz) {
        this.accz = accz;
    }

    public float getDegx() {
        return degx;
    }

    public void setDegx(float degx) {
        this.degx = degx;
    }

    public float getDegy() {
        return degy;
    }

    public void setDegy(float degy) {
        this.degy = degy;
    }

    public float getDegz() {
        return degz;
    }

    public void setDegz(float degz) {
        this.degz = degz;
    }

    public float getSpeedz() {
        return speedz;
    }

    public void setSpeedz(float speedz) {
        this.speedz = speedz;
    }

    public float getFloor() {
        return floor;
    }

    public void setFloor(float floor) {
        this.floor = floor;
    }

    public int getDoorStateId() {
        return doorStateId;
    }

    public void setDoorStateId(int doorStateId) {
        this.doorStateId = doorStateId;
    }

    public boolean isPeopleInSide() {
        return peopleInSide;
    }

    public void setPeopleInSide(boolean peopleInSide) {
        this.peopleInSide = peopleInSide;
    }
}
