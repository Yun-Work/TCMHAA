package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class WarningActivity extends AppCompatActivity {
    private static final String TAG = "WarningActivity";

    // 添加靜態變量來暫存大型數據，解決Intent大小限制問題
    private static String tempOriginalImageBase64 = null;

    private TextView tvWarningMessage;
    private Button btnYes, btnNo;
    private boolean hasMoles = false;
    private boolean hasBeard = false;
    private ApiService apiService;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_n);

        initViews();
        initApiService();
        setupContent();

        // 保存大型數據到靜態變量，避免多次Intent傳遞導致數據丟失
        Intent from = getIntent();
        String originalImageBase64 = from.getStringExtra("original_image_base64");
        if (originalImageBase64 != null && !originalImageBase64.isEmpty()) {
            tempOriginalImageBase64 = originalImageBase64;
            Log.d(TAG, "已保存照片數據到靜態變量，長度: " + originalImageBase64.length());
        } else {
            Log.w(TAG, "未收到有效的照片數據");
        }
    }

    private void initViews() {
        tvWarningMessage = findViewById(R.id.text_message);
        btnYes = findViewById(R.id.btn_yes);
        btnNo = findViewById(R.id.btn_no);
    }

    private void initApiService() {
        apiService = new ApiService();
        userId = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);

        if (userId == -1) {
            Log.e(TAG, "用戶ID無效");
            Toast.makeText(this, "用戶身份驗證失敗", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.setUserId(userId);
        Log.d(TAG, "設置用戶ID: " + userId);
    }

    private void setupContent() {
        Intent from = getIntent();

        // 從AnalysisResult獲取檢測結果
        AnalysisResult result = from.getParcelableExtra("analysis_result");

        if (result != null) {
            hasMoles = result.hasAnyMoles();
            hasBeard = result.hasAnyBeard();

            // 重要：檢查鬍鬚數量，如果 <= 1 則不視為明顯鬍鬚
            if (hasBeard && result.getBeardCount() <= 1) {
                hasBeard = false;
                Log.d(TAG, "鬍鬚數量 <= 1，不視為明顯鬍鬚");
            }

            Log.d(TAG, "從AnalysisResult檢測結果: 痣=" + hasMoles + ", 鬍鬚=" + hasBeard + ", 鬍鬚數量=" + result.getBeardCount());
        } else {
            // 備用方法：從Intent額外參數獲取
            hasMoles = from.getBooleanExtra("has_moles", false);
            hasBeard = from.getBooleanExtra("has_beard", false);

            // 新增：檢查Intent中的鬍鬚數量
            int beardCount = from.getIntExtra("beard_count", 0);
            if (hasBeard && beardCount <= 1) {
                hasBeard = false;
                Log.d(TAG, "從Intent獲取：鬍鬚數量 <= 1，不視為明顯鬍鬚");
            }

            Log.d(TAG, "從Intent獲取檢測結果: 痣=" + hasMoles + ", 鬍鬚=" + hasBeard + ", 鬍鬚數量=" + beardCount);
        }

        if (hasMoles || hasBeard) {
            // 有痣或鬍鬚的情況
            setupWarningContent(result);
        } else {
            // 沒有痣也沒有鬍鬚，直接前往結果頁面
            Log.d(TAG, "無明顯特徵，直接前往主結果頁面");
            goToMainActivity();
        }
    }

    private void setupWarningContent(AnalysisResult result) {
        StringBuilder description = new StringBuilder();

        if (hasMoles && hasBeard) {
            // 同時有痣和鬍鬚
            description.append("檢測到您的面部同時存在明顯的痣和鬍鬚");
            if (result != null && result.getMoleCount() > 0) {
                description.append("\n發現 ").append(result.getMoleCount()).append(" 個痣");
            }
            description.append("\n\n這些特徵可能會影響面部膚色分析的準確性。");
            description.append("\n\n是否要移除以獲得更準確的分析結果？");

        } else if (hasMoles) {
            // 只有痣
            description.append("檢測到您的面部存在明顯的痣");

            if (result != null && result.getMoleCount() > 0) {
                description.append("\n發現 ").append(result.getMoleCount()).append(" 個痣");
            }

            description.append("\n\n這些痣可能會影響面部膚色分析的準確性。");
            description.append("\n\n是否要移除以獲得更準確的分析結果？");

        } else if (hasBeard) {
            // 只有鬍鬚
            description.append("檢測到您的面部存在明顯的鬍鬚");
            description.append("\n\n鬍鬚可能會影響面部膚色分析的準確性。");
            description.append("\n\n是否要移除以獲得更準確的分析結果？");
        }

        tvWarningMessage.setText(description.toString());

        btnYes.setText("是");
        btnNo.setText("否");

        btnYes.setOnClickListener(v -> processWithFeatureRemoval());
        btnNo.setOnClickListener(v -> processWithoutRemoval());
    }

    /**
     * 選擇移除特徵：重新分析（移除痣/鬍鬚）
     */
    private void processWithFeatureRemoval() {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("處理中")
                .setMessage(buildProcessingMessage())
                .setCancelable(false)
                .create();
        progressDialog.show();

        // 修改：從靜態變量獲取照片數據，而不是從Intent
        String originalImageBase64 = tempOriginalImageBase64;

        // 關鍵修復：添加詳細的Base64數據驗證
        if (originalImageBase64 == null) {
            Log.e(TAG, "靜態變量中的照片數據為null");
            progressDialog.dismiss();
            Toast.makeText(this, "照片數據遺失，將使用原始分析結果", Toast.LENGTH_SHORT).show();
            processWithoutRemoval();
            return;
        }

        if (originalImageBase64.isEmpty()) {
            Log.e(TAG, "靜態變量中的照片數據為空字符串");
            progressDialog.dismiss();
            Toast.makeText(this, "照片數據為空，將使用原始分析結果", Toast.LENGTH_SHORT).show();
            processWithoutRemoval();
            return;
        }

        // 新增：驗證Base64格式
        if (!originalImageBase64.startsWith("data:image/")) {
            Log.e(TAG, "照片數據格式不正確: " + originalImageBase64.substring(0, Math.min(50, originalImageBase64.length())));
            progressDialog.dismiss();
            Toast.makeText(this, "圖像數據格式錯誤，將使用原始分析結果", Toast.LENGTH_SHORT).show();
            processWithoutRemoval();
            return;
        }

        Log.d(TAG, "從靜態變量獲取照片數據，長度: " + originalImageBase64.length());

        boolean removeMoles = hasMoles;
        boolean removeBeard = hasBeard;

        Log.d(TAG, "用戶選擇移除特徵 - removeMoles: " + removeMoles + ", removeBeard: " + removeBeard);

        // 修復：確保userId正確獲取
        if (userId == -1) {
            Log.e(TAG, "用戶ID無效");
            progressDialog.dismiss();
            Toast.makeText(this, "用戶身份驗證失敗", Toast.LENGTH_SHORT).show();
            processWithoutRemoval();
            return;
        }

        // 重新分析，移除檢測到的特徵
        apiService.analyzeFaceWithFeatureRemoval(
                originalImageBase64,
                removeMoles,
                removeBeard,
                userId,
                new ApiService.AnalysisCallback() {
                    @Override
                    public void onSuccess(ApiService.AnalysisResult result) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.d(TAG, "移除特徵後重新分析成功");

                            // 前往主結果頁面，帶上新的分析結果
                            Intent intent = new Intent(WarningActivity.this, _bMainActivity.class);

                            AnalysisResult parcelableResult = new AnalysisResult(result);
                            intent.putExtra("analysis_result", parcelableResult);
                            intent.putExtra("source_type", getIntent().getStringExtra("source_type"));

                            // 重要修改：不通過Intent傳遞大型Base64數據
                            // 而是讓_bMainActivity從靜態方法獲取
                            intent.putExtra("use_static_image", true);
                            intent.putExtra("moles_removed", removeMoles);
                            intent.putExtra("beard_removed", removeBeard);
                            intent.putExtra("has_moles", false); // 已處理
                            intent.putExtra("has_beard", false); // 已處理

                            Log.d(TAG, "跳轉到_bMainActivity，使用靜態圖片數據");

                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.e(TAG, "移除特徵後重新分析失敗: " + error);

                            new AlertDialog.Builder(WarningActivity.this)
                                    .setTitle("處理失敗")
                                    .setMessage("移除特徵時發生錯誤：\n" + error + "\n\n將使用原始分析結果繼續。")
                                    .setPositiveButton("確定", (dialog, which) -> processWithoutRemoval())
                                    .show();
                        });
                    }
                });
    }

    private String buildProcessingMessage() {
        StringBuilder message = new StringBuilder("正在");

        if (hasMoles && hasBeard) {
            message.append("移除痣和鬍鬚");
        } else if (hasMoles) {
            message.append("移除痣");
        } else if (hasBeard) {
            message.append("移除鬍鬚");
        }

        message.append("並重新分析，請稍候...");
        return message.toString();
    }

    /**
     * 選擇不移除特徵：使用原始分析結果
     */
    private void processWithoutRemoval() {
        Log.d(TAG, "用戶選擇保持原樣，使用原始分析結果");
        goToMainActivity();
    }

    /**
     * 前往主結果頁面
     */
    private void goToMainActivity() {
        Intent from = getIntent();
        Intent intent = new Intent(WarningActivity.this, _bMainActivity.class);

        if (from != null) {
            // 只傳遞輕量級數據
            AnalysisResult result = from.getParcelableExtra("analysis_result");
            if (result != null) {
                intent.putExtra("analysis_result", result);
            }
            intent.putExtra("source_type", from.getStringExtra("source_type"));
            intent.putExtra("from_camera", from.getBooleanExtra("from_camera", false));
            intent.putExtra("from_photo", from.getBooleanExtra("from_photo", false));
            intent.putExtra("has_moles", from.getBooleanExtra("has_moles", false));
            intent.putExtra("has_beard", from.getBooleanExtra("has_beard", false));

            // 標記使用靜態圖片數據
            intent.putExtra("use_static_image", true);
            intent.putExtra("moles_removed", false);
            intent.putExtra("beard_removed", false);

            Log.d(TAG, "goToMainActivity - 使用靜態圖片數據傳遞");
        }

        startActivity(intent);
        finish();
    }

    // 提供靜態方法讓其他Activity獲取照片數據
    public static String getStoredImageData() {
        return tempOriginalImageBase64;
    }

    // 清理靜態數據的方法
    public static void clearStoredImageData() {
        tempOriginalImageBase64 = null;
        Log.d("WarningActivity", "已清理靜態照片數據");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 防止用戶返回到相機或照片選擇頁面
        // 直接前往主活動頁面
        processWithoutRemoval();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Activity銷毀時不立即清理，讓下個Activity能夠使用
        // 清理工作交給_bMainActivity處理
    }
}