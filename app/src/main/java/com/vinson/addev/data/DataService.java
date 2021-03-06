package com.vinson.addev.data;

import com.vinson.addev.model.request.SensorData;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DataService {

    @POST("/dev/data/createRunningData")
    Call<ResponseBody> createRunningData(@Body SensorData data);
}
