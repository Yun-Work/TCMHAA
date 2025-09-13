package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class GetProfileRequestDto {
    @SerializedName("user_id")
    public int userId;

    public GetProfileRequestDto(int userId) {
        this.userId = userId;
    }
}
