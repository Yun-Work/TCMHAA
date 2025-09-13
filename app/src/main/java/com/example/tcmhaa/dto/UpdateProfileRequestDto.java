package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequestDto {
    @SerializedName("user_id")
    public int userId;

    // 只填有變更的欄位；沒改就保持為 null
    public String name;
    public String gender;         // "male" / "female"
    @SerializedName("birth_date")
    public String birthDate;      // "YYYY-MM-DD"

    public UpdateProfileRequestDto(int userId) {
        this.userId = userId;
    }

    public UpdateProfileRequestDto(int userId, String name, String gender, String birthDate) {
        this.userId   = userId;
        this.name     = isBlank(name) ? null : name.trim();
        this.gender   = isBlank(gender) ? null : gender.trim();
        this.birthDate= isBlank(birthDate) ? null : birthDate.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
