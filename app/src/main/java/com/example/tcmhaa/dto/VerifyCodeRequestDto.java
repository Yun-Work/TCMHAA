package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class VerifyCodeRequestDto {
    @SerializedName("user_id")
    private int userId;
    private String code;

    public VerifyCodeRequestDto(int userId, String code) {
        this.userId = userId;
        this.code = code;
    }
    public int getUser_id() { return userId; }
    public String getCode() { return code; }
}
