package com.example.tcmhaa.api;


import com.example.tcmhaa.api.dto.LoginRequest;
import com.example.tcmhaa.api.dto.LoginResponse;
import com.example.tcmhaa.api.dto.RegisterRequest;
import com.example.tcmhaa.api.dto.RegisterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {
    @GET("/status")
    Call<ResponseBody> status();

    @POST("/api/users/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("/api/users/register")
    Call<RegisterResponse> register(@Body RegisterRequest body);
}

