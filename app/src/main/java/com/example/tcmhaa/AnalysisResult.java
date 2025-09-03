package com.example.tcmhaa;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 分析結果數據類（增強版，支持痣和鬍鬚檢測）
 */
public class AnalysisResult implements Parcelable {
    public boolean success;
    public String error;
    public int abnormalCount;
    public String overallColorJson;
    public String allRegionResultsJson;
    public String regionResultsJson;
    public String diagnosisText;

    // 痣檢測相關欄位
    public boolean hasMoles;
    public boolean molesRemoved;
    public int moleCount;

    // 新增：鬍鬚檢測相關欄位
    public boolean hasBeard;
    public boolean beardRemoved;
    public int beardCount;

    public AnalysisResult() {}

    // 從ApiService.AnalysisResult轉換的構造函數
    public AnalysisResult(ApiService.AnalysisResult apiResult) {
        this.success = apiResult.isSuccess();
        this.error = apiResult.getError();
        this.abnormalCount = apiResult.getAbnormalCount();
        this.diagnosisText = apiResult.getDiagnosisText();

        // 處理整體膚色
        if (apiResult.getOverallColor() != null) {
            try {
                JSONObject colorJson = new JSONObject();
                colorJson.put("r", apiResult.getOverallColor().getR());
                colorJson.put("g", apiResult.getOverallColor().getG());
                colorJson.put("b", apiResult.getOverallColor().getB());
                colorJson.put("hex", apiResult.getOverallColor().getHex());
                this.overallColorJson = colorJson.toString();
            } catch (Exception e) {
                this.overallColorJson = null;
            }
        }

        // 處理區域結果
        if (apiResult.getAllRegionResults() != null) {
            try {
                JSONObject allRegionsJson = new JSONObject();
                for (Map.Entry<String, String> entry : apiResult.getAllRegionResults().entrySet()) {
                    allRegionsJson.put(entry.getKey(), entry.getValue());
                }
                this.allRegionResultsJson = allRegionsJson.toString();
            } catch (Exception e) {
                this.allRegionResultsJson = null;
            }
        }

        if (apiResult.getRegionResults() != null) {
            try {
                JSONObject regionJson = new JSONObject();
                for (Map.Entry<String, String> entry : apiResult.getRegionResults().entrySet()) {
                    regionJson.put(entry.getKey(), entry.getValue());
                }
                this.regionResultsJson = regionJson.toString();
            } catch (Exception e) {
                this.regionResultsJson = null;
            }
        }

        // 處理痣檢測數據
        this.hasMoles = apiResult.hasMoles();
        this.molesRemoved = apiResult.isMolesRemoved();
        this.moleCount = apiResult.getMoleCount();

        // 新增：處理鬍鬚檢測數據
        this.hasBeard = apiResult.hasBeard();
        this.beardRemoved = apiResult.isBeardRemoved();
        this.beardCount = apiResult.getBeardCount();
    }

