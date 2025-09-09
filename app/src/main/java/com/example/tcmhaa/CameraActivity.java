package com.example.tcmhaa;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    private PreviewView previewView;
    private FaceOverlayView faceOverlayView;
    private ImageButton captureButton;
    private ImageButton backButton;

    private ImageCapture imageCapture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ApiService apiService;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private Uri photoUri;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_5_1);
        userId = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        // 螢幕常亮與高亮度
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(params);

        initViews();
        initApiService();
        setupPermissionLauncher();
        checkAndRequestPermissions();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        faceOverlayView = findViewById(R.id.faceOverlayView);
        captureButton = findViewById(R.id.captureButton);
        backButton = findViewById(R.id.backButton);

        captureButton.setOnClickListener(v -> takePicture());
        backButton.setOnClickListener(v -> finish());
    }

    private void initApiService() {
        apiService = new ApiService();
    }

    private void setupPermissionLauncher() {
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean camOk = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                    boolean readOk = true; // 預設 true，根據版本再檢查

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        readOk = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                    } else {
                        readOk = Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE))
                                || ContextCompat.checkSelfPermission(
                                this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                    }

                    if (camOk) {
                        startCamera();
                    } else {
                        Toast.makeText(this, "需要相機權限才能拍照", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    // 相簿權限主要給挑圖頁用（PhotoActivity），這裡先提示
                    if (!readOk) {
                        Toast.makeText(this, "未授權讀取相簿，稍後選圖功能可能受限", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkAndRequestPermissions() {
        boolean camGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        String readPermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        boolean readGranted = ContextCompat.checkSelfPermission(this, readPermission)
                == PackageManager.PERMISSION_GRANTED;

        if (camGranted) {
            startCamera();
        } else {
            // 一次請兩個（相簿可選擇性）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                });
            } else {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                });
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相機初始化失敗", e);
                Toast.makeText(this, "相機初始化失敗", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "用例綁定失敗", e);
                Toast.makeText(this, "相機初始化失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        if (imageCapture == null) {
            Toast.makeText(this, "相機未初始化，請稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        // 顯示進度對話框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("處理中")
                .setMessage("正在拍攝並進行面部分析，請稍候...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", photoFile);

            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            Log.d(TAG, "照片已保存: " + photoFile.getAbsolutePath());

                            // 讀取拍攝的照片並轉換為Bitmap
                            try {
                                Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                                if (originalBitmap == null) {
                                    progressDialog.dismiss();
                                    Toast.makeText(CameraActivity.this, "讀取拍攝照片失敗", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // 如果圖片太大，進行縮放
                                Bitmap processedBitmap = scaleBitmapIfNeeded(originalBitmap);

                                // 🔧 關鍵：保存原始圖片的Base64數據（用於顯示）
                                String originalImageBase64 = bitmapToBase64(originalBitmap);

                                Log.d(TAG, "開始分析拍攝的照片，尺寸: " + processedBitmap.getWidth() + "x" + processedBitmap.getHeight());

                                // 調用API分析（使用處理後的圖片以提高速度）
                                apiService.analyzeFace(processedBitmap,new ApiService.AnalysisCallback() {
                                    @Override
                                    public void onSuccess(ApiService.AnalysisResult result) {
                                        runOnUiThread(() -> {
                                            progressDialog.dismiss();
                                            Log.d(TAG, "拍攝照片分析成功");

                                            // 👉 先跳 WarningActivity，而不是直接去 _bMainActivity
                                            Intent intent = new Intent(CameraActivity.this, WarningActivity.class);

                                            // 將分析結果資料也帶過去，交給 WarningActivity 再傳到 _bMainActivity
                                            AnalysisResult parcelableResult = new AnalysisResult(result);
                                            intent.putExtra("analysis_result", parcelableResult);
                                            intent.putExtra("source_type", "camera");
                                            intent.putExtra("original_image_base64", originalImageBase64);

                                            startActivity(intent);
                                            finish();
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        runOnUiThread(() -> {
                                            progressDialog.dismiss();
                                            Log.e(TAG, "拍攝照片分析失敗: " + error);

                                            new AlertDialog.Builder(CameraActivity.this)
                                                    .setTitle("分析失敗")
                                                    .setMessage("面部分析失敗：\n" + error + "\n\n請檢查：\n• 網絡連接是否正常\n• 光線是否充足\n• 面部是否完整對準框線")
                                                    .setPositiveButton("重新拍攝", (dialog, which) -> {
                                                        // 用戶可以重新拍攝
                                                    })
                                                    .setNegativeButton("返回", (dialog, which) -> finish())
                                                    .show();
                                        });
                                    }
                                });

                            } catch (Exception e) {
                                progressDialog.dismiss();
                                Log.e(TAG, "處理拍攝照片時發生錯誤", e);
                                Toast.makeText(CameraActivity.this, "處理照片失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(ImageCaptureException exception) {
                            progressDialog.dismiss();
                            Log.e(TAG, "拍照失敗", exception);
                            Toast.makeText(CameraActivity.this,
                                    "拍照失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            logDetailedInfo("拍照失敗", exception);
                        }
                    });
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "拍照過程中出錯", e);
            Toast.makeText(this, "拍照過程中出錯: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            logDetailedInfo("拍照過程中出錯", e);
        }
    }

    private Bitmap scaleBitmapIfNeeded(Bitmap bitmap) {
        int maxSize = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxSize || height > maxSize) {
            float scale = Math.min((float) maxSize / width, (float) maxSize / height);
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            bitmap.recycle(); // 釋放原始圖片記憶體
            return scaledBitmap;
        }

        return bitmap;
    }

    // 添加Bitmap轉Base64的方法
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // 為顯示用途保持較好的質量
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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) storageDir = getFilesDir();
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("無法創建存儲目錄");
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void logDetailedInfo(String message, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");
        if (e != null) {
            sb.append("錯誤類型: ").append(e.getClass().getName()).append("\n");
            sb.append("錯誤信息: ").append(e.getMessage()).append("\n");
        }
        Log.e(TAG, sb.toString());
        try {
            File logFile = new File(getExternalFilesDir(null), "camera_error_log.txt");
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date()))
                        .append(" - ")
                        .append(sb.toString())
                        .append("\n\n");
            }
        } catch (IOException ioe) {
            Log.e(TAG, "無法寫入日誌文件", ioe);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraProvider != null && imageCapture == null) startCamera();
    }
}