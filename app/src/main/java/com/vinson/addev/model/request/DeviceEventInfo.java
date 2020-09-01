package com.vinson.addev.model.request;

import com.vinson.addev.model.annotation.DeviceEventId;
import com.vinson.addev.model.annotation.UploadStorageType;

public class DeviceEventInfo {
    /**
     * local/qiniu
     */
    @UploadStorageType
    public String storage;
    @DeviceEventId
    public int typeId;
    public int deviceId;

    @Override
    public String toString() {
        return "DeviceEventInfo{" +
                "storage='" + storage + '\'' +
                ", typeId=" + typeId +
                ", deviceId=" + deviceId +
                '}';
    }
}
