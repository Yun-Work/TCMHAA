package com.example.tcmhaa.dto;


public class SendCodeRequestDto {
    private String email;
    private String status; // "1" 註冊、"2" 忘記密碼

    public SendCodeRequestDto(String email, String status) {
        this.email = email;
        this.status = status;
    }

    public String getEmail() { return email; }
    public String getStatus() { return status; }
}
