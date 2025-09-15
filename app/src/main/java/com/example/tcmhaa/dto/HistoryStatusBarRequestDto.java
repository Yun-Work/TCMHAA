package com.example.tcmhaa.dto;

public class HistoryStatusBarRequestDto {
    public String organ;  // 器官中文名（要跟 sys_code.code_name 一致）
    public String start;  // YYYY-MM-DD
    public String end;    // YYYY-MM-DD
    public Integer user_id;   // ← 新增

    public HistoryStatusBarRequestDto(String organ, String start, String end,Integer user_id) {
        this.organ = organ;
        this.start = start;
        this.end   = end;
        this.user_id = user_id;
    }
}
