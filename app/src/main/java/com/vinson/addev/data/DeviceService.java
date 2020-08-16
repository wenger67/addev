package com.vinson.addev.data;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DeviceService {

    @GET("/dev/device/findDevice")
    Call<ResponseBody> findDevice(@Query("ID") int deviceId);
}
