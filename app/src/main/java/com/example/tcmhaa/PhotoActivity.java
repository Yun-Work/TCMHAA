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
    private int userId = -1;

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

    // Android 13+ 要求 READ_MEDIA_IMAGES 權限
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
        userId = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        initApiService();
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        btnBack = findViewById(R.id.btnBack);

        // 第一次按：選照片；已選照片後按：開始分析
        btnPickPhoto.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                pickImage();
            } else {
                analyzeSelectedImage();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void initApiService() {
        apiService = new ApiService();
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                requestReadImagesPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12- 使用 GetContent 不需要額外權限
            pickImageLauncher.launch("image/*");
        }
    }

    private void showPreview(@NonNull Uri uri) {
        try {
            imagePreview.setImageURI(uri);
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
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("分析中")
                .setMessage("正在進行面部膚色分析，請稍候...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        try {
            // URI → Bitmap（含大小控制）
            Bitmap originalBitmap = uriToBitmap(selectedImageUri);
            if (originalBitmap == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "無法載入圖片，請重新選擇", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保留原圖 Base64，之後顯示用
            String originalImageBase64 = bitmapToBase64(originalBitmap);

            Log.d(TAG, "開始分析圖片，尺寸: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

            // 修正：使用完整的特徵檢測，包含痣和鬍鬚檢測
            int userId = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
            apiService.analyzeFaceWithFeatureRemoval(originalBitmap, false, false, userId,new ApiService.AnalysisCallback() {
                @Override
                public void onSuccess(ApiService.AnalysisResult result) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.d(TAG, "分析成功");

                        // 🔧 修正：檢查是否有痣或鬍鬚 - 加入鬍鬚數量檢查
                        boolean hasMoles = result.hasMoles();
                        boolean hasBeard = result.hasBeard();
                        int beardCount = result.getBeardCount();

                        // 🔥 重要：鬍鬚數量 <= 1 時不視為明顯鬍鬚
                        if (hasBeard && beardCount <= 1) {
                            hasBeard = false;
                            Log.d(TAG, "鬍鬚數量 <= 1，不視為明顯鬍鬚");
                        }

                        Log.d(TAG, "檢測結果 - 痣: " + hasMoles + ", 鬍鬚: " + hasBeard + ", 鬍鬚數量: " + beardCount);

                        if (hasMoles || hasBeard) {
                            Log.d(TAG, "檢測到特徵，前往 WarningActivity");
                            // 有痣或鬍鬚，前往警告頁面
                            Intent intent = new Intent(PhotoActivity.this, WarningActivity.class);

                            AnalysisResult parcelableResult = new AnalysisResult(result);
                            intent.putExtra("analysis_result", parcelableResult);
                            intent.putExtra("source_type", "photo");
                            intent.putExtra("original_image_base64", originalImageBase64);
                            intent.putExtra("from_photo", true);
                            intent.putExtra("has_moles", hasMoles);
                            intent.putExtra("has_beard", hasBeard);
                            intent.putExtra("beard_count", beardCount);

                            startActivity(intent);
                        } else {
                            Log.d(TAG, "未檢測到明顯特徵，直接前往 _bMainActivity");
                            // 沒有痣也沒有鬍鬚，直接前往主結果頁面
                            Intent intent = new Intent(PhotoActivity.this, _bMainActivity.class);

                            AnalysisResult parcelableResult = new AnalysisResult(result);
                            intent.putExtra("analysis_result", parcelableResult);
                            intent.putExtra("source_type", "photo");
                            intent.putExtra("original_image_base64", originalImageBase64);
                            intent.putExtra("from_photo", true);
                            intent.putExtra("has_moles", false);
                            intent.putExtra("has_beard", false);
                            intent.putExtra("beard_count", beardCount);

                            startActivity(intent);
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
                                .setMessage("面部分析失敗：\n" + error + "\n\n請檢查：\n• 網路是否正常\n• 圖片是否清晰\n• 面部是否完整可見")
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
            if (originalBitmap == null) return null;

            // 避免超大圖造成 OOM：縮到不超過 1024
            int maxSize = 1024;
            int w = originalBitmap.getWidth();
            int h = originalBitmap.getHeight();

            if (w > maxSize || h > maxSize) {
                float scale = Math.min((float) maxSize / w, (float) maxSize / h);
                int newW = Math.round(w * scale);
                int newH = Math.round(h * scale);
                Bitmap scaled = Bitmap.createScaledBitmap(originalBitmap, newW, newH, true);
                originalBitmap.recycle();
                return scaled;
            }
            return originalBitmap;

        } catch (IOException e) {
            Log.e(TAG, "URI→Bitmap 失敗", e);
            return null;
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bytes = bos.toByteArray();
            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + b64;
        } catch (Exception e) {
            Log.e(TAG, "Bitmap→Base64 失敗", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imagePreview != null && imagePreview.getDrawable() != null) {
            imagePreview.setImageDrawable(null);
        }
    }
}