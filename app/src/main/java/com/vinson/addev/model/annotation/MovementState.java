package com.vinson.addev.model.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.vinson.addev.model.annotation.MovementState.MOVEMENT_ACCELERATE;
import static com.vinson.addev.model.annotation.MovementState.MOVEMENT_DECELERATE;
import static com.vinson.addev.model.annotation.MovementState.MOVEMENT_NONE;
import static com.vinson.addev.model.annotation.MovementState.MOVEMENT_STABLE;
import static com.vinson.addev.model.annotation.MovementState.MOVEMENT_STATIC;

@IntDef({MOVEMENT_NONE, MOVEMENT_STATIC, MOVEMENT_ACCELERATE, MOVEMENT_DECELERATE, MOVEMENT_STABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface MovementState {
    int MOVEMENT_NONE = -1;
    int MOVEMENT_STATIC = 1;
    int MOVEMENT_ACCELERATE = 2;
    int MOVEMENT_STABLE = 3;
    int MOVEMENT_DECELERATE = 4;
}
