package com.vinson.addev.initial;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.socks.library.KLog;
import com.vinson.addev.App;
import com.vinson.addev.R;
import com.vinson.addev.SplashActivity;
import com.vinson.addev.data.DataHelper;
import com.vinson.addev.data.DataManager;
import com.vinson.addev.model.LiftInfo;
import com.vinson.addev.model.request.SensorData;
import com.vinson.addev.model.request.SensorData_;
import com.vinson.addev.services.RecorderService;
import com.vinson.addev.tools.Config;
import com.vinson.addev.tools.NetworkObserver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.ResponseBody;
import okhttp3.internal.annotations.EverythingIsNonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 1. register device info on server
 * 2. get device info from server
 * 3. save device info from server
 * 4. intent to splash activity
 */
public class ConfigActivity extends AppCompatActivity {

    private static final int MSG_LAUNCH_SPLASH = 1;
    private static final int MSG_NETWORK_CHANGE = 4;
    MaterialButton btnGetInfo, btnSaveInfo;
    TextInputEditText etDeviceId;
    MaterialTextView tvLiftInfo;
    ImageView mNetworkStateView;
    MaterialTextView mHeightTv;
    TextInputEditText mFloorEt;
    MaterialButton mUpdate;
    float initFloor;
    float initHeight;
    private NetworkObserver mNetwork;
    private boolean mConfigDone = false;
    private IconicsDrawable mNetworkStateDrawable;
    private Handler mHandler = new Handler(this::handleMessage);
    private String mDeviceId = "";
    private LiftInfo liftInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        KLog.d();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        btnGetInfo = findViewById(R.id.btn_get_info);
        btnSaveInfo = findViewById(R.id.btn_save_info);
        etDeviceId = findViewById(R.id.et_device_id);
        tvLiftInfo = findViewById(R.id.tv_lift_info);

        mHeightTv = findViewById(R.id.tv_height);
        mFloorEt = findViewById(R.id.et_floor);
        mUpdate = findViewById(R.id.btn_update);

        mNetworkStateView = findViewById(R.id.iv_network_state);
        mNetworkStateDrawable = new IconicsDrawable(this).sizeDp(24);
        mNetworkStateView.setImageDrawable(mNetworkStateDrawable);

        initEvent();

        mNetwork = new NetworkObserver(this);
        mNetwork.register(connected -> {
            mHandler.sendEmptyMessage(MSG_NETWORK_CHANGE);
            if (connected) {
                KLog.d("network connected, verify device");
            } else {
                KLog.w("network connected but permission not granted, ignore");
            }
        });
    }

    private void updateHeight() {
        List<SensorData> data =
                DataManager.get().sensorDataBox.query().orderDesc(SensorData_.createdAt).
                build().find(0, 10);
        float sum = 0;
        for (SensorData item : data) {
            sum += item.height;
        }
        initHeight = sum / data.size();
        mHeightTv.setText(initHeight + " m");
    }

    private void initEvent() {
        // achieve init height
        mHeightTv.postDelayed(this::updateHeight, 2000);
        mUpdate.setOnClickListener(v -> updateHeight());

        // assign init floor
        mFloorEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                initFloor = Float.parseFloat(s.toString());
            }
        });

        // get lift info
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
                KLog.d(mDeviceId + " " + Integer.parseInt(mDeviceId));
                DataHelper.getInstance().getDevice(Integer.parseInt(mDeviceId)).enqueue(new Callback<ResponseBody>() {
                    @Override
                    @EverythingIsNonNull
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        ResponseBody result = response.body();
                        assert result != null;
                        try {
                            JsonObject object =
                                    new JsonParser().parse(result.string()).getAsJsonObject();
                            JsonElement liftElement = object.get("data").getAsJsonObject().get(
                                    "lift");
                            liftInfo = new Gson().fromJson(liftElement, new TypeToken<LiftInfo>() {
                            }.getType());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        KLog.d(liftInfo.toString());

                        String builder =
                                "ID:" + liftInfo.getID() + "\n" + "别名:" + liftInfo.getNickName() + "\n" +
                                "编码:" + liftInfo.getCode() + "\n" +
                                "使用单位:" + liftInfo.getOwner().getFullName() + "\n" +
                                "地址" + liftInfo.getAddress().getAddressName() + liftInfo.getBuilding() + "栋" +
                                liftInfo.getCell() + "单元";
                        tvLiftInfo.setText(builder);
                    }

                    @Override
                    @EverythingIsNonNull
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        KLog.d("onFailure");
                        KLog.d(Objects.requireNonNull(t.getMessage()));
                        KLog.d(Arrays.toString(t.getStackTrace()));
                    }
                });
            }
        });

        // save init config information
        btnSaveInfo.setOnClickListener(v -> {
            Config.setConfiged(true);
            Config.setInitFloor(initFloor);
            String height = mHeightTv.getText().toString();
            try {
                float h = Float.parseFloat(height);
                Config.setInitHeight(h);
            } catch (Exception e) {
                Config.setInitHeight(1000);
            }
            Config.setLiftInfo(liftInfo);
            RecorderService.startObjectDetect(this);
            mHandler.sendEmptyMessage(MSG_LAUNCH_SPLASH);
        });
    }

    @Override
    protected void onResume() {
        KLog.d();
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
            case MSG_LAUNCH_SPLASH:
                KLog.d("launch SplashActivity");
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
