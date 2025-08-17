package com.example.tcmhaa.dto;

public class RegisterRequestDto {
    private String email;
    private String password;

    // 後端若還要 name，再加一個欄位與建構子即可
    public RegisterRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
