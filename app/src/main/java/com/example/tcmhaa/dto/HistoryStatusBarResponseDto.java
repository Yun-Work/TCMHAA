package com.example.tcmhaa.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HistoryStatusBarResponseDto {
    public String organ;
    public String start;     // YYYY-MM-DD
    public String end;       // YYYY-MM-DD
    public List<String> x;   // 日期軸

    public Series series;    // 顏色 → 0/1 陣列
    public List<String> dominant;     // 每天："發紅"|"發黑"|...|"正常"|"無資料"
    public List<Integer> no_data;     // 每天：1=沒資料，0=有資料
    @SerializedName("locations_detected")
    public List<String> locationsDetected;
    public String error;

    public boolean hasError() { return error != null && !error.isEmpty(); }

    public static class Series {
        @SerializedName("發紅") public List<Integer> red;
        @SerializedName("發黑") public List<Integer> black;
        @SerializedName("發黃") public List<Integer> yellow;
        @SerializedName("發白") public List<Integer> white;
        @SerializedName("發青") public List<Integer> cyan;
    }
}
