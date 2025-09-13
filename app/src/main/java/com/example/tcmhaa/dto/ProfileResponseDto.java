package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class ProfileResponseDto {
    @SerializedName("user_id")
    public int userId;

    public String email;          // "tt123@gmail.com"
    public String name;           // "tt123"
    public String gender;         // "male" / "female"
    @SerializedName("birth_date")
    public String birthDate;      // "YYYY-MM-DD"
}
