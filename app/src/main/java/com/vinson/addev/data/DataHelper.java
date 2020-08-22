package com.vinson.addev.data;

import com.vinson.addev.model.request.RunningData;
import com.vinson.addev.utils.Constants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DataHelper {

    private static DataHelper INSTANCE;
    private static OkHttpClient okHttpClient;
    private DeviceService mDeviceService;
    private RunningDataService mRunningDataService;

    public DataHelper(String host) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(host)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mDeviceService = retrofit.create(DeviceService.class);
        mRunningDataService = retrofit.create(RunningDataService.class);
    }

    public static DataHelper getInstance() {
        if (INSTANCE == null) {
            return new DataHelper(Constants.BASE_URL);
        }
        return INSTANCE;
    }

    public Call<ResponseBody> getDevice(int deviceId) {
        return mDeviceService.findDevice(deviceId);
    }

    public Call<ResponseBody> createRunningData(RunningData data) {
        return mRunningDataService.createRunningData(data);
    }

    private OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }
}
