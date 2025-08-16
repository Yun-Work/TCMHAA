package com.example.tcmhaa;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_5_1);

        // 螢幕常亮與高亮度
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(params);

        initViews();
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
                            // ✅ 拍照成功 → 先跳 WarningActivity
                            Intent intent = new Intent(CameraActivity.this, WarningActivity.class);
                            intent.setData(photoUri); // 或者用 putExtra("photoUri", photoUri.toString())
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(ImageCaptureException exception) {
                            Log.e(TAG, "拍照失敗", exception);
                            Toast.makeText(CameraActivity.this,
                                    "拍照失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            logDetailedInfo("拍照失敗", exception);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "拍照過程中出錯", e);
            Toast.makeText(this, "拍照過程中出錯: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            logDetailedInfo("拍照過程中出錯", e);
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
