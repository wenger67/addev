package com.vinson.addev.model.upload;

public class ObjectDetect {
    public long id;
    public long createdAt;
    public String title;
    public float confidence;
    public float left;
    public float top;
    public float right;
    public float bottom;

    public ObjectDetect(long id, long createdAt, String title, float confidence, float left,
                        float top, float right, float bottom) {
        this.id = id;
        this.createdAt = createdAt;
        this.title = title;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
