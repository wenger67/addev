package com.vinson.addev;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.vinson.addev.services.WSService;
import com.vinson.addev.tools.Config;
import com.vinson.addev.tools.NetworkObserver;

import im.zego.zegoexpress.entity.ZegoUser;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private NetworkObserver mNetwork;
    ImageView mNetworkStateView;
    private IconicsDrawable mNetworkStateDrawable;
    private Handler mHandler = new Handler(this::handleMessage);
    private static final int MSG_NETWORK_CHANGE = 5;
    WSService.Binder wsService;

    MaterialButton mBtnSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNetworkStateView = findViewById(R.id.iv_network_state);
        mNetworkStateDrawable = new IconicsDrawable(this).sizeDp(24);
        mNetworkStateView.setImageDrawable(mNetworkStateDrawable);

        mNetwork = new NetworkObserver(this);
        mNetwork.register(connected -> {
            mHandler.sendEmptyMessage(MSG_NETWORK_CHANGE);
            if (connected ) {
                Log.d(TAG, "network connected, verify device");
            } else {
                Log.w(TAG, "network connected but permission not granted, ignore");
            }
        });

        Intent intent = new Intent(this, WSService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        mBtnSensor = findViewById(R.id.btn_sensors);
        initEvent();

    }

    private void initEvent() {

        mBtnSensor.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SensorActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            wsService = (WSService.Binder) service;
            mHandler.post(() -> {
                if (wsService.networkAvailable()) {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi).color(Color.GREEN);
                } else {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi_off).color(Color.RED);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            wsService.setCallback(null);
            wsService = null;
        }
    };

    private boolean handleMessage(Message msg) {
        if (isFinishing() || isDestroyed()) return true;
        switch (msg.what) {
            case MSG_NETWORK_CHANGE:
                boolean disconnected = mNetwork.disconnected();
                if (!disconnected) {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi).color(Color.GREEN);
                } else {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi_off).color(Color.RED);
                }
                break;
        }
        return false;
    }
}