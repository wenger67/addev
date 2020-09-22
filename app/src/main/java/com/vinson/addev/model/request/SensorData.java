package com.vinson.addev.model.request;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class SensorData {
    @Id
    public long id;
    public float accx;
    public float accy;
    public float accz;
    public float degx;
    public float degy;
    public float degz;
    public float speedz;
    public float temp;
    public float height;
    public long createdAt;

    public SensorData() {
    }

    public SensorData(float accx, float accy, float accz, float degx, float degy, float degz,
                      float speedz, float temp, float height) {
        this.accx = accx;
        this.accy = accy;
        this.accz = accz;
        this.degx = degx;
        this.degy = degy;
        this.degz = degz;
        this.speedz = speedz;
        this.temp = temp;
        this.height = height;
        this.createdAt = System.currentTimeMillis();
    }
}
