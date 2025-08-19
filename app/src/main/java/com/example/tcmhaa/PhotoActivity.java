package com.example.tcmhaa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class PhotoActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private Button btnPickPhoto, btnBack;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
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
                    Toast.makeText(this, "未取得圖片讀取權限", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_5_1);

        imagePreview = findViewById(R.id.imagePreview);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        btnBack = findViewById(R.id.btnBack);

        btnPickPhoto.setOnClickListener(v -> pickImage());
        btnBack.setOnClickListener(v -> finish());
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
        imagePreview.setImageURI(uri);
    }
}
