package com.vinson.addev.data;

import com.vinson.addev.model.request.RunningData;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RunningDataService {

    @POST("/dev/data/createRunningData")
    Call<ResponseBody> createRunningData(@Body RunningData data);
}
