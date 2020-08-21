package com.vinson.addev.utils;

import android.util.Size;

public class TfLiteUtil {
    public static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    public static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    public static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    public static final int TF_OD_API_INPUT_SIZE = 300;
    public static final boolean TF_OD_API_IS_QUANTIZED = true;
}
