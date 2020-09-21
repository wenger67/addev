package com.vinson.addev.model.local;

import android.graphics.RectF;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ObjectDetectResult {
    @Id
    public long id;
    public long createdAt;
    public String title;
    public float confidence;
    public float left;
    public float top;
    public float right;
    public float bottom;
    public ToOne<AIDetectResult> detectResult;

    public ObjectDetectResult() {
    }

    public ObjectDetectResult(String title, float confidence, float left, float top, float right,
                              float bottom) {
        this.title = title;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.createdAt = System.currentTimeMillis();
    }

    public ObjectDetectResult(long createdAt, String title, float confidence, float left,
                              float top, float right, float bottom) {
        this.createdAt = createdAt;
        this.title = title;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
