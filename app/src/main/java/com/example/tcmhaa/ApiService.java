package com.example.tcmhaa;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final String TAG = "ApiService";

    private static final String BASE_URL = "http://10.0.2.2:6060";
    private static final String ANALYZE_ENDPOINT = "/api/face/upload";
    private static final String HEALTH_ENDPOINT = "/api/face/health";

    private OkHttpClient client;

    public ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        testConnectionOnInit();
    }

    private void testConnectionOnInit() {
        Log.d(TAG, "測試API連接: " + BASE_URL + HEALTH_ENDPOINT);

        Request request = new Request.Builder()
                .url(BASE_URL + HEALTH_ENDPOINT)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API連接測試失敗: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "API連接測試成功! 狀態碼: " + response.code());
                } else {
                    Log.w(TAG, "API連接測試響應異常: " + response.code());
                }
            }
        });
    }

    public interface AnalysisCallback {
        void onSuccess(AnalysisResult result);
        void onFailure(String error);
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }

    public static class OverallColor {
        private int r, g, b;
        private String hex;

        public OverallColor() {}

        public OverallColor(int r, int g, int b, String hex) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.hex = hex;
        }

        public int getR() { return r; }
        public int getG() { return g; }
        public int getB() { return b; }
        public String getHex() { return hex; }
        public void setR(int r) { this.r = r; }
        public void setG(int g) { this.g = g; }
        public void setB(int b) { this.b = b; }
        public void setHex(String hex) { this.hex = hex; }
    }

    public static class AnalysisResult {
        private boolean success;
        private String error;
        private int abnormalCount;
        private OverallColor overallColor;
        private Map<String, String> allRegionResults;
        private Map<String, String> regionResults;
        private String diagnosisText;

        // 痣檢測相關欄位（簡化版）
        private boolean hasMoles;
        private boolean molesRemoved;
        private int moleCount;

        public AnalysisResult() {
            allRegionResults = new HashMap<>();
            regionResults = new HashMap<>();
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public int getAbnormalCount() { return abnormalCount; }
        public OverallColor getOverallColor() { return overallColor; }
        public Map<String, String> getAllRegionResults() { return allRegionResults; }
        public Map<String, String> getRegionResults() { return regionResults; }
        public String getDiagnosisText() { return diagnosisText; }
        public boolean hasMoles() { return hasMoles; }
        public boolean isMolesRemoved() { return molesRemoved; }
        public int getMoleCount() { return moleCount; }

        // Setters
        public void setSuccess(boolean success) { this.success = success; }
        public void setError(String error) { this.error = error; }
        public void setAbnormalCount(int abnormalCount) { this.abnormalCount = abnormalCount; }
        public void setOverallColor(OverallColor overallColor) { this.overallColor = overallColor; }
        public void setAllRegionResults(Map<String, String> allRegionResults) { this.allRegionResults = allRegionResults; }
        public void setRegionResults(Map<String, String> regionResults) { this.regionResults = regionResults; }
        public void setDiagnosisText(String diagnosisText) { this.diagnosisText = diagnosisText; }
        public void setHasMoles(boolean hasMoles) { this.hasMoles = hasMoles; }
        public void setMolesRemoved(boolean molesRemoved) { this.molesRemoved = molesRemoved; }
        public void setMoleCount(int moleCount) { this.moleCount = moleCount; }

        // 便利方法
        public String getMoleDescription() {
            if (!hasMoles) {
                return "未檢測到明顯的痣";
            }
            return "檢測到 " + moleCount + " 個痣";
        }

        public static AnalysisResult fromJson(JSONObject json) {
            AnalysisResult result = new AnalysisResult();
            try {
                result.success = json.optBoolean("success", false);
                result.error = json.optString("error", null);
                result.abnormalCount = json.optInt("abnormal_count", 0);
                result.diagnosisText = json.optString("diagnosis_text", "");

                // 整體膚色
                JSONObject overallColorJson = json.optJSONObject("overall_color");
                if (overallColorJson != null) {
                    result.overallColor = new OverallColor(
                            overallColorJson.optInt("r", 0),
                            overallColorJson.optInt("g", 0),
                            overallColorJson.optInt("b", 0),
                            overallColorJson.optString("hex", "#000000")
                    );
                }

                // 區域結果
                JSONObject allRegionResultsJson = json.optJSONObject("all_region_results");
                if (allRegionResultsJson != null) {
                    Iterator<String> keys = allRegionResultsJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        result.allRegionResults.put(key, allRegionResultsJson.optString(key));
                    }
                }

                JSONObject regionResultsJson = json.optJSONObject("region_results");
                if (regionResultsJson != null) {
                    Iterator<String> keys = regionResultsJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        result.regionResults.put(key, regionResultsJson.optString(key));
                    }
                }

                // 痣檢測分析
                result.hasMoles = json.optBoolean("has_moles", false);
                result.molesRemoved = json.optBoolean("moles_removed", false);

                JSONObject moleAnalysisJson = json.optJSONObject("mole_analysis");
                if (moleAnalysisJson != null) {
                    result.moleCount = moleAnalysisJson.optInt("mole_count", 0);
                } else {
                    result.moleCount = 0;
                }

            } catch (Exception e) {
                Log.e(TAG, "解析JSON結果時發生錯誤", e);
                result.success = false;
                result.error = "解析結果失敗: " + e.getMessage();
            }
            return result;
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        try {
            // 確保圖片不會太大
            int maxSize = 800;
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float scale = Math.min((float) maxSize / bitmap.getWidth(),
                        (float) maxSize / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            if (byteArray.length == 0) {
                throw new RuntimeException("壓縮後圖片數據為空");
            }

            String base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            Log.d(TAG, "圖片轉換完成，Base64長度: " + base64String.length());

            return "data:image/jpeg;base64," + base64String;

        } catch (Exception e) {
            Log.e(TAG, "Bitmap轉Base64失敗", e);
            throw new RuntimeException("圖片轉換失敗: " + e.getMessage());
        }
    }

    /**
     * 分析面部圖片（包含痣檢測功能）
     */
    public void analyzeFaceWithMoleDetection(Bitmap bitmap, boolean removeMoles, AnalysisCallback callback) {
        String base64 = bitmapToBase64(bitmap);
        analyzeFaceWithBase64(base64, removeMoles, callback);
    }

    /**
     * 使用base64分析面部（包含痣檢測功能）
     */
    public void analyzeFaceWithBase64(String base64Image, boolean removeMoles, AnalysisCallback callback) {
        Log.d(TAG, "開始面部分析，移除痣: " + removeMoles);

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("image", base64Image);
            requestJson.put("remove_moles", removeMoles);

            RequestBody requestBody = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + ANALYZE_ENDPOINT)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "發送分析請求到: " + BASE_URL + ANALYZE_ENDPOINT);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "網絡請求失敗", e);
                    String errorMessage = "網絡連接失敗: " + e.getMessage();

                    if (e.getMessage().contains("Failed to connect")) {
                        errorMessage += "\n\n請檢查：\n• 後端服務是否在6060端口運行？\n• IP地址是否正確？";
                    } else if (e.getMessage().contains("timeout")) {
                        errorMessage += "\n\n連接超時，請檢查網絡狀況";
                    }

                    callback.onFailure(errorMessage);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API響應狀態碼: " + response.code());

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            AnalysisResult result = AnalysisResult.fromJson(jsonResponse);

                            if (result.success) {
                                Log.d(TAG, "分析成功！異常區域數量: " + result.abnormalCount +
                                        ", 檢測到痣: " + result.hasMoles);
                                callback.onSuccess(result);
                            } else {
                                Log.w(TAG, "分析失敗: " + result.error);
                                callback.onFailure(result.error != null ? result.error : "分析失敗");
                            }
                        } else {
                            Log.e(TAG, "HTTP錯誤: " + response.code());
                            if (response.code() == 404) {
                                callback.onFailure("API端點不存在 (404)");
                            } else {
                                callback.onFailure("服務器錯誤 (" + response.code() + ")");
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析響應JSON失敗", e);
                        callback.onFailure("解析服務器響應失敗: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "處理響應時發生錯誤", e);
                        callback.onFailure("處理結果時發生錯誤: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "創建請求JSON失敗", e);
            callback.onFailure("創建請求失敗: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "發送請求時發生錯誤", e);
            callback.onFailure("發送請求失敗: " + e.getMessage());
        }
    }

    /**
     * 分析面部圖片（向後兼容的方法）
     */
    public void analyzeFace(Bitmap bitmap, AnalysisCallback callback) {
        analyzeFaceWithMoleDetection(bitmap, false, callback);
    }

    public void analyzeFace(Bitmap bitmap, boolean includeImages, AnalysisCallback callback) {
        analyzeFaceWithMoleDetection(bitmap, false, callback);
    }

    public void testConnection(TestCallback callback) {
        Log.d(TAG, "手動測試API連接...");

        Request request = new Request.Builder()
                .url(BASE_URL + HEALTH_ENDPOINT)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "連接測試失敗", e);
                callback.onTestResult(false, "連接失敗: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                String message = success ?
                        "連接成功！狀態碼: " + response.code() :
                        "連接異常，狀態碼: " + response.code();

                callback.onTestResult(success, message);
            }
        });
    }
}