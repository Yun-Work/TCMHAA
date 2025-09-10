package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class _dProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_profile);

        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnBack = findViewById(R.id.btnBackProfile);

        // 儲存按鈕
        btnSave.setOnClickListener(v -> {
            // 👉 這裡你可以加上「儲存使用者資料」的邏輯
            Intent intent = new Intent(_dProfileActivity.this, _dMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // 返回按鈕
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(_dProfileActivity.this, _dMainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
