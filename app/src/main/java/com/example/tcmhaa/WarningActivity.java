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
    }

    private void initViews() {
        tvWarningMessage = findViewById(R.id.text_message);
        btnYes = findViewById(R.id.btn_yes);
        btnNo = findViewById(R.id.btn_no);
    }

    private void initApiService() {
        apiService = new ApiService();
        userId = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
    }

    private void setupContent() {
        Intent from = getIntent();

        // 從 AnalysisResult 獲取檢測結果
        AnalysisResult result = from.getParcelableExtra("analysis_result");

        if (result != null) {
            hasMoles = result.hasAnyMoles();
            hasBeard = result.hasAnyBeard();

            // 🔥 新增：檢查鬍鬚數量，如果 <= 1 則不視為明顯鬍鬚
            if (hasBeard && result.getBeardCount() <= 1) {
                hasBeard = false;
                Log.d(TAG, "鬍鬚數量 <= 1，不視為明顯鬍鬚");
            }

            Log.d(TAG, "檢測結果: 痣=" + hasMoles + ", 鬍鬚=" + hasBeard + ", 鬍鬚數量=" + result.getBeardCount());
        } else {
            // 備用方法：從 Intent 額外參數獲取
            hasMoles = from.getBooleanExtra("has_moles", false);
            hasBeard = from.getBooleanExtra("has_beard", false);

            // 🔥 新增：檢查 Intent 中的鬍鬚數量
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
            // 只有鬍鬚 - 簡化顯示，不顯示具體數量
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

        Intent from = getIntent();
        String originalImageBase64 = from.getStringExtra("original_image_base64");

        if (originalImageBase64 == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "原始圖像數據遺失", Toast.LENGTH_SHORT).show();
            goToMainActivity();
            return;
        }



        boolean removeMoles = hasMoles;
        boolean removeBeard = hasBeard;

        Log.d(TAG, "用戶選擇移除特徵 - removeMoles: " + removeMoles + ", removeBeard: " + removeBeard);

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
                            intent.putExtra("source_type", from.getStringExtra("source_type"));
                            intent.putExtra("original_image_base64", originalImageBase64);
                            intent.putExtra("moles_removed", removeMoles);
                            intent.putExtra("beard_removed", removeBeard);
                            intent.putExtra("has_moles", false); // 已處理
                            intent.putExtra("has_beard", false); // 已處理

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
            // 傳遞所有原始資料
            intent.putExtras(from);
            // 標記特徵未被移除
            intent.putExtra("moles_removed", false);
            intent.putExtra("beard_removed", false);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 防止用戶返回到相機或照片選擇頁面
        // 直接前往主活動頁面
        processWithoutRemoval();
    }
}