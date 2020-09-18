package com.vinson.addev.data;

import com.vinson.addev.ObjectBox;
import com.vinson.addev.model.local.AIDetectResult;
import com.vinson.addev.model.local.FrameSavedImage;
import com.vinson.addev.model.local.LoopVideoFile;
import com.vinson.addev.model.local.ObjectDetectResult;

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
    public Box<ObjectDetectResult> objectBox;
    public Box<FrameSavedImage> savedImageBox;
    public Box<AIDetectResult> mAIDetectResultBox;

    public void init() {
        boxStore = ObjectBox.get();
        initRecordFileEntityBox();
    }

    private void initRecordFileEntityBox() {
        //对应操作对应表的类
        recordFileBox = boxStore.boxFor(LoopVideoFile.class);
        objectBox = boxStore.boxFor(ObjectDetectResult.class);
        savedImageBox = boxStore.boxFor(FrameSavedImage.class);
        mAIDetectResultBox = boxStore.boxFor(AIDetectResult.class);
    }
}
