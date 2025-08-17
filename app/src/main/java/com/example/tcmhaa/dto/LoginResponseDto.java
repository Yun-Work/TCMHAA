package com.example.tcmhaa.dto;

import com.example.tcmhaa.model.User;

public class LoginResponseDto {
    private boolean success;
    private String message;
    private User user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}
