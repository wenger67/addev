package com.vinson.addev.tools;

import android.content.SharedPreferences;

import com.vinson.addev.App;
import com.vinson.addev.utils.Constants;

public class Config {

    public static void setConfiged(boolean configed) {
        if (configed == getConfiged()) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putBoolean(Constants.SP_KEY_CONFIGED, configed);
        editor.apply();
    }

    public static boolean getConfiged() {
        return App.getInstance().getSP().getBoolean(Constants.SP_KEY_CONFIGED, false);
    }

    public static void setDeviceSerial(String serial) {
        if (serial.equals(getDeviceSerial())) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putString(Constants.SP_KEY_DEVICE_SERIAL, serial);
        editor.apply();
    }

    public static String getDeviceSerial() {
        return App.getInstance().getSP().getString(Constants.SP_KEY_DEVICE_SERIAL, "");
    }

    public static void setStreamId(String serial) {
        if (serial.equals(getStreamId())) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putString(Constants.SP_KEY_STREAM_ID, serial);
        editor.apply();
    }

    public static String getStreamId() {
        return App.getInstance().getSP().getString(Constants.SP_KEY_STREAM_ID, "");
    }
}
