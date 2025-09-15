package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class VerifyCodeRequestDto {
    @SerializedName("user_id")
    private Integer userId;
    private String email;
    private String code;
    private final String status; // "1" 註冊, "2" 忘記
    // 註冊驗證：email + code（status 自動帶 "1"）
    public VerifyCodeRequestDto(String email, String code) {
        this.email = email;
        this.code = code;
        this.status = "1";
    }

    // 忘記密碼驗證：user_id + code（status 自動帶 "2"）
    public VerifyCodeRequestDto(int userId, String code) {
        this.userId = userId;
        this.code = code;
        this.status = "2";
    }

    // 可選：getter（若需要）
    public Integer getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getCode() { return code; }
    public String getStatus() { return status; }
}
