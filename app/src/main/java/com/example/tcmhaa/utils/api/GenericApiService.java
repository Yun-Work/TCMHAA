package com.example.tcmhaa.utils.api;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface GenericApiService {
//    @Headers("Content-Type: application/json")
    @POST
    Call<ResponseBody> postData(@Url String path, @Body RequestBody body);
}
