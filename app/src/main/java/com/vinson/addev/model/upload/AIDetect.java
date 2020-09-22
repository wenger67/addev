package com.vinson.addev.model.upload;

import com.vinson.addev.model.annotation.ObjectChangeType;

import java.util.List;

public class AIDetect {
    public long id;
    public long createdAt;
    public int personCount;
    public int motorCount;
    public int personDelta;
    public int motorDelta;
    @ObjectChangeType
    public int personChange;
    @ObjectChangeType
    public int motorChange;
    public List<ObjectDetect> objects;

    public AIDetect(long id, long createdAt, int personCount, int motorCount, int personDelta,
                    int motorDelta, int personChange, int motorChange, List<ObjectDetect> objects) {
        this.id = id;
        this.createdAt = createdAt;
        this.personCount = personCount;
        this.motorCount = motorCount;
        this.personDelta = personDelta;
        this.motorDelta = motorDelta;
        this.personChange = personChange;
        this.motorChange = motorChange;
        this.objects = objects;
    }
}
