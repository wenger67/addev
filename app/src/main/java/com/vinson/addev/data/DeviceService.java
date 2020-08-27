package com.vinson.addev.data;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface DeviceService {

    @GET("/dev/device/findDevice")
    Call<ResponseBody> findDevice(@Query("ID") int deviceId);

    @Multipart
    @POST("/fileUploadAndDownload/uploadList")
    Call<ResponseBody> uploadFiles(@Query("storage") String storage,
                                   @Part List<MultipartBody.Part> files);
}
