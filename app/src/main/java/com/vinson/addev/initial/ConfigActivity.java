package com.vinson.addev.initial;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.vinson.addev.App;
import com.vinson.addev.R;
import com.vinson.addev.SplashActivity;
import com.vinson.addev.tools.Config;
import com.vinson.addev.tools.NetworkObserver;

/**
 * 1. register device info on server
 * 2. get device info from server
 * 3. save device info from server
 * 4. intent to splash activity
 */
public class ConfigActivity extends AppCompatActivity {

    public static final String TAG = ConfigActivity.class.getSimpleName();

    private static final int MSG_LAUNCH = 1;
    private static final int MSG_NETWORK_CHANGE = 4;

    private NetworkObserver mNetwork;
    private Handler mHandler = new Handler(this::handleMessage);
    private boolean mConfigDone = false;

    MaterialButton btnGetInfo, btnSaveInfo;
    TextInputEditText etDeviceId;
    ImageView mNetworkStateView;
    private IconicsDrawable mNetworkStateDrawable;
    private String mDeviceId = "";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        btnGetInfo = findViewById(R.id.btn_get_info);
        btnSaveInfo = findViewById(R.id.btn_save_info);
        etDeviceId = findViewById(R.id.et_device_id);
        mNetworkStateView = findViewById(R.id.iv_network_state);
        mNetworkStateDrawable = new IconicsDrawable(this).sizeDp(24);
        mNetworkStateView.setImageDrawable(mNetworkStateDrawable);

        initEvent();

        mNetwork = new NetworkObserver(this);
        mNetwork.register(connected -> {
            mHandler.sendEmptyMessage(MSG_NETWORK_CHANGE);
            if (connected) {
                Log.d(TAG, "network connected, verify device");
            } else {
                Log.w(TAG, "network connected but permission not granted, ignore");
            }
        });
    }

    private void initEvent() {
        etDeviceId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mDeviceId = s.toString();
            }
        });

        btnGetInfo.setOnClickListener(v -> {
            if (mDeviceId.isEmpty()) {
                App.getInstance().showToast("Device ID can not be empty!");
            } else {
                //TODO get device info
            }
        });

        btnSaveInfo.setOnClickListener(v-> {
            // TODO save device info info sharedpreference

            Config.setConfiged(true);
            mHandler.sendEmptyMessage(MSG_LAUNCH);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean handleMessage(Message msg) {
        if (isFinishing() || isDestroyed()) return true;
        switch (msg.what) {
            case MSG_LAUNCH:
                Log.d(TAG, "launch SplashActivity");
                synchronized (ConfigActivity.this) {
                    if (mConfigDone) break;
                    Intent intent = new Intent(this, SplashActivity.class);
                    startActivity(intent);
                    mNetwork.unregister();
                    ConfigActivity.this.finish();
                    mConfigDone = true;
                }
                break;
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
