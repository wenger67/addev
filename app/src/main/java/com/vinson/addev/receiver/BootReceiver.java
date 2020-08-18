package com.vinson.addev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vinson.addev.App;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        Log.d(TAG, "onReceive: " + intent.toString());
        if (intent.getAction().equals("com.vinson.addev.wsservice.restart")) {
            // TODO restart wsservice
            App.getInstance().startService(new Intent("com.vinson.addev.wsservice"));
        }
    }
}
