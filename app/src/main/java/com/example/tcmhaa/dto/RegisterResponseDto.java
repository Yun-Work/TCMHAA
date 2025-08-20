package com.example.tcmhaa.dto;
import com.google.gson.annotations.SerializedName;
public class RegisterResponseDto {
    public boolean success;        // true / false
    public String  message;        // "註冊成功" 或 "Email 已被註冊"
    @SerializedName("user_id")
    public Integer user_id;        // 成功時才會有
    public String  code;           // 可選：若後端有設定 "EMAIL_TAKEN" 之類
}
