package com.example.tcmhaa;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoActivity extends AppCompatActivity {
    private static final String TAG = "PhotoActivity";

    private ImageView imagePreview;
    private Button btnPickPhoto, btnBack;
    private Uri selectedImageUri;
    private ApiService apiService;

    // 選取圖片（相簿）
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    showPreview(uri);
                } else {
                    Toast.makeText(this, "未選擇任何圖片", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestReadImagesPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    pickImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "未獲得圖片讀取權限", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_5_1);

        initViews();
        initApiService();
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        btnBack = findViewById(R.id.btnBack);

        btnPickPhoto.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                pickImage();
            } else {
                // 已選擇圖片 → 跳提醒再分析
                Intent i = new Intent(PhotoActivity.this, WarningActivity.class);
                i.putExtra("source_type", "photo");
                // 也可以把圖片 Uri 一起帶去
                i.putExtra("selected_uri", selectedImageUri.toString());
                startActivity(i);
            }
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void initApiService() {
        apiService = new ApiService();
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要 READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                requestReadImagesPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12-：GetContent 不需要額外權限
            pickImageLauncher.launch("image/*");
        }
    }

    private void showPreview(@NonNull Uri uri) {
        try {
            imagePreview.setImageURI(uri);
            // 更改按鈕文字提示用戶可以開始分析
            btnPickPhoto.setText("開始分析");
            Toast.makeText(this, "圖片已選擇，點擊「開始分析」進行面部分析", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "顯示圖片預覽失敗", e);
            Toast.makeText(this, "顯示圖片失敗", Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeSelectedImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "請先選擇圖片", Toast.LENGTH_SHORT).show();
            return;
        }

        _bMainActivity.clearGlobalCache();

        // 進度對話框
        // 顯示進度對話框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("分析中")
                .setMessage("正在進行面部膚色分析，請稍候...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        try {
            // 將URI轉換為Bitmap
            Bitmap originalBitmap = uriToBitmap(selectedImageUri);

            if (originalBitmap == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "無法載入圖片，請重新選擇", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔧 關鍵：保存原始圖片的Base64數據
            String originalImageBase64 = bitmapToBase64(originalBitmap);

            Log.d(TAG, "開始分析圖片，尺寸: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

            // 呼叫後端分析（第一次分析：僅檢測痣，不移除）
            apiService.analyzeFaceWithMoleDetection(originalBitmap, false, new ApiService.AnalysisCallback() {
                @Override
                public void onSuccess(ApiService.AnalysisResult result) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.d(TAG, "分析成功");

                        // 檢查是否有痣
                        boolean hasMoles = result.hasMoles();  // 修正：改為 hasMoles()

                        if (hasMoles) {
                            Log.d(TAG, "檢測到痣，前往 WarningActivity");
                            // 有痣，前往警告頁面
                            Intent intent = new Intent(PhotoActivity.this, WarningActivity.class);

                            AnalysisResult parcelableResult = new AnalysisResult(result);
                            intent.putExtra("analysis_result", parcelableResult);
                            intent.putExtra("source_type", "photo");
                            intent.putExtra("original_image_base64", originalImageBase64);
                            intent.putExtra("from_photo", true);
                            intent.putExtra("has_moles", true);  // 修正：改為 has_moles

                            startActivity(intent);
                        } else {
                            Log.d(TAG, "未檢測到痣，直接前往 _bMainActivity");
                            // 沒有痣，直接前往主結果頁面
                            Intent intent = new Intent(PhotoActivity.this, _bMainActivity.class);

                            AnalysisResult parcelableResult = new AnalysisResult(result);
                            intent.putExtra("analysis_result", parcelableResult);
                            intent.putExtra("source_type", "photo");
                            intent.putExtra("original_image_base64", originalImageBase64);
                            intent.putExtra("from_photo", true);
                            intent.putExtra("has_moles", false);  // 修正：改為 has_moles

                            startActivity(intent);
                            finish();
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "分析失敗: " + error);

                        new AlertDialog.Builder(PhotoActivity.this)
                                .setTitle("分析失敗")
                                .setMessage("面部分析失敗：\n" + error + "\n\n請檢查：\n• 網絡連接是否正常\n• 圖片是否清晰\n• 面部是否完整可見")
                                .setPositiveButton("重試", (dialog, which) -> analyzeSelectedImage())
                                .setNegativeButton("取消", (dialog, which) -> {
                                    selectedImageUri = null;
                                    imagePreview.setImageResource(0);
                                    btnPickPhoto.setText("選擇照片");
                                })
                                .show();
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "處理圖片時發生錯誤", e);
            Toast.makeText(this, "處理圖片失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);


            if (originalBitmap == null) {
                return null;
            }

            // 如果圖片太大，進行縮放以提高處理速度
            int maxSize = 1024;
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
                originalBitmap.recycle(); // 釋放原始圖片記憶體
                return scaledBitmap;
            }

            return originalBitmap;

        } catch (IOException e) {
            Log.e(TAG, "轉換URI到Bitmap失敗", e);
            return null;
        }
    }

    // 添加Bitmap轉Base64的方法
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // 壓縮圖片以減少大小，但保持可顯示的質量
            int quality = 80;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);

            return "data:image/jpeg;base64," + base64String;

        } catch (Exception e) {
            Log.e(TAG, "Bitmap轉Base64失敗", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理資源
        if (imagePreview.getDrawable() != null) {
            imagePreview.setImageDrawable(null);
        }
    }
}