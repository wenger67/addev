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
}
