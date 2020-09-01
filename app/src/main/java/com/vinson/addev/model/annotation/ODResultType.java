package com.vinson.addev.model.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.vinson.addev.model.annotation.ODResultType.NONE;
import static com.vinson.addev.model.annotation.ODResultType.PERSON_WITH_MOTOR;


@IntDef({NONE, ODResultType.ONLY_PERSON, ODResultType.ONLY_MOTOR, PERSON_WITH_MOTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface ODResultType {
    int NONE = -1;
    int ONLY_PERSON = 0;
    int ONLY_MOTOR = 1;
    int PERSON_WITH_MOTOR = 2;
}