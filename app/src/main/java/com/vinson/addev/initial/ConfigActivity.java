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
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.vinson.addev.App;
import com.vinson.addev.R;
import com.vinson.addev.SplashActivity;
import com.vinson.addev.data.DataHelper;
import com.vinson.addev.model.LiftInfo;
import com.vinson.addev.model.Result;
import com.vinson.addev.tools.Config;
import com.vinson.addev.tools.NetworkObserver;
import com.vinson.addev.utils.Constants;

import java.io.IOException;
import java.util.Arrays;
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

    public static final String TAG = ConfigActivity.class.getSimpleName();

    private static final int MSG_LAUNCH = 1;
    private static final int MSG_NETWORK_CHANGE = 4;

    private NetworkObserver mNetwork;
    private Handler mHandler = new Handler(this::handleMessage);
    private boolean mConfigDone = false;

    MaterialButton btnGetInfo, btnSaveInfo;
    TextInputEditText etDeviceId;
    MaterialTextView tvLiftInfo;

    ImageView mNetworkStateView;
    private IconicsDrawable mNetworkStateDrawable;
    private String mDeviceId = "";
    private LiftInfo liftInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        btnGetInfo = findViewById(R.id.btn_get_info);
        btnSaveInfo = findViewById(R.id.btn_save_info);
        etDeviceId = findViewById(R.id.et_device_id);
        tvLiftInfo = findViewById(R.id.tv_lift_info);

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
            Log.d(TAG, mDeviceId + " " + Integer.parseInt(mDeviceId));
            if (mDeviceId.isEmpty()) {
                App.getInstance().showToast("Device ID can not be empty!");
            } else {
                DataHelper dataHelper = new DataHelper(Constants.BASE_URL);
                dataHelper.getDevice(Integer.parseInt(mDeviceId)).enqueue(new Callback<ResponseBody>() {
                    @Override
                    @EverythingIsNonNull
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        ResponseBody result = response.body();
                        assert result != null;
                        try {
                            JsonObject object = new JsonParser().parse(result.string()).getAsJsonObject();
                            JsonElement liftElement = object.get("data").getAsJsonObject().get("lift");
                            liftInfo = new Gson().fromJson(liftElement, new TypeToken<LiftInfo>(){}.getType());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, liftInfo.toString());

                        String builder = "ID:" + liftInfo.getID() + "\n" + "别名:" + liftInfo.getNickName() + "\n" +
                                "编码:" + liftInfo.getCode() + "\n" +
                                "使用单位:" + liftInfo.getOwner().getFullName() + "\n" +
                                "地址" + liftInfo.getAddress().getAddressName() + liftInfo.getBuilding() + "栋" +
                                liftInfo.getCell() + "单元";
                        tvLiftInfo.setText(builder);
                    }

                    @Override
                    @EverythingIsNonNull
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d(TAG, "onFailure");
                        Log.d(TAG, Objects.requireNonNull(t.getMessage()));
                        Log.d(TAG, Arrays.toString(t.getStackTrace()));
                    }
                });
            }
        });

        btnSaveInfo.setOnClickListener(v-> {
            Config.setConfiged(true);
            Config.setDeviceSerial(Constants.PREFIX + liftInfo.getID());
            Config.setLiftInfo(liftInfo);
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
