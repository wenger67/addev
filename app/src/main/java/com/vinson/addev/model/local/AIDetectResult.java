package com.vinson.addev.model.local;

import android.os.SystemClock;

import com.vinson.addev.model.annotation.ObjectChangeType;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class AIDetectResult {
    @Id
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

    @Backlink
    public ToMany<ObjectDetectResult> objects;

    public AIDetectResult() {
        this.createdAt = System.currentTimeMillis();
        this.personCount = 0;
        this.motorCount = 0;
        this.personChange = ObjectChangeType.NONE;
        this.motorChange = ObjectChangeType.NONE;
    }

    public AIDetectResult(int personCount, int motorCount) {
        this.createdAt = System.currentTimeMillis();
        this.personCount = personCount;
        this.motorCount = motorCount;
        personDelta = 0;
        motorDelta = 0;
    }

    public AIDetectResult(int personCount, int motorCount, int personChange, int motorChange) {
        this.createdAt = System.currentTimeMillis();
        this.personCount = personCount;
        this.motorCount = motorCount;
        this.personChange = personChange;
        this.motorChange = motorChange;
    }

    public AIDetectResult(long createdAt, int personCount, int motorCount, int personChange,
                          int motorChange) {
        this.createdAt = createdAt;
        this.personCount = personCount;
        this.motorCount = motorCount;
        this.personChange = personChange;
        this.motorChange = motorChange;
    }

    @Override
    public String toString() {
        return "AIDetectResult{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", personCount=" + personCount +
                ", motorCount=" + motorCount +
                ", personChange=" + personChange +
                ", motorChange=" + motorChange +
                '}';
    }
}
