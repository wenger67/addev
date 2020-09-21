package com.vinson.addev.tools;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vinson.addev.App;
import com.vinson.addev.model.LiftInfo;
import com.vinson.addev.utils.Constants;

public class Config {

    public static boolean getConfiged() {
        return App.getInstance().getSP().getBoolean(Constants.SP_KEY_CONFIGED, false);
    }

    public static void setConfiged(boolean configed) {
        if (configed == getConfiged()) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putBoolean(Constants.SP_KEY_CONFIGED, configed);
        editor.apply();
    }

    public static LiftInfo getLiftInfo() {
        String json = App.getInstance().getSP().getString(Constants.SP_KEY_LIFT_INFO, "");
        return new Gson().fromJson(json, new TypeToken<LiftInfo>() {
        }.getType());
    }

    public static void setLiftInfo(LiftInfo liftInfo) {
        if (liftInfo.equals(getLiftInfo())) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        String json = new Gson().toJson(liftInfo);
        editor.putString(Constants.SP_KEY_LIFT_INFO, json);
        editor.apply();
    }

    public static String getDeviceSerial() {
        return Constants.PREFIX_LIFT + "_" + getLiftInfo().getID();
    }

    public static String getStreamId() {
        return App.getInstance().getSP().getString(Constants.SP_KEY_STREAM_ID, "");
    }

    public static void setStreamId(String serial) {
        if (serial.equals(getStreamId())) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putString(Constants.SP_KEY_STREAM_ID, serial);
        editor.apply();
    }

    public static float getInitFloor() {
        return App.getInstance().getSP().getFloat(Constants.SP_KEY_INIT_FLOOR, -100);
    }

    public static void setInitFloor(float floor) {
        if (floor == getInitFloor()) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putFloat(Constants.SP_KEY_INIT_FLOOR, floor);
        editor.apply();
    }

    public static float getInitHeight() {
        return App.getInstance().getSP().getFloat(Constants.SP_KEY_INIT_HEIGHT, -100);
    }

    public static void setInitHeight(float height) {
        if (height == getInitHeight()) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putFloat(Constants.SP_KEY_INIT_HEIGHT, height);
        editor.apply();
    }
}
