package com.example.tcmhaa.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface GenericApiService {
    @POST
    <T> Call<ResponseBody> postData(@Url String url, @Body T body);
}
