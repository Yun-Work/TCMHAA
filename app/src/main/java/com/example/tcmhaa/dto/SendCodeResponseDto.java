package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class SendCodeResponseDto {
    private String message; // 成功時
    private String error;   // 失敗時

    @SerializedName("user_id") //  user_id
    private Integer userId;

    public String getMessage() { return message; }
    public String getError() { return error; }
    public Integer getUserId() { return userId; }
}
