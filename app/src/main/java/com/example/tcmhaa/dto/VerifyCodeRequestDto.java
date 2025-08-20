package com.example.tcmhaa.dto;

public class VerifyCodeRequestDto {

    private String email;
    private String code;

    public VerifyCodeRequestDto(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
