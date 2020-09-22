package com.vinson.addev.data;

import com.vinson.addev.ObjectBox;
import com.vinson.addev.model.local.AIDetectResult;
import com.vinson.addev.model.local.FrameSavedImage;
import com.vinson.addev.model.local.LoopVideoFile;
import com.vinson.addev.model.local.ObjectDetectResult;
import com.vinson.addev.model.request.SensorData;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class DataManager {
     private static DataManager dataManager;

    public static synchronized DataManager get() {
        if (dataManager == null) {
            dataManager = new DataManager();
        }
        return dataManager;
    }

    public BoxStore boxStore;
    public Box<LoopVideoFile> recordFileBox;
    public Box<FrameSavedImage> savedImageBox;
    public Box<AIDetectResult> detectResultBox;
    public Box<SensorData> sensorDataBox;

    public void init() {
        boxStore = ObjectBox.get();
        initRecordFileEntityBox();
    }

    private void initRecordFileEntityBox() {
        //对应操作对应表的类
        recordFileBox = boxStore.boxFor(LoopVideoFile.class);
        savedImageBox = boxStore.boxFor(FrameSavedImage.class);
        detectResultBox = boxStore.boxFor(AIDetectResult.class);
        sensorDataBox = boxStore.boxFor(SensorData.class);
    }
}
