package com.example.tcmhaa.dto;
import com.google.gson.annotations.SerializedName;
public class RegisterResponseDto {
    // 常見兩種後端回傳都相容：success/message 或 error/user_id
    private boolean success;                 //  成功訊息（成功時有）
    @SerializedName("user_id")
    private Integer userId;                  // 新使用者 id（成功時有）
    private String error;                    // 失敗訊息（失敗時有）

    public boolean isSuccess() { return success; }
    public Integer getUserId() { return userId; }
    public String getError() { return error; }
}
