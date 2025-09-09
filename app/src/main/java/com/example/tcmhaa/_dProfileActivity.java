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

        btnSave.setOnClickListener(v -> {
            // 👉 這裡你可以加上「儲存使用者資料」的邏輯
            // 例如寫入 SharedPreferences / 資料庫 / API 呼叫
            // 儲存完成後跳轉回 _dMainActivity

            Intent intent = new Intent(_dProfileActivity.this, _dMainActivity.class);
            // 如果不想讓使用者按返回再回到 Profile 頁，可以加上 FLAG
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 關閉當前 Activity
        });
    }
}
