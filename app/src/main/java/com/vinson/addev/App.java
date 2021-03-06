package com.vinson.addev;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.socks.library.KLog;
import com.tencent.bugly.crashreport.CrashReport;
import com.vinson.addev.data.DataManager;
import com.vinson.addev.services.WSService;
import com.vinson.addev.tools.CrashHandler;
import com.vinson.addev.utils.ToastCompat;
import com.xdandroid.hellodaemon.DaemonEnv;

import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoScenario;

public class App extends Application {
    private static final String SP_NAME = "config";
    private static App INSTANCE;
    private SharedPreferences sp;
    private static ZegoExpressEngine ENGINE;
    public Gson mGson;
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mGson = new Gson();
        // register crash handler
        CrashHandler.getInstance().init();
        CrashReport.initCrashReport(getApplicationContext(), "3b268d3e09", true);
        // NOSQL orm
        ObjectBox.init(this);
        DataManager.get().init();
        // logcat
        KLog.init(true);
        // daemon service initial
        DaemonEnv.initialize(this, WSService.class, null);
        // start sensor data service
        startService(new Intent(this, WSService.class));

        ENGINE = ZegoExpressEngine.createEngine(BuildConfig.ZegoAppId, BuildConfig.ZegoAppSign,
                true, ZegoScenario.GENERAL, this, null);
    }

    public static App getInstance() {
        return INSTANCE;
    }
    public static ZegoExpressEngine getEngine() {
        return ENGINE;
    }

    public void showToast(String msg) {
        Log.d("App", "showToast:" + msg);
        ToastCompat.showToast(this, msg);
    }

    public SharedPreferences getSP() {
        return sp;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ZegoExpressEngine.destroyEngine(null);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
