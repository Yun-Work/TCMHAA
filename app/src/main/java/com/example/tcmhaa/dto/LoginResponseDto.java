package com.example.tcmhaa.dto;

import com.example.tcmhaa.api.dto.LoginResponse;
import com.example.tcmhaa.model.User;

public class LoginResponseDto {
    public boolean success;
    public String code;     // ← 新增：對齊後端
    public String message;
    public User user; // 成功時才有

    public static class User {
        public int user_id;
        public String name;
        public String email;
    }
}
