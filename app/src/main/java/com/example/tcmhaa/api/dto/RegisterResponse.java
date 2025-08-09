package com.example.tcmhaa.api.dto;

public class RegisterResponse {
    public String success;  // 成功訊息（成功時有）
    public Integer user_id; // 新使用者 id（成功時有）
    public String error;    // 失敗訊息（失敗時有）
}
