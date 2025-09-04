package com.example.tcmhaa.dto;

import java.util.List;

public class HistoryStatusBarResponseDto {
    public List<String> categories; // 例：["發紅","發黑","發黃","發白","發青"]
    public List<Integer> data;      // 對應各類次數
    public String error;            // 後端錯誤訊息（有時會回傳）

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
}
