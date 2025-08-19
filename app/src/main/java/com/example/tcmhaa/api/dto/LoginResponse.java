package com.example.tcmhaa.api.dto;


public class LoginResponse {
    public boolean success;
    public String message;
    public User user; // 成功時才有

    public static class User {
        public int user_id;
        public String name;
        public String email;
    }
}