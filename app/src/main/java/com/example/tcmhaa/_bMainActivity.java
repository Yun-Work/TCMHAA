package com.example.tcmhaa;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.Iterator;

public class _bMainActivity extends AppCompatActivity {
    private static final String TAG = "_bMainActivity";

    // 🎯 新增靜態變量來保存分析結果
    private static AnalysisResult cachedAnalysisResult = null;
    private static String cachedSourceType = null;
    private static boolean hasDisplayedResult = false;

    private ImageView ivLogo;
    private TextView tvTitle;
    private FrameLayout blockUserPhoto;
    private FrameLayout blockTextResult;
    private Button btnDone;

    private AnalysisResult analysisResult;
    private String sourceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_b);

        initViews();
        handleAnalysisResult();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🎯 從其他頁面返回時，恢復之前的分析結果
        if (hasDisplayedResult && cachedAnalysisResult != null) {
            analysisResult = cachedAnalysisResult;
            sourceType = cachedSourceType;
            displayAnalysisResult();
            Log.d(TAG, "從緩存恢復分析結果");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 🎯 保存當前的分析結果到緩存
        if (analysisResult != null) {
            cachedAnalysisResult = analysisResult;
            cachedSourceType = sourceType;
            hasDisplayedResult = true;
            Log.d(TAG, "分析結果已保存到緩存");
        }
    }

    private void initViews() {
        ivLogo = findViewById(R.id.ivLogo);
        tvTitle = findViewById(R.id.tvTitle);
        blockUserPhoto = findViewById(R.id.blockUserPhoto);
        blockTextResult = findViewById(R.id.blockTextResult);
        btnDone = findViewById(R.id.btnDone);

        btnDone.setOnClickListener(v -> {
            // 🎯 點擊完成時清空緩存

            finish();
        });
    }

    private void handleAnalysisResult() {
        Intent intent = getIntent();

        // 🎯 檢查是否有新的分析結果
        AnalysisResult newAnalysisResult = intent.getParcelableExtra("analysis_result");
        String newSourceType = intent.getStringExtra("source_type");
        String originalImageBase64 = intent.getStringExtra("original_image_base64");

        // 如果有新的分析結果，直接使用新資料並更新快取
        if (newAnalysisResult != null) {
            Log.d(TAG, "收到新的分析結果，更新快取");

            analysisResult = newAnalysisResult;
            sourceType = newSourceType;

            // 將原始圖片數據添加到結果中以便顯示
            if (originalImageBase64 != null) {
                analysisResult.originalImage = originalImageBase64;
            }

            // 🎯 更新靜態快取
            cachedAnalysisResult = analysisResult;
            cachedSourceType = sourceType;
            hasDisplayedResult = true;

            displayAnalysisResult();
            return;
        }

        // 🎯 只有在沒有新資料時才使用快取
        if (hasDisplayedResult && cachedAnalysisResult != null) {
            analysisResult = cachedAnalysisResult;
            sourceType = cachedSourceType;
            displayAnalysisResult();
            Log.d(TAG, "使用快取的分析結果");
            return;
        }

        // 如果既沒有新資料也沒有快取，顯示錯誤
        Log.e(TAG, "未收到分析結果");
        Toast.makeText(this, "未收到分析結果", Toast.LENGTH_SHORT).show();
        showErrorState();
    }

    private void displayAnalysisResult() {
        if (analysisResult == null) {
            showErrorState();
            return;
        }

        if (!analysisResult.success) {
            showErrorResult();
            return;
        }

        try {
            // 顯示用戶照片
            displayUserPhoto();

            // 顯示分析結果文字
            displayAnalysisText();

            Log.d(TAG, "分析結果顯示完成");

        } catch (Exception e) {
            Log.e(TAG, "顯示分析結果時發生錯誤", e);
            showErrorState();
        }
    }

    private void displayUserPhoto() {
        try {
            // 清除現有內容
            blockUserPhoto.removeAllViews();

            String imageBase64 = analysisResult.originalImage;

            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // 顯示原始圖片
                Bitmap bitmap = base64ToBitmap(imageBase64);

                if (bitmap != null) {
                    ImageView imageView = new ImageView(this);
                    imageView.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(bitmap);

                    blockUserPhoto.addView(imageView);
                    Log.d(TAG, "原始照片顯示完成");

                } else {
                    showPhotoPlaceholder();
                }
            } else {
                showPhotoPlaceholder();
            }

        } catch (Exception e) {
            Log.e(TAG, "顯示用戶照片時發生錯誤", e);
            showPhotoError();
        }
    }

    private void showPhotoPlaceholder() {
        TextView placeholderView = new TextView(this);
        placeholderView.setText("📷 分析完成\n\n" +
                "來源：" + (sourceType.equals("camera") ? "相機拍攝" : "相簿選擇") + "\n" +
                "狀態：處理成功");
        placeholderView.setTextSize(16);
        placeholderView.setGravity(Gravity.CENTER);
        placeholderView.setTextColor(getColor(android.R.color.black));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.CENTER;
        placeholderView.setLayoutParams(params);

        blockUserPhoto.addView(placeholderView);
    }

    private void displayAnalysisText() {
        try {
            // 清除現有內容
            blockTextResult.removeAllViews();

            // 創建滾動視圖來顯示結果
            ScrollView scrollView = new ScrollView(this);
            LinearLayout resultLayout = new LinearLayout(this);
            resultLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            resultLayout.setOrientation(LinearLayout.VERTICAL);
            resultLayout.setPadding(16, 16, 16, 16);

            // 分析標題
            TextView titleView = new TextView(this);
            titleView.setText("🎉 面部膚色分析結果");
            titleView.setTextSize(18);
            titleView.setTextColor(getColor(R.color.titlePurple));
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setPadding(0, 0, 0, 16);
            resultLayout.addView(titleView);

            // 整體結果摘要
            LinearLayout summaryLayout = new LinearLayout(this);
            summaryLayout.setOrientation(LinearLayout.HORIZONTAL);
            summaryLayout.setPadding(0, 0, 0, 16);

            TextView summaryLabel = new TextView(this);
            summaryLabel.setText("📊 檢測結果：");
            summaryLabel.setTextSize(14);
            summaryLabel.setTextColor(getColor(android.R.color.black));
            summaryLayout.addView(summaryLabel);

            TextView summaryValue = new TextView(this);
            if (analysisResult.abnormalCount > 0) {
                summaryValue.setText(analysisResult.abnormalCount + " 個異常區域");
                summaryValue.setTextColor(getColor(android.R.color.holo_red_dark));
            } else {
                summaryValue.setText("所有區域正常");
                summaryValue.setTextColor(getColor(android.R.color.holo_green_dark));
            }
            summaryValue.setTextSize(14);
            summaryValue.setTypeface(null, Typeface.BOLD);
            summaryLayout.addView(summaryValue);

            resultLayout.addView(summaryLayout);

            // 顯示異常區域詳情
            if (analysisResult.abnormalCount > 0) {
                JSONObject regionResults = analysisResult.getRegionResults();
                if (regionResults != null && regionResults.length() > 0) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 2));
                    divider.setBackgroundColor(getColor(android.R.color.darker_gray));


                    TextView abnormalTitle = new TextView(this);
                    abnormalTitle.setText("⚠️ 異常區域詳情");
                    abnormalTitle.setTextSize(16);
                    abnormalTitle.setTypeface(null, Typeface.BOLD);
                    abnormalTitle.setTextColor(getColor(android.R.color.holo_red_dark));
                    abnormalTitle.setPadding(0, 16, 0, 8);
                    resultLayout.addView(abnormalTitle);

                    Iterator<String> keys = regionResults.keys();
                    while (keys.hasNext()) {
                        String region = keys.next();
                        try {
                            String condition = regionResults.getString(region);
                            TextView regionView = new TextView(this);
                            regionView.setText("• " + region + "：" + condition);
                            regionView.setTextSize(14);
                            regionView.setPadding(16, 4, 0, 4);
                            regionView.setTextColor(getColor(android.R.color.black));
                            resultLayout.addView(regionView);
                        } catch (Exception e) {
                            Log.w(TAG, "解析區域結果失敗: " + region, e);
                        }
                    }
                }
            }

            // 如果有診斷文字，顯示診斷建議
            if (analysisResult.diagnosisText != null && !analysisResult.diagnosisText.trim().isEmpty()) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(getColor(android.R.color.darker_gray));


                TextView diagnosisTitle = new TextView(this);
                diagnosisTitle.setText("📋 診斷建議");
                diagnosisTitle.setTextSize(16);
                diagnosisTitle.setTypeface(null, Typeface.BOLD);
                diagnosisTitle.setTextColor(getColor(R.color.titlePurple));
                diagnosisTitle.setPadding(0, 16, 0, 8);
                resultLayout.addView(diagnosisTitle);

                TextView diagnosisView = new TextView(this);
                diagnosisView.setText(analysisResult.diagnosisText);
                diagnosisView.setTextSize(14);
                diagnosisView.setLineSpacing(4, 1.0f);
                diagnosisView.setPadding(8, 0, 0, 12);
                diagnosisView.setTextColor(getColor(android.R.color.black));
                resultLayout.addView(diagnosisView);
            }

            scrollView.addView(resultLayout);
            blockTextResult.addView(scrollView);

            Log.d(TAG, "分析文字顯示完成");

        } catch (Exception e) {
            Log.e(TAG, "顯示分析文字時發生錯誤", e);
            showTextError();
        }
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            // 移除data URL前綴（如果存在）
            if (base64String.startsWith("data:image")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Base64轉Bitmap失敗", e);
            return null;
        }
    }

    private void showPhotoError() {
        blockUserPhoto.removeAllViews();
        TextView errorView = new TextView(this);
        errorView.setText("圖片顯示失敗");
        errorView.setTextColor(getColor(android.R.color.holo_red_dark));
        errorView.setTextSize(16);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        errorView.setLayoutParams(params);
        blockUserPhoto.addView(errorView);
    }

    private void showTextError() {
        blockTextResult.removeAllViews();
        TextView errorView = new TextView(this);
        errorView.setText("分析結果顯示失敗");
        errorView.setTextColor(getColor(android.R.color.holo_red_dark));
        errorView.setTextSize(16);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        errorView.setLayoutParams(params);
        blockTextResult.addView(errorView);
    }

    private void showErrorResult() {
        // 顯示錯誤信息
        blockUserPhoto.removeAllViews();
        blockTextResult.removeAllViews();

        TextView errorTitle = new TextView(this);
        errorTitle.setText("分析失敗");
        errorTitle.setTextColor(getColor(android.R.color.holo_red_dark));
        errorTitle.setTextSize(20);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = Gravity.CENTER;
        errorTitle.setLayoutParams(titleParams);
        blockUserPhoto.addView(errorTitle);

        TextView errorMessage = new TextView(this);
        errorMessage.setText(analysisResult.error != null ? analysisResult.error : "未知錯誤");
        errorMessage.setTextSize(14);
        errorMessage.setPadding(16, 16, 16, 16);
        FrameLayout.LayoutParams messageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.gravity = Gravity.CENTER;
        errorMessage.setLayoutParams(messageParams);
        blockTextResult.addView(errorMessage);
    }

    private void showErrorState() {
        blockUserPhoto.removeAllViews();
        blockTextResult.removeAllViews();

        TextView errorView = new TextView(this);
        errorView.setText("無法載入分析結果");
        errorView.setTextColor(getColor(android.R.color.holo_red_dark));
        errorView.setTextSize(18);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        errorView.setLayoutParams(params);
        blockUserPhoto.addView(errorView);

        TextView instructionView = new TextView(this);
        instructionView.setText("請返回重新進行分析");
        instructionView.setTextSize(14);
        instructionView.setPadding(16, 16, 16, 16);
        FrameLayout.LayoutParams instructionParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        instructionParams.gravity = Gravity.CENTER;
        instructionView.setLayoutParams(instructionParams);
        blockTextResult.addView(instructionView);
    }

    private void setupBottomNav() {
        LinearLayout navA = findViewById(R.id.nav_a);
        LinearLayout navB = findViewById(R.id.nav_b);
        LinearLayout navC = findViewById(R.id.nav_c);
        LinearLayout navD = findViewById(R.id.nav_d);

        navA.setOnClickListener(v -> startActivity(new Intent(this, _aMainActivity.class)));
        navB.setOnClickListener(v -> { /* 留在本頁 */ });
        navC.setOnClickListener(v -> startActivity(new Intent(this, _cMainActivity.class)));
        navD.setOnClickListener(v -> startActivity(new Intent(this, _dMainActivity.class)));
    }
}