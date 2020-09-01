package com.vinson.addev.model.annotation;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({DeviceEventId.ONLINE, DeviceEventId.OFFLINE, DeviceEventId.MOTOR_DETECTED, DeviceEventId.PERSON_DETECTED})
@Retention(RetentionPolicy.SOURCE)
public @interface DeviceEventId {
    int ONLINE = 164;
    int OFFLINE = 165;
    int MOTOR_DETECTED = 180;
    int PERSON_DETECTED = 181;
}
