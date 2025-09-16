package com.example.tcmhaa;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiService {
    private static final String TAG = "ApiService";

    private static final String BASE_URL = "https://tcmha-python.duckdns.org/";
    //private static final String BASE_URL = "http://163.13.202.117：6060";
//    private static final String BASE_URL = "http://10.0.2.2:6060";
    private static final String ANALYZE_ENDPOINT = "/api/face/upload";
    private static final String HEALTH_ENDPOINT = "/api/face/health";

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2秒
    private static final long PROGRESSIVE_DELAY_MULTIPLIER = 2;
    private static final int MAX_RESPONSE_SIZE = 50 * 1024 * 1024; // 50MB

    private OkHttpClient client;
    private Integer userId;          // ← 新增

    public void setUserId(int userId) {  // ← 新增
        this.userId = userId;
    }

    public ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)   // 3分鐘寫入超時
                .readTimeout(180, TimeUnit.SECONDS)    // 3分鐘讀取超時
                .callTimeout(300, TimeUnit.SECONDS)    // 5分鐘總超時
                .retryOnConnectionFailure(true)
                .addNetworkInterceptor(new ResponseBufferingInterceptor())
                .build();

        testConnectionOnInit();
    }

    // 響應緩衝攔截器
    private static class ResponseBufferingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            if (response.body() != null) {
                ResponseBody originalBody = response.body();
                try {
                    // 預先讀取所有數據到內存中
                    byte[] bodyBytes = originalBody.bytes();
                    ResponseBody newBody = ResponseBody.create(bodyBytes, originalBody.contentType());

                    return response.newBuilder()
                            .body(newBody)
                            .build();
                } catch (OutOfMemoryError e) {
                    Log.w("ApiService", "響應數據過大，跳過緩衝");
                    return response;
                }
            }

            return response;
        }
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

        // 痣檢測相關欄位
        private boolean hasMoles;
        private boolean molesRemoved;
        private int moleCount;

        // 鬍鬚檢測相關欄位
        private boolean hasBeard;
        private boolean beardRemoved;
        private int beardCount;

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
        public boolean hasBeard() { return hasBeard; }
        public boolean isBeardRemoved() { return beardRemoved; }
        public int getBeardCount() { return beardCount; }

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
        public void setHasBeard(boolean hasBeard) { this.hasBeard = hasBeard; }
        public void setBeardRemoved(boolean beardRemoved) { this.beardRemoved = beardRemoved; }
        public void setBeardCount(int beardCount) { this.beardCount = beardCount; }

        // 便利方法
        public String getMoleDescription() {
            if (!hasMoles) {
                return "未檢測到明顯的痣";
            }
            return "檢測到 " + moleCount + " 個痣";
        }

        public String getBeardDescription() {
            if (!hasBeard) {
                return "未檢測到明顯的鬍鬚";
            }
            return "檢測到 " + beardCount + " 處鬍鬚";
        }

        public String getFeatureDescription() {
            StringBuilder desc = new StringBuilder();

            if (hasMoles || hasBeard) {
                desc.append("檢測到面部特徵：");
                if (hasMoles) {
                    desc.append(getMoleDescription());
                }
                if (hasBeard) {
                    if (hasMoles) desc.append("；");
                    desc.append(getBeardDescription());
                }
            } else {
                desc.append("未檢測到明顯的面部特徵");
            }

            return desc.toString();
        }

        // 在你的 ApiService.java 中，替換 AnalysisResult 類的 fromJson 方法

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

                // 🔧 修復：安全的痣檢測分析
                result.hasMoles = json.optBoolean("has_moles", false);
                result.molesRemoved = json.optBoolean("moles_removed", false);

                JSONObject moleAnalysisJson = json.optJSONObject("mole_analysis");
                if (moleAnalysisJson != null) {
                    // 🚨 關鍵修復：安全地處理可能為 null 的 mole_count
                    Object moleCountObj = moleAnalysisJson.opt("mole_count");
                    if (moleCountObj instanceof Number) {
                        result.moleCount = ((Number) moleCountObj).intValue();
                    } else if (moleCountObj instanceof String) {
                        try {
                            result.moleCount = Integer.parseInt((String) moleCountObj);
                        } catch (NumberFormatException e) {
                            Log.w("ApiService", "無法解析 mole_count: " + moleCountObj);
                            result.moleCount = 0;
                        }
                    } else {
                        result.moleCount = 0;
                    }
                } else {
                    result.moleCount = 0;
                }

                // 🔧 修復：安全的鬍鬚檢測分析
                result.hasBeard = json.optBoolean("has_beard", false);
                result.beardRemoved = json.optBoolean("beard_removed", false);

                JSONObject beardAnalysisJson = json.optJSONObject("beard_analysis");
                if (beardAnalysisJson != null && !beardAnalysisJson.toString().equals("null")) {
                    // 🚨 關鍵修復：安全地處理可能為 null 的 beard_count
                    Object beardCountObj = beardAnalysisJson.opt("beard_count");
                    if (beardCountObj instanceof Number) {
                        result.beardCount = ((Number) beardCountObj).intValue();
                    } else if (beardCountObj instanceof String) {
                        try {
                            result.beardCount = Integer.parseInt((String) beardCountObj);
                        } catch (NumberFormatException e) {
                            Log.w("ApiService", "無法解析 beard_count: " + beardCountObj);
                            result.beardCount = 0;
                        }
                    } else {
                        result.beardCount = 0;
                    }

                    // 檢查 beard_analysis 中是否有 has_beard 欄位
                    if (beardAnalysisJson.has("has_beard")) {
                        result.hasBeard = beardAnalysisJson.optBoolean("has_beard", false);
                    }
                } else {
                    result.beardCount = 0;
                }

                // 🔧 額外檢查：處理可能在頂層的鬍鬚資訊
                JSONObject beardInfoJson = json.optJSONObject("beard_info");
                if (beardInfoJson != null) {
                    result.hasBeard = beardInfoJson.optBoolean("has_beard", result.hasBeard);

                    Object topBeardCountObj = beardInfoJson.opt("beard_count");
                    if (topBeardCountObj instanceof Number) {
                        result.beardCount = ((Number) topBeardCountObj).intValue();
                    } else if (topBeardCountObj instanceof String) {
                        try {
                            result.beardCount = Integer.parseInt((String) topBeardCountObj);
                        } catch (NumberFormatException e) {
                            Log.w("ApiService", "無法解析頂層 beard_count: " + topBeardCountObj);
                        }
                    }
                }

                // 🔧 處理頂層的鬍鬚欄位（如果存在）
                if (json.has("has_beard")) {
                    result.hasBeard = json.optBoolean("has_beard", false);
                }
                if (json.has("beard_count")) {
                    Object topLevelBeardCount = json.opt("beard_count");
                    if (topLevelBeardCount instanceof Number) {
                        result.beardCount = ((Number) topLevelBeardCount).intValue();
                    } else if (topLevelBeardCount instanceof String) {
                        try {
                            result.beardCount = Integer.parseInt((String) topLevelBeardCount);
                        } catch (NumberFormatException e) {
                            Log.w("ApiService", "無法解析最頂層 beard_count: " + topLevelBeardCount);
                        }
                    }
                }

                Log.d("ApiService", "JSON解析結果:");
                Log.d("ApiService", "  has_moles: " + result.hasMoles);
                Log.d("ApiService", "  has_beard: " + result.hasBeard);
                Log.d("ApiService", "  mole_count: " + result.moleCount);
                Log.d("ApiService", "  beard_count: " + result.beardCount);
                Log.d("ApiService", "  success: " + result.success);

            } catch (Exception e) {
                Log.e("ApiService", "解析JSON結果時發生錯誤", e);
                result.success = false;
                result.error = "解析結果失敗: " + e.getMessage();

                // 設置安全的預設值
                result.abnormalCount = 0;
                result.diagnosisText = "";
                result.hasMoles = false;
                result.molesRemoved = false;
                result.moleCount = 0;
                result.hasBeard = false;
                result.beardRemoved = false;
                result.beardCount = 0;

                if (result.allRegionResults == null) {
                    result.allRegionResults = new HashMap<>();
                }
                if (result.regionResults == null) {
                    result.regionResults = new HashMap<>();
                }
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

    // 主要的分析方法 - 智能重試版本
    public void analyzeFaceWithSmartRetry(Bitmap bitmap, boolean removeMoles, boolean removeBeard, AnalysisCallback callback) {
        analyzeFaceWithSmartRetry(bitmap, removeMoles, removeBeard, callback, 0, System.currentTimeMillis());
    }

    private void analyzeFaceWithSmartRetry(Bitmap bitmap, boolean removeMoles, boolean removeBeard,
                                           AnalysisCallback callback, int attemptCount, long startTime) {

        Log.d(TAG, "開始智能重試分析 - 第" + (attemptCount + 1) + "次嘗試");

        analyzeFaceWithFeatureRemoval(bitmap, removeMoles, removeBeard,userId,new AnalysisCallback() {
            @Override
            public void onSuccess(AnalysisResult result) {
                long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "分析成功！總耗時: " + totalTime + "ms, 嘗試次數: " + (attemptCount + 1));
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "第" + (attemptCount + 1) + "次嘗試失敗: " + error);

                boolean shouldRetry = shouldRetryOnError(error, attemptCount);

                if (shouldRetry && attemptCount < MAX_RETRY_ATTEMPTS - 1) {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(PROGRESSIVE_DELAY_MULTIPLIER, attemptCount);

                    Log.d(TAG, "將在" + delay + "ms後進行第" + (attemptCount + 2) + "次重試");

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        analyzeFaceWithSmartRetry(bitmap, removeMoles, removeBeard, callback,
                                attemptCount + 1, startTime);
                    }, delay);
                } else {
                    String finalError = buildFinalErrorMessage(error, attemptCount + 1);
                    Log.e(TAG, "所有重試失敗: " + finalError);
                    callback.onFailure(finalError);
                }
            }
        });
    }

    /**
     * 分析面部圖片（包含痣和鬍鬚檢測功能）
     */
    public void analyzeFaceWithFeatureRemoval(String base64Image, boolean removeMoles, boolean removeBeard,int userId, AnalysisCallback callback) {
        Log.d(TAG, "開始面部分析，移除痣: " + removeMoles + ", 移除鬍鬚: " + removeBeard);

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("image", base64Image);
            requestJson.put("remove_moles", removeMoles);
            requestJson.put("remove_beard", removeBeard);
            requestJson.put("user_id", userId);

            RequestBody requestBody = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + ANALYZE_ENDPOINT)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
