package com.vinson.addev.model.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({ObjectChangeType.NONE, ObjectChangeType.NONE_TO_SOME, ObjectChangeType.SOME_TO_REDUCE,
        ObjectChangeType.SOME_TO_INCREASE, ObjectChangeType.SOME_TO_NONE,
        ObjectChangeType.SOME_TO_MAINTAIN})
@Retention(RetentionPolicy.SOURCE)
public @interface ObjectChangeType {
    int NONE = 1;
    int NONE_TO_SOME = 2;
    int SOME_TO_REDUCE = 3;
    int SOME_TO_INCREASE = 4;
    int SOME_TO_NONE = 5;
    int SOME_TO_MAINTAIN = 6;
}
