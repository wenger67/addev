package com.vinson.addev.model.local;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * static -> accelerate -> stable -> decelerate -> static
 */

@Entity
public class Movement {
    @Id
    public long id;
    public float startHeight;
    public long startTime;
    public long accelerateEndTime;
    public long stableEndTime;
    public long decelerateEndTime;
    public long endHeight;

    public String startData;
    public String accelerateData;
    public String stableData;
    public String decelerateData;
    public String endData;

    public Movement() {
    }
}