//                    .addHeader("X-User-Id", String.valueOf(userId))   //把 user_id 放在 header
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
                    handleResponse(response, callback);
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

    private void handleResponse(Response response, AnalysisCallback callback) throws IOException {
        String responseBody = null;
        boolean isPartialResponse = false;

        try {
            ResponseBody body = response.body();
            if (body == null) {
                callback.onFailure("服務器響應為空");
                return;
            }

            // 嘗試讀取響應，支持部分響應恢復
            try {
                responseBody = readResponseWithFallback(body);
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("unexpected end of stream")) {
                    Log.w(TAG, "檢測到流截斷，嘗試使用部分數據");
                    responseBody = getPartialResponseIfAvailable(body);
                    isPartialResponse = true;
                }
                if (responseBody == null) {
                    throw e;
                }
            }

            Log.d(TAG, "響應狀態: " + response.code() +
                    ", 長度: " + (responseBody != null ? responseBody.length() : 0) +
                    (isPartialResponse ? " (部分數據)" : ""));

            if (response.isSuccessful()) {
                JSONObject jsonResponse = parseJsonWithFallback(responseBody);

                if (jsonResponse == null) {
                    callback.onFailure("無法解析服務器響應" + (isPartialResponse ? "（數據不完整）" : ""));
                    return;
                }

                AnalysisResult result = AnalysisResult.fromJson(jsonResponse);

                if (result.success) {
                    if (isPartialResponse) {
                        Log.w(TAG, "使用部分響應數據，結果可能不完整");
                    }

                    Log.d(TAG, "分析成功 - 異常區域: " + result.abnormalCount +
                            ", 痣: " + result.hasMoles + ", 鬍鬚: " + result.hasBeard +
                            (isPartialResponse ? " (使用部分數據)" : ""));

                    callback.onSuccess(result);
                } else {
                    callback.onFailure(result.error != null ? result.error : "分析失敗");
                }
            } else {
                handleHttpError(response.code(), callback);
            }

        } catch (OutOfMemoryError e) {
            Log.e(TAG, "內存不足，響應數據過大", e);
            callback.onFailure("響應數據過大，請稍後重試");
        } catch (IOException e) {
            Log.e(TAG, "IO異常", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("unexpected end of stream")) {
                callback.onFailure("網絡傳輸中斷，請檢查網絡連接後重試");
            } else {
                callback.onFailure("網絡錯誤: " + errorMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "處理響應時發生錯誤", e);
            callback.onFailure("處理結果時發生錯誤: " + e.getMessage());
        }
    }

    // 安全讀取響應體
    private String readResponseWithFallback(ResponseBody body) throws IOException {
        try (InputStream inputStream = body.byteStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_RESPONSE_SIZE) {
                    Log.e(TAG, "響應數據超過最大允許大小");
                    throw new IOException("響應數據過大");
                }
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toString("UTF-8");

        } catch (IOException e) {
            Log.e(TAG, "讀取響應體失敗", e);
            throw e;
        }
    }

    // 嘗試獲取部分響應數據
    private String getPartialResponseIfAvailable(ResponseBody body) {
        try {
            // 這是一個簡化的實現，實際中可能需要更複雜的邏輯
            String content = body.string();
            if (content != null && content.length() > 0) {
                return content;
            }
        } catch (Exception e) {
            Log.w(TAG, "無法獲取部分響應數據", e);
        }
        return null;
    }

    // 更寬容的JSON解析
    private JSONObject parseJsonWithFallback(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.w(TAG, "標準JSON解析失敗，嘗試修復", e);

            // 嘗試修復常見的JSON問題
            String fixedJson = jsonString.trim();

            // 如果JSON被截斷，嘗試找到最後一個完整的對象
            if (!fixedJson.endsWith("}")) {
                int lastBrace = fixedJson.lastIndexOf("}");
                if (lastBrace > 0) {
                    fixedJson = fixedJson.substring(0, lastBrace + 1);
                    Log.d(TAG, "嘗試使用截斷修復的JSON");
                }
            }

            try {
                return new JSONObject(fixedJson);
            } catch (JSONException e2) {
                Log.e(TAG, "JSON修復也失敗", e2);
                return null;
            }
        }
    }

    // HTTP錯誤處理
    private void handleHttpError(int code, AnalysisCallback callback) {
        String errorMessage;
        switch (code) {
            case 404:
                errorMessage = "API端點不存在 (404)";
                break;
            case 413:
                errorMessage = "請求數據過大 (413)，請嘗試使用較小的圖片";
                break;
            case 500:
                errorMessage = "服務器內部錯誤 (500)，請稍後重試";
                break;
            case 502:
                errorMessage = "服務器網關錯誤 (502)";
                break;
            case 503:
                errorMessage = "服務暫時不可用 (503)，請稍後重試";
                break;
            case 504:
                errorMessage = "服務器處理超時 (504)，分析可能需要更多時間";
                break;
            default:
                errorMessage = "服務器錯誤 (" + code + ")";
        }

        Log.e(TAG, "HTTP錯誤: " + errorMessage);
        callback.onFailure(errorMessage);
    }

    // 判斷是否應該重試
    private boolean shouldRetryOnError(String error, int attemptCount) {
        if (attemptCount >= MAX_RETRY_ATTEMPTS - 1) {
            return false;
        }

        if (error == null) {
            return true;
        }

        String lowerError = error.toLowerCase();
        return lowerError.contains("unexpected end of stream") ||
                lowerError.contains("網絡傳輸中斷") ||
                lowerError.contains("網絡連接失敗") ||
                lowerError.contains("timeout") ||
                lowerError.contains("超時") ||
                lowerError.contains("connection reset") ||
                lowerError.contains("socket") ||
                lowerError.contains("504") ||
                lowerError.contains("502") ||
                lowerError.contains("503");
    }

    // 構建最終錯誤消息
    private String buildFinalErrorMessage(String lastError, int totalAttempts) {
        StringBuilder sb = new StringBuilder();
        sb.append("經過").append(totalAttempts).append("次嘗試後仍然失敗\n\n");
        sb.append("最後錯誤: ").append(lastError).append("\n\n");
        sb.append("可能的解決方案:\n");
        sb.append("• 檢查網絡連接是否穩定\n");
        sb.append("• 確認後端服務是否正常運行\n");
        sb.append("• 嘗試使用較小的圖片\n");
        sb.append("• 稍後再試");

        return sb.toString();
    }

    /**
     * Bitmap版本的分析方法
     */
    public void analyzeFaceWithFeatureRemoval(Bitmap bitmap, boolean removeMoles, boolean removeBeard, int userId,AnalysisCallback callback) {
        String base64 = bitmapToBase64(bitmap);
        analyzeFaceWithFeatureRemoval(base64, removeMoles, removeBeard, userId, callback);
    }

    /**
     * 向後兼容的方法
     */
    public void analyzeFaceWithMoleDetection(Bitmap bitmap, boolean removeMoles, AnalysisCallback callback) {
        analyzeFaceWithSmartRetry(bitmap, removeMoles, false, callback);
    }

    public void analyzeFaceWithBase64(String base64Image, boolean removeMoles, int userId,AnalysisCallback callback) {
        analyzeFaceWithFeatureRemoval(base64Image, removeMoles, false,  userId,callback);
    }

    public void analyzeFace(Bitmap bitmap, AnalysisCallback callback) {
        analyzeFaceWithSmartRetry(bitmap, false, false, callback);
    }

    public void analyzeFace(Bitmap bitmap, boolean includeImages, AnalysisCallback callback) {
        analyzeFaceWithSmartRetry(bitmap, false, false, callback);
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