package com.vinson.addev.model.annotation;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import static com.vinson.addev.model.annotation.UploadStorageType.LOCAL;
import static com.vinson.addev.model.annotation.UploadStorageType.QINIU;

@StringDef({LOCAL, QINIU})
@Retention(RetentionPolicy.SOURCE)
public @interface UploadStorageType {
    String LOCAL = "local";
    String QINIU = "qiniu";
}
