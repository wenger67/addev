package com.vinson.addev.model;

public class AdDevice {
    private int ID;
    private Category type;
    private String factoryTime;
    private String installTime;
    private Category status;
    private boolean online;
    private String lastOfflineTime;
    private String lastOnlineTime;

    public AdDevice(int ID, Category type, String factoryTime, String installTime, Category status, boolean online, String lastOfflineTime, String lastOnlineTime) {
        this.ID = ID;
        this.type = type;
        this.factoryTime = factoryTime;
        this.installTime = installTime;
        this.status = status;
        this.online = online;
        this.lastOfflineTime = lastOfflineTime;
        this.lastOnlineTime = lastOnlineTime;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Category getType() {
        return type;
    }

    public void setType(Category type) {
        this.type = type;
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

    public Category getStatus() {
        return status;
    }

    public void setStatus(Category status) {
        this.status = status;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getLastOfflineTime() {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(String lastOfflineTime) {
        this.lastOfflineTime = lastOfflineTime;
    }

    public String getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(String lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }
}
