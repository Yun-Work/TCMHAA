package com.example.tcmhaa;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    // 全局靜態變量來保存分析結果和照片，直到下一次分析
    private static AnalysisResult globalAnalysisResult = null;
    private static String globalSourceType = null;
    private static String globalOriginalImageBase64 = null;
    private static boolean hasGlobalResult = false;

    private TextView tvTitle;
    private FrameLayout blockUserPhoto;
    private FrameLayout blockTextResult;
    private Button btnDone;

    private AnalysisResult analysisResult;
    private String sourceType;
    private String originalImageBase64;

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

        // 每次onResume都嘗試恢復全局數據
        if (hasGlobalResult && globalAnalysisResult != null) {
            analysisResult = globalAnalysisResult;
            sourceType = globalSourceType;
            originalImageBase64 = globalOriginalImageBase64;
            displayAnalysisResult();
            Log.d(TAG, "從全局緩存恢復分析結果和照片");
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        blockUserPhoto = findViewById(R.id.blockUserPhoto);
        blockTextResult = findViewById(R.id.blockTextResult);
        btnDone = findViewById(R.id.btnDone);

        // 修改：完成按鈕跳轉到MainhealthyActivity
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(_bMainActivity.this, MainhealthyActivity.class);
            // 清除任務堆疊，確保返回到主頁面
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void handleAnalysisResult() {
        Intent intent = getIntent();

        Log.d(TAG, "=== _bMainActivity 開始處理數據 ===");

        // 檢查是否有新的分析結果
        AnalysisResult newAnalysisResult = intent.getParcelableExtra("analysis_result");
        String newSourceType = intent.getStringExtra("source_type");

        // 修改：檢查是否應該從靜態變量獲取圖片數據
        String newOriginalImageBase64 = null;
        boolean useStaticImage = intent.getBooleanExtra("use_static_image", false);

        Log.d(TAG, "接收到的數據檢查:");
        Log.d(TAG, "- newAnalysisResult: " + (newAnalysisResult != null ? "存在" : "null"));
        Log.d(TAG, "- newSourceType: " + newSourceType);
        Log.d(TAG, "- useStaticImage: " + useStaticImage);

        if (useStaticImage) {
            // 從WarningActivity的靜態變量獲取圖片數據
            newOriginalImageBase64 = WarningActivity.getStoredImageData();
            Log.d(TAG, "從WarningActivity靜態變量獲取圖片數據，長度: " +
                    (newOriginalImageBase64 != null ? newOriginalImageBase64.length() : "null"));
        } else {
            // 直接從Intent獲取（適用於直接跳轉的情況，如PhotoActivity）
            newOriginalImageBase64 = intent.getStringExtra("original_image_base64");
            Log.d(TAG, "從Intent獲取圖片數據，長度: " +
                    (newOriginalImageBase64 != null ? newOriginalImageBase64.length() : "null"));
        }

        if (newOriginalImageBase64 != null && newOriginalImageBase64.length() > 50) {
            Log.d(TAG, "- Base64 前50字符: " + newOriginalImageBase64.substring(0, 50));
        }

        // 如果有新的分析結果，更新全局緩存
        if (newAnalysisResult != null) {
            Log.d(TAG, "收到新的分析結果，更新全局緩存");
            Log.d(TAG, "新的Base64數據長度: " + (newOriginalImageBase64 != null ? newOriginalImageBase64.length() : "null"));

            analysisResult = newAnalysisResult;
            sourceType = newSourceType;
            originalImageBase64 = newOriginalImageBase64;

            // 更新全局緩存
            globalAnalysisResult = analysisResult;
            globalSourceType = sourceType;
            globalOriginalImageBase64 = originalImageBase64;
            hasGlobalResult = true;

            Log.d(TAG, "全局緩存更新完成");
            Log.d(TAG, "globalOriginalImageBase64長度: " +
                    (globalOriginalImageBase64 != null ? globalOriginalImageBase64.length() : "null"));

            displayAnalysisResult();
            return;
        }

        // 如果沒有新資料但有全局緩存，使用全局緩存
        if (hasGlobalResult && globalAnalysisResult != null) {
            analysisResult = globalAnalysisResult;
            sourceType = globalSourceType;
            originalImageBase64 = globalOriginalImageBase64;
            displayAnalysisResult();
            Log.d(TAG, "使用全局緩存的分析結果");
            return;
        }

        // 如果既沒有新資料也沒有緩存，顯示錯誤
        Log.e(TAG, "未收到分析結果且無緩存數據");
        Toast.makeText(this, "未收到分析結果", Toast.LENGTH_SHORT).show();
        showErrorState();
    }

    /**
     * 清除全局緩存 - 當開始新的分析時調用
     */
    public static void clearGlobalCache() {
        globalAnalysisResult = null;
        globalSourceType = null;
        globalOriginalImageBase64 = null;
        hasGlobalResult = false;
        // 同時清理WarningActivity的靜態數據
        WarningActivity.clearStoredImageData();
        Log.d("_bMainActivity", "全局緩存已清除");
    }

    /**
     * 檢查是否有緩存的結果
     */
    public static boolean hasAnalysisResult() {
        return hasGlobalResult && globalAnalysisResult != null;
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
            // 顯示原始照片在照片區域
            displayOriginalPhoto();

            // 顯示分析結果文字
            displayAnalysisText();

            Log.d(TAG, "分析結果顯示完成");

        } catch (Exception e) {
            Log.e(TAG, "顯示分析結果時發生錯誤", e);
            showErrorState();
        }
    }

    private void displayOriginalPhoto() {
        try {
            // 清除現有內容
            blockUserPhoto.removeAllViews();

            // 詳細的Base64數據驗證和日誌
            Log.d(TAG, "=== 開始顯示原始照片 ===");
            Log.d(TAG, "originalImageBase64狀態檢查:");
            Log.d(TAG, "- 是否為null: " + (originalImageBase64 == null));

            if (originalImageBase64 != null) {
                Log.d(TAG, "- 長度: " + originalImageBase64.length());
                Log.d(TAG, "- 是否為空字符串: " + originalImageBase64.isEmpty());
                Log.d(TAG, "- 前50字符: " + originalImageBase64.substring(0, Math.min(50, originalImageBase64.length())));
                Log.d(TAG, "- 是否包含data:image: " + originalImageBase64.startsWith("data:image"));

                // 檢查是否包含有效的Base64數據
                if (originalImageBase64.contains(",")) {
                    String[] parts = originalImageBase64.split(",");
                    Log.d(TAG, "- split後部分數量: " + parts.length);
                    if (parts.length > 1) {
                        Log.d(TAG, "- Base64部分長度: " + parts[1].length());
                        Log.d(TAG, "- Base64部分前20字符: " + parts[1].substring(0, Math.min(20, parts[1].length())));
                    }
                }
            } else {
                Log.e(TAG, "originalImageBase64為null - 這是問題所在！");
            }

            if (originalImageBase64 != null && !originalImageBase64.isEmpty()) {
                // 創建ImageView來顯示照片
                ImageView imageView = new ImageView(this);

                // 設置佈局參數
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                imageView.setLayoutParams(params);

                // 設置縮放類型，保持長寬比並居中
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                try {
                    // 改進的Base64解析邏輯
                    String base64Image = originalImageBase64;

                    // 檢查並移除data URL前綴
                    if (base64Image.contains(",")) {
                        String[] parts = base64Image.split(",");
                        if (parts.length > 1) {
                            base64Image = parts[1];
                            Log.d(TAG, "移除data URL前綴後的Base64長度: " + base64Image.length());
                        } else {
                            Log.e(TAG, "Base64格式錯誤：無法找到逗號分隔符後的數據");
                            showPhotoError();
                            return;
                        }
                    }

                    // 驗證Base64字符串是否為空
                    if (base64Image.isEmpty()) {
                        Log.e(TAG, "處理後的Base64字符串為空");
                        showPhotoError();
                        return;
                    }

                    // 解碼Base64
                    byte[] imageBytes;
                    try {
                        Log.d(TAG, "開始解碼Base64...");
                        imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Log.d(TAG, "Base64解碼成功，字節數組長度: " + imageBytes.length);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Base64解碼失敗：格式不正確", e);
                        showPhotoError();
                        return;
                    }

                    if (imageBytes == null || imageBytes.length == 0) {
                        Log.e(TAG, "Base64解碼後的字節數組為空");
                        showPhotoError();
                        return;
                    }

                    // 解析為Bitmap
                    Log.d(TAG, "開始創建Bitmap...");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    if (bitmap != null) {
                        Log.d(TAG, "Bitmap創建成功，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        imageView.setImageBitmap(bitmap);
                        blockUserPhoto.addView(imageView);
                        Log.d(TAG, "成功顯示原始照片");
                    } else {
                        Log.e(TAG, "BitmapFactory.decodeByteArray返回null");
                        showPhotoError();
                    }

                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "記憶體不足，無法解析圖片", e);
                    showPhotoError();
                } catch (Exception e) {
                    Log.e(TAG, "解析照片時發生錯誤", e);
                    showPhotoError();
                }

            } else {
                // 如果沒有照片，顯示佔位符
                Log.w(TAG, "沒有照片數據，顯示佔位符");
                showPhotoPlaceholder();
            }

        } catch (Exception e) {
            Log.e(TAG, "顯示原始照片時發生錯誤", e);
            showPhotoError();
        }
    }

    private void showPhotoPlaceholder() {
        LinearLayout placeholderLayout = new LinearLayout(this);
        placeholderLayout.setOrientation(LinearLayout.VERTICAL);
        placeholderLayout.setGravity(Gravity.CENTER);

        TextView placeholderView = new TextView(this);
        placeholderView.setText("📷\n照片已分析");
        placeholderView.setTextSize(20);
        placeholderView.setGravity(Gravity.CENTER);
        placeholderView.setTextColor(getColor(android.R.color.darker_gray));
        placeholderLayout.addView(placeholderView);

        TextView infoView = new TextView(this);
        infoView.setText("原始照片數據不可用");
        infoView.setTextSize(12);
        infoView.setGravity(Gravity.CENTER);
        infoView.setTextColor(getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = 8;
        infoView.setLayoutParams(infoParams);
        placeholderLayout.addView(infoView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        placeholderLayout.setLayoutParams(params);
        blockUserPhoto.addView(placeholderLayout);
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
            titleView.setText("詳細分析結果");
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
            summaryLabel.setText("檢測狀態：");
            summaryLabel.setTextSize(14);
            summaryLabel.setTextColor(getColor(android.R.color.black));
            summaryLayout.addView(summaryLabel);

            TextView summaryValue = new TextView(this);
            summaryValue.setText(analysisResult.getStatusSummary());
            if (analysisResult.abnormalCount > 0) {
                summaryValue.setTextColor(getColor(android.R.color.holo_red_dark));
            } else {
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
                    resultLayout.addView(divider);

                    TextView abnormalTitle = new TextView(this);
                    abnormalTitle.setText("異常區域詳情");
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
                resultLayout.addView(divider);

                TextView diagnosisTitle = new TextView(this);
                diagnosisTitle.setText("可能的症狀");
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

    private void showPhotoError() {
        blockUserPhoto.removeAllViews();

        LinearLayout errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(Gravity.CENTER);

        TextView errorView = new TextView(this);
        errorView.setText("📷\n照片顯示失敗");
        errorView.setTextColor(getColor(android.R.color.holo_red_dark));
        errorView.setTextSize(16);
        errorView.setGravity(Gravity.CENTER);
        errorLayout.addView(errorView);

        TextView detailView = new TextView(this);
        detailView.setText("圖像數據可能已損壞");
        detailView.setTextColor(getColor(android.R.color.darker_gray));
        detailView.setTextSize(12);
        detailView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = 8;
        detailView.setLayoutParams(detailParams);
        errorLayout.addView(detailView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        errorLayout.setLayoutParams(params);
        blockUserPhoto.addView(errorLayout);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 當確定不再需要圖片數據時清理
        if (isFinishing()) {
            // 清理WarningActivity的靜態數據
            WarningActivity.clearStoredImageData();
        }
    }
}