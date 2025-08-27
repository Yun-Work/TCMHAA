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
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_n);

        initViews();
        initApiService();
        setupContent();
    }

    private void initViews() {
        // 修正：使用正確的ID名稱
        tvWarningMessage = findViewById(R.id.text_message);
        // 如果XML中的ID是text_message，請改為：
        // tvWarningMessage = findViewById(R.id.text_message);

        btnYes = findViewById(R.id.btn_yes);
        btnNo = findViewById(R.id.btn_no);
    }

    private void initApiService() {
        apiService = new ApiService();
    }

    private void setupContent() {
        Intent from = getIntent();
        hasMoles = from.getBooleanExtra("has_moles", false);

        if (hasMoles) {
            // 有痣的情況
            AnalysisResult result = from.getParcelableExtra("analysis_result");

            StringBuilder description = new StringBuilder("檢測到您的面部存在明顯的痣");

            if (result != null && result.moleCount > 0) {
                description.append("\n發現 ").append(result.moleCount).append(" 個痣");
            }

            description.append("\n\n這些痣可能會影響面部膚色分析的準確性。");
            description.append("\n\n是否要移除這些痣以獲得更準確的分析結果？");

            tvWarningMessage.setText(description.toString());

            btnYes.setText("是");
            btnNo.setText("否");

            btnYes.setOnClickListener(v -> processWithMoleRemoval());
            btnNo.setOnClickListener(v -> processWithoutMoleRemoval());

        } else {
            // 沒有痣的情況，直接前往結果頁面
            goToMainActivity();
        }
    }

    /**
     * 選擇移除痣：重新分析（移除痣）
     */
    private void processWithMoleRemoval() {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("處理中")
                .setMessage("正在移除痣並重新分析，請稍候...")
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

        // 重新分析，這次移除痣
        apiService.analyzeFaceWithBase64(originalImageBase64, true, new ApiService.AnalysisCallback() {
            @Override
            public void onSuccess(ApiService.AnalysisResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.d(TAG, "移除痣後重新分析成功");

                    // 前往主結果頁面，帶上新的分析結果
                    Intent intent = new Intent(WarningActivity.this, _bMainActivity.class);

                    AnalysisResult parcelableResult = new AnalysisResult(result);
                    intent.putExtra("analysis_result", parcelableResult);
                    intent.putExtra("source_type", from.getStringExtra("source_type"));
                    intent.putExtra("original_image_base64", originalImageBase64);
                    intent.putExtra("moles_removed", true);
                    intent.putExtra("has_moles", false); // 已處理，標記為無痣

                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "移除痣後重新分析失敗: " + error);

                    new AlertDialog.Builder(WarningActivity.this)
                            .setTitle("處理失敗")
                            .setMessage("移除痣時發生錯誤：\n" + error + "\n\n將使用原始分析結果繼續。")
                            .setPositiveButton("確定", (dialog, which) -> processWithoutMoleRemoval())
                            .show();
                });
            }
        });
    }

    /**
     * 選擇不移除痣：使用原始分析結果
     */
    private void processWithoutMoleRemoval() {
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
            // 標記痣未被移除
            intent.putExtra("moles_removed", false);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // 修正：調用super方法避免警告
        super.onBackPressed();

        // 防止用戶返回到相機或照片選擇頁面
        // 直接前往主活動頁面
        processWithoutMoleRemoval();
    }
}