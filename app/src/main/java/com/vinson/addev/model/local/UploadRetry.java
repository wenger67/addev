package com.vinson.addev.model.local;

import java.util.List;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class UploadRetry {
    @Id
    public long id;
    public List<FrameSavedImage> images;
    public List<LoopVideoFile> videos;
    public List<ObjectDetectResult> results;

    public UploadRetry(List<FrameSavedImage> images, List<LoopVideoFile> videos,
                       List<ObjectDetectResult> results) {
        this.images = images;
        this.videos = videos;
        this.results = results;
    }
}
