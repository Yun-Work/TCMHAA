package com.example.tcmhaa.api.dto;
public class RegisterRequest {
    public String email;
    public String password;
    public RegisterRequest(String name, String email, String password) {
        this.email = email; this.password = password;
    }
}