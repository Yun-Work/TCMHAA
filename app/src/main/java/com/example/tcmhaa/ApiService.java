package com.example.tcmhaa;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    // 🔧 修正：使用6060端口，並且API路徑是 /api/face
    private static final String BASE_URL = "http://10.0.2.2:6060"; // Android模擬器訪問主機
    // 如果是實體設備，請改為: "http://YOUR_COMPUTER_IP:6060"
    // 例如: "http://192.168.1.100:6060"

    // 根據你的後端API調整端點
    private static final String ANALYZE_ENDPOINT = "/api/face/upload";
    private static final String HEALTH_ENDPOINT = "/api/face/health";

    private OkHttpClient client;

    public ApiService() {
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)   // NEW: 關閉連線自動重試
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 啟動時測試連接
        testConnectionOnInit();
    }

    /**
     * 初始化時測試連接
     */
    private void testConnectionOnInit() {
        Log.d(TAG, "🔍 測試API連接: " + BASE_URL + HEALTH_ENDPOINT);

        Request request = new Request.Builder()
                .url(BASE_URL + HEALTH_ENDPOINT)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ API連接測試失敗: " + e.getMessage());
                Log.e(TAG, "📍 請檢查:");
                Log.e(TAG, "   1. 後端服務是否在6060端口運行?");
                Log.e(TAG, "   2. IP地址是否正確? 當前: " + BASE_URL);
                Log.e(TAG, "   3. 防火牆是否阻擋了6060端口?");
                Log.e(TAG, "   4. API端點路徑是否正確?");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ API連接測試成功! 狀態碼: " + response.code());
                    String body = response.body().string();
                    Log.d(TAG, "📄 響應內容: " + body);
                } else {
                    Log.w(TAG, "⚠️ API連接測試響應異常: " + response.code());
                    Log.w(TAG, "可能的問題: API端點路徑不正確");
                }
            }
        });
    }

    /**
     * 分析面部圖片的介面
     */
    public interface AnalysisCallback {
        void onSuccess(AnalysisResult result);
        void onFailure(String error);
    }

    /**
     * 分析結果數據類
     */
    public static class AnalysisResult {
        public boolean success;
        public String error;
        public String originalImage;
        public String annotatedImage;
        public String abnormalOnlyImage;
        public int abnormalCount;
        public JSONObject overallColor;
        public JSONObject allRegionResults;
        public JSONObject regionResults;
        public JSONObject diagnoses;
        public String diagnosisText;
        public JSONObject gridAnalysis;

        public static AnalysisResult fromJson(JSONObject json) {
            AnalysisResult result = new AnalysisResult();
            try {
                result.success = json.optBoolean("success", false);
                result.error = json.optString("error", null);
                result.originalImage = json.optString("original_image", null);
                result.annotatedImage = json.optString("annotated_image", null);
                result.abnormalOnlyImage = json.optString("abnormal_only_image", null);
                result.abnormalCount = json.optInt("abnormal_count", 0);
                result.overallColor = json.optJSONObject("overall_color");
                result.allRegionResults = json.optJSONObject("all_region_results");
                result.regionResults = json.optJSONObject("region_results");
                result.diagnoses = json.optJSONObject("diagnoses");
                result.diagnosisText = json.optString("diagnosis_text", "");
                result.gridAnalysis = json.optJSONObject("grid_analysis");
            } catch (Exception e) {
                Log.e(TAG, "解析JSON結果時發生錯誤", e);
                result.success = false;
                result.error = "解析結果失敗: " + e.getMessage();
            }
            return result;
        }
    }

    /**
     * 將Bitmap轉換為Base64字符串
     */
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // 壓縮圖片以減少檔案大小
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);

            Log.d(TAG, "📷 圖片轉換完成，Base64長度: " + base64String.length());

            // 添加data URL前綴
            return "data:image/jpeg;base64," + base64String;

        } catch (Exception e) {
            Log.e(TAG, "Bitmap轉Base64失敗", e);
            throw new RuntimeException("圖片轉換失敗: " + e.getMessage());
        }
    }

    /**
     * 分析面部圖片
     */
    public void analyzeFace(Bitmap bitmap,int userId, AnalysisCallback callback) {
        analyzeFace(bitmap, userId,true, callback);
    }

    /**
     * 分析面部圖片（可選擇是否包含圖片）
     */
    public void analyzeFace(Bitmap bitmap,int userId, boolean includeImages, AnalysisCallback callback) {
        Log.d(TAG, "🚀 開始面部分析，圖片尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        try {
            // 轉換為Base64
            String base64Image = bitmapToBase64(bitmap);

            // 創建JSON請求體
            JSONObject requestJson = new JSONObject();
            requestJson.put("image", base64Image);
            requestJson.put("include_images", includeImages);

            // 創建HTTP請求
            RequestBody requestBody = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + ANALYZE_ENDPOINT)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-User-Id", String.valueOf(userId))   //把 user_id 放在 header
                    .build();

            Log.d(TAG, "📡 發送分析請求到: " + BASE_URL + ANALYZE_ENDPOINT);
            Log.d(TAG, "📦 請求體大小: " + requestJson.toString().length() + " 字符");

            // 異步執行請求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ 網絡請求失敗", e);

                    String errorMessage = "網絡連接失敗: " + e.getMessage();

                    // 提供更詳細的錯誤信息
                    if (e.getMessage().contains("Failed to connect")) {
                        errorMessage += "\n\n請檢查：\n" +
                                "• 後端服務是否在6060端口運行？\n" +
                                "• IP地址是否正確？當前: " + BASE_URL + "\n" +
                                "• 防火牆是否允許6060端口？\n" +
                                "• 在Postman中測試: " + BASE_URL + HEALTH_ENDPOINT;
                    } else if (e.getMessage().contains("timeout")) {
                        errorMessage += "\n\n連接超時，請檢查網絡狀況";
                    }

                    callback.onFailure(errorMessage);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "📨 API響應狀態碼: " + response.code());
                        Log.d(TAG, "📄 響應內容長度: " + responseBody.length());

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            AnalysisResult result = AnalysisResult.fromJson(jsonResponse);

                            if (result.success) {
                                Log.d(TAG, "✅ 分析成功！異常區域數量: " + result.abnormalCount);
                                callback.onSuccess(result);
                            } else {
                                Log.w(TAG, "⚠️ 分析失敗: " + result.error);
                                callback.onFailure(result.error != null ? result.error : "分析失敗");
                            }
                        } else {
                            Log.e(TAG, "❌ HTTP錯誤: " + response.code());
                            Log.e(TAG, "📄 錯誤響應: " + responseBody);

                            // 特別處理404錯誤
                            if (response.code() == 404) {
                                callback.onFailure("API端點不存在 (404)\n請檢查API路徑是否正確:\n" + BASE_URL + ANALYZE_ENDPOINT);
                            } else {
                                callback.onFailure("服務器錯誤 (" + response.code() + "): " + responseBody);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "❌ 解析響應JSON失敗", e);
                        callback.onFailure("解析服務器響應失敗: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "❌ 處理響應時發生未知錯誤", e);
                        callback.onFailure("處理結果時發生錯誤: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "❌ 創建請求JSON失敗", e);
            callback.onFailure("創建請求失敗: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ 發送請求時發生未知錯誤", e);
            callback.onFailure("發送請求失敗: " + e.getMessage());
        }
    }

    /**
     * 手動測試API連接
     */
    public void testConnection(TestCallback callback) {
        Log.d(TAG, "🔍 手動測試API連接...");

        Request request = new Request.Builder()
                .url(BASE_URL + HEALTH_ENDPOINT)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ 連接測試失敗", e);
                callback.onTestResult(false, "連接失敗: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                String message = success ?
                        "連接成功！狀態碼: " + response.code() :
                        "連接異常，狀態碼: " + response.code();

                Log.d(TAG, success ? "✅ " + message : "⚠️ " + message);
                callback.onTestResult(success, message);
            }
        });
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }
}