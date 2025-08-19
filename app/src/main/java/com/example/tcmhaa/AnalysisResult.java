package com.example.tcmhaa;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONObject;

/**
 * 分析結果數據類，實現Parcelable接口以便在Activity間傳遞
 */
public class AnalysisResult implements Parcelable {
    public boolean success;
    public String error;
    public String originalImage;
    public String annotatedImage;
    public String abnormalOnlyImage;
    public int abnormalCount;
    public String overallColorJson;  // 存儲為JSON字符串
    public String allRegionResultsJson;  // 存儲為JSON字符串
    public String regionResultsJson;  // 存儲為JSON字符串
    public String diagnosesJson;  // 存儲為JSON字符串
    public String diagnosisText;
    public String gridAnalysisJson;  // 存儲為JSON字符串

    public AnalysisResult() {}

    // 從ApiService.AnalysisResult轉換的構造函數
    public AnalysisResult(ApiService.AnalysisResult apiResult) {
        this.success = apiResult.success;
        this.error = apiResult.error;
        this.originalImage = apiResult.originalImage;
        this.annotatedImage = apiResult.annotatedImage;
        this.abnormalOnlyImage = apiResult.abnormalOnlyImage;
        this.abnormalCount = apiResult.abnormalCount;
        this.diagnosisText = apiResult.diagnosisText;

        // 將JSONObject轉換為字符串
        this.overallColorJson = apiResult.overallColor != null ? apiResult.overallColor.toString() : null;
        this.allRegionResultsJson = apiResult.allRegionResults != null ? apiResult.allRegionResults.toString() : null;
        this.regionResultsJson = apiResult.regionResults != null ? apiResult.regionResults.toString() : null;
        this.diagnosesJson = apiResult.diagnoses != null ? apiResult.diagnoses.toString() : null;
        this.gridAnalysisJson = apiResult.gridAnalysis != null ? apiResult.gridAnalysis.toString() : null;
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

    public JSONObject getDiagnoses() {
        try {
            return diagnosesJson != null ? new JSONObject(diagnosesJson) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getGridAnalysis() {
        try {
            return gridAnalysisJson != null ? new JSONObject(gridAnalysisJson) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Parcelable 實現
    protected AnalysisResult(Parcel in) {
        success = in.readByte() != 0;
        error = in.readString();
        originalImage = in.readString();
        annotatedImage = in.readString();
        abnormalOnlyImage = in.readString();
        abnormalCount = in.readInt();
        overallColorJson = in.readString();
        allRegionResultsJson = in.readString();
        regionResultsJson = in.readString();
        diagnosesJson = in.readString();
        diagnosisText = in.readString();
        gridAnalysisJson = in.readString();
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
        dest.writeString(originalImage);
        dest.writeString(annotatedImage);
        dest.writeString(abnormalOnlyImage);
        dest.writeInt(abnormalCount);
        dest.writeString(overallColorJson);
        dest.writeString(allRegionResultsJson);
        dest.writeString(regionResultsJson);
        dest.writeString(diagnosesJson);
        dest.writeString(diagnosisText);
        dest.writeString(gridAnalysisJson);
    }
}