    // 獲取JSONObject的便利方法
    public JSONObject getOverallColor() {
        try {
            return overallColorJson != null ? new JSONObject(overallColorJson) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getAllRegionResults() {
        try {
            return allRegionResultsJson != null ? new JSONObject(allRegionResultsJson) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getRegionResults() {
        try {
            return regionResultsJson != null ? new JSONObject(regionResultsJson) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 痣相關便利方法
    public boolean hasAnyMoles() {
        return hasMoles;
    }

    public int getMoleCount() {
        return moleCount;
    }

    public String getMoleDescription() {
        if (!hasMoles) {
            return "未檢測到明顯的痣";
        }
        return "";
    }

    // 新增：鬍鬚相關便利方法
    public boolean hasAnyBeard() {
        return hasBeard;
    }

    public int getBeardCount() {
        return beardCount;
    }

    public String getBeardDescription() {
        if (!hasBeard) {
            return "未檢測到明顯的鬍鬚";
        }
        return "";
    }

    // 新增：綜合特徵檢查方法
    public boolean hasAnyFeatures() {
        return hasMoles || hasBeard;
    }

    public String getFeaturesSummary() {
        if (!hasAnyFeatures()) {
            return "未檢測到明顯的面部特徵";
        }

        StringBuilder summary = new StringBuilder();
        if (hasMoles) {
            summary.append("");
        }
        if (hasBeard) {
            if (hasMoles) {
                summary.append("");
            }
            summary.append(getBeardDescription());
        }
        return summary.toString();
    }

    // 獲取處理狀態描述
    public String getProcessingStatus() {
        StringBuilder status = new StringBuilder();

        if (molesRemoved || beardRemoved) {
            status.append("已處理特徵：");
            if (molesRemoved) {
                status.append("痣");
            }
            if (beardRemoved) {
                if (molesRemoved) status.append("、");
                status.append("鬍鬚");
            }
        } else if (hasAnyFeatures()) {
            status.append("保留原始特徵");
        } else {
            status.append("無需特殊處理");
        }

        return status.toString();
    }

    // 獲取整體膚色的便利方法
    public String getOverallColorHex() {
        JSONObject color = getOverallColor();
        return color != null ? color.optString("hex", "#000000") : "#000000";
    }

    public int getOverallColorR() {
        JSONObject color = getOverallColor();
        return color != null ? color.optInt("r", 0) : 0;
    }

    public int getOverallColorG() {
        JSONObject color = getOverallColor();
        return color != null ? color.optInt("g", 0) : 0;
    }

    public int getOverallColorB() {
        JSONObject color = getOverallColor();
        return color != null ? color.optInt("b", 0) : 0;
    }

    // 獲取區域結果的便利方法
    public Map<String, String> getAllRegionResultsAsMap() {
        Map<String, String> results = new HashMap<>();
        JSONObject json = getAllRegionResults();
        if (json != null) {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                results.put(key, json.optString(key));
            }
        }
        return results;
    }

    public Map<String, String> getRegionResultsAsMap() {
        Map<String, String> results = new HashMap<>();
        JSONObject json = getRegionResults();
        if (json != null) {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                results.put(key, json.optString(key));
            }
        }
        return results;
    }

    // 檢查是否有異常區域
    public boolean hasAbnormalRegions() {
        return abnormalCount > 0;
    }

    // 獲取狀態摘要
    public String getStatusSummary() {
        if (!success) {
            return "分析失敗";
        }

        StringBuilder summary = new StringBuilder();

        if (abnormalCount == 0) {
            summary.append("所有區域膚色狀態正常");
        } else {
            summary.append("發現 ").append(abnormalCount).append(" 個異常區域");
        }

        // 添加特徵處理狀態
        if (hasAnyFeatures()) {
            summary.append("").append(getFeaturesSummary());
            if (molesRemoved || beardRemoved) {
                summary.append("");
            }
        }

        return summary.toString();
    }

    // Parcelable實現
    protected AnalysisResult(Parcel in) {
        success = in.readByte() != 0;
        error = in.readString();
        abnormalCount = in.readInt();
        overallColorJson = in.readString();
        allRegionResultsJson = in.readString();
        regionResultsJson = in.readString();
        diagnosisText = in.readString();

        // 痣檢測相關欄位
        hasMoles = in.readByte() != 0;
        molesRemoved = in.readByte() != 0;
        moleCount = in.readInt();

        // 新增：鬍鬚檢測相關欄位
        hasBeard = in.readByte() != 0;
        beardRemoved = in.readByte() != 0;
        beardCount = in.readInt();
    }

    public static final Creator<AnalysisResult> CREATOR = new Creator<AnalysisResult>() {
        @Override
        public AnalysisResult createFromParcel(Parcel in) {
            return new AnalysisResult(in);
        }

        @Override
        public AnalysisResult[] newArray(int size) {
            return new AnalysisResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (success ? 1 : 0));
        dest.writeString(error);
        dest.writeInt(abnormalCount);
        dest.writeString(overallColorJson);
        dest.writeString(allRegionResultsJson);
        dest.writeString(regionResultsJson);
        dest.writeString(diagnosisText);

        // 痣檢測相關欄位
        dest.writeByte((byte) (hasMoles ? 1 : 0));
        dest.writeByte((byte) (molesRemoved ? 1 : 0));
        dest.writeInt(moleCount);

        // 新增：鬍鬚檢測相關欄位
        dest.writeByte((byte) (hasBeard ? 1 : 0));
        dest.writeByte((byte) (beardRemoved ? 1 : 0));
        dest.writeInt(beardCount);
    }
}