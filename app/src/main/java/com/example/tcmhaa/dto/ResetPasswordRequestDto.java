package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequestDto {
    @SerializedName("user_id")
    private final int userId;
    @SerializedName("new_password")
    private final String newPassword;

    public ResetPasswordRequestDto(int userId, String newPassword) {
        this.userId = userId;
        this.newPassword = newPassword;
    }
}