package com.vinson.addev.serialport;

import android.content.SharedPreferences;

import com.vinson.addev.App;

public class SerialConfig {

    public static final String SP_KEY_SERIAL_PATH = "serial.path";
    public static final String SP_KEY_SERIAL_BAUDRATE = "serial.baudrate";

    public static String getSerialPath() {
        return App.getInstance().getSP().getString(SP_KEY_SERIAL_PATH, "/dev/es_tty0");
    }

    public static void setSerialPath(String path) {
        if (path.equals(getSerialPath())) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putString(SP_KEY_SERIAL_PATH, path);
        editor.apply();
    }

    public static int getSerialBau() {
        return App.getInstance().getSP().getInt(SP_KEY_SERIAL_BAUDRATE, -1);
    }

    public static void setSerialBau(int bad) {
        if (bad == getSerialBau()) return;
        SharedPreferences.Editor editor = App.getInstance().getSP().edit();
        editor.putInt(SP_KEY_SERIAL_BAUDRATE, bad);
        editor.apply();
    }


}
