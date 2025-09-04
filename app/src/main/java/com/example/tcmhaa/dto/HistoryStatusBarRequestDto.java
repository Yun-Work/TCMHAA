package com.example.tcmhaa.dto;

public class HistoryStatusBarRequestDto {
    public String organ;  // 器官中文名（要跟 sys_code.code_name 一致）
    public String start;  // YYYY-MM-DD
    public String end;    // YYYY-MM-DD

    public HistoryStatusBarRequestDto(String organ, String start, String end) {
        this.organ = organ;
        this.start = start;
        this.end   = end;
    }
}
