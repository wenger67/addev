package com.vinson.addev;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textview.MaterialTextView;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.socks.library.KLog;
import com.vinson.addev.initial.ConfigActivity;
import com.vinson.addev.tools.Config;
import com.vinson.addev.tools.NetworkObserver;
import com.wang.avi.AVLoadingIndicatorView;

public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getSimpleName();

    private static final String[] mPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE_DEFAULT = 1;
    private static final int MSG_LAUNCH_MAIN = 1;
    private static final int MSG_LAUNCH_CONFIG = 2;
    private static final int MSG_ERROR_VERIFY_FAILED = 3;
    private static final int MSG_VERIFY_DEVICE = 4;
    private static final int MSG_NETWORK_CHANGE = 5;
    ImageView mNetworkStateView;
    private boolean mPermissionGranted = false;
    private boolean mSplashDone = false;
    private NetworkObserver mNetwork;
    private IconicsDrawable mNetworkStateDrawable;
    private AVLoadingIndicatorView mLoadingView;
    private MaterialTextView mTipsView;
    private int mTick;
    private Handler mHandler = new Handler(this::handleMessage);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        mNetworkStateView = findViewById(R.id.iv_network_state);
        mNetworkStateDrawable = new IconicsDrawable(this).sizeDp(24);
        mNetworkStateView.setImageDrawable(mNetworkStateDrawable);

        mLoadingView = findViewById(R.id.av_loading_view);
        mTipsView = findViewById(R.id.loading_tips);

        mNetwork = new NetworkObserver(this);
        mNetwork.register(connected -> {
            mHandler.sendEmptyMessage(MSG_NETWORK_CHANGE);
            if (connected && mPermissionGranted) {
                Log.d(TAG, "network connected, verify device");
                mHandler.removeMessages(MSG_VERIFY_DEVICE);
                mHandler.sendEmptyMessage(MSG_VERIFY_DEVICE);
            } else if (!mPermissionGranted) {
                Log.w(TAG, "network connected but permission not granted, ignore");
            }
        });
    }

    @Override
    protected void onResume() {
        KLog.d();
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(SplashActivity.this)) {
                KLog.d("canDrawOverlays");
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            } else {
                if (checkPermission()) {
                    if (!Config.getConfiged()) {
                        KLog.d("configactivity");
                        // launch configactivity
                        mHandler.removeCallbacksAndMessages(null);
                        mHandler.sendEmptyMessage(MSG_LAUNCH_CONFIG);
                    } else {
                        mHandler.removeMessages(MSG_VERIFY_DEVICE);
                        mHandler.sendEmptyMessage(MSG_VERIFY_DEVICE);
                    }
                } else {
                    Log.w(TAG, "permission not granted, raise up request tips");
                    requestPermissions(mPermissions, REQUEST_CODE_DEFAULT);
                }
            }
        } else {
            if (!Config.getConfiged()) {
                KLog.d("configactivity");
                // launch configactivity
                mHandler.removeCallbacksAndMessages(null);
                mHandler.sendEmptyMessage(MSG_LAUNCH_CONFIG);
            } else {
                mHandler.removeMessages(MSG_VERIFY_DEVICE);
                mHandler.sendEmptyMessage(MSG_VERIFY_DEVICE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkPermission() {
        for (String permission : mPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        mPermissionGranted = true;
        return true;
    }


    private void verifyFailed() {
        Log.d(TAG, "verifyFailed");
        if (mLoadingView.getVisibility() == View.VISIBLE)
            mLoadingView.smoothToHide();

        if (mTick == 0) {
            verifyDevice();
        } else {
            String tips = "设备认证失败," + mTick + "s后重试...";
            mTipsView.setText(tips);
            if (mTick % 10 == 0) {
                Log.w(TAG, tips);
                Log.d(TAG, "device verify failed, retry in " + mTick + "s");
            }
            mTick -= 1;
            mHandler.removeCallbacksAndMessages(null);
            mHandler.sendEmptyMessageDelayed(MSG_ERROR_VERIFY_FAILED, 1000);
        }
    }

    private void verifyDevice() {
        // TODO check if need upgrade apk
        // TODO check last verify timestamp

        if (mTipsView.getVisibility() != View.VISIBLE)
            mTipsView.setVisibility(View.VISIBLE);

        mHandler.removeCallbacksAndMessages(null);
        mTick = 0;

        if (mNetwork.disconnected()) {
            mTipsView.setText("设备认证失败,无可用网络");
            return;
        } else {
            mTipsView.setText("verifying ...... ");
            mLoadingView.smoothToShow();
        }

        // TODO request device info
        // TODO if request failed, then retry by set mTick and send MSG_ERROR_VERIFY_FAILED
        // TODO verify succuss, launch mainactivity
        mHandler.removeCallbacksAndMessages(null);
//        mHandler.sendEmptyMessage(MSG_LAUNCH_MAIN);
        SplashActivity.this.finish();
    }

    private boolean handleMessage(Message msg) {
        if (isFinishing() || isDestroyed()) return true;
        switch (msg.what) {
            case MSG_LAUNCH_MAIN:
                Log.d(TAG, "launch MainActivity");
                synchronized (SplashActivity.this) {
                    if (mSplashDone) break;
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    mNetwork.unregister();
                    SplashActivity.this.finish();
                    mSplashDone = true;
                }
                break;
            case MSG_LAUNCH_CONFIG:
                Log.d(TAG, "launch ConfigActivity");
                synchronized (SplashActivity.this) {
                    Intent intent = new Intent(this, ConfigActivity.class);
                    startActivity(intent);
                    mNetwork.unregister();
                    SplashActivity.this.finish();
                }
                break;
            case MSG_ERROR_VERIFY_FAILED:
                verifyFailed();
                break;
            case MSG_VERIFY_DEVICE:
                verifyDevice();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_DEFAULT) {
            boolean granted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                mPermissionGranted = true;
                if (!Config.getConfiged()) {
                    // launch configactivity
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.sendEmptyMessage(MSG_LAUNCH_CONFIG);
                } else {
                    mHandler.removeMessages(MSG_VERIFY_DEVICE);
                    mHandler.sendEmptyMessage(MSG_VERIFY_DEVICE);
                }
            } else {
                Log.e(TAG, "permission request rejected");
                App.getInstance().showToast("无法获取摄像头权限和媒体访问权限");
                this.finish();
            }
        }
    }

}
