package com.example.tcmhaa.dto;

public class ChangePasswordRequestDto {
    public int user_id;
    public String new_password;

    public ChangePasswordRequestDto(int user_id, String new_password) {
        this.user_id = user_id;
        this.new_password = new_password;
    }
}
