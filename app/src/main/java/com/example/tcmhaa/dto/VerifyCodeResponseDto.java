package com.example.tcmhaa.dto;

public class VerifyCodeResponseDto {
    private String message; // 成功時
    private String error;   // 失敗時

    public String getMessage() { return message; }
    public String getError() { return error; }
}
