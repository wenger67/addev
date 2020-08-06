package com.vinson.addev;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.vinson.addev.tools.CrashHandler;
import com.vinson.addev.utils.ToastCompat;

public class App extends Application {
    private static final String SP_NAME = "config";
    private static App INSTANCE;
    private SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        // register crash handler
        CrashHandler.getInstance().init();
    }

    public static App getInstance() {
        return INSTANCE;
    }

    public void showToast(String msg) {
        Log.d("App", "showToast:" + msg);
        ToastCompat.showToast(this, msg);
    }

    public SharedPreferences getSP() {
        return sp;
    }
}
