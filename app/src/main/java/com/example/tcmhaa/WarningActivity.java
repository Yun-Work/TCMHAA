package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WarningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_n);

        Button btnYes = findViewById(R.id.btn_yes);
        Button btnNo  = findViewById(R.id.btn_no);

        btnYes.setOnClickListener(v -> goNext());
        btnNo.setOnClickListener(v -> goNext());
    }

    private void goNext() {
        String next = getIntent().getStringExtra(nav.EXTRA_NEXT);
        Intent intent;

        if (nav.NEXT_TO_PHOTO.equals(next)) {
            // ➜ 去選相簿頁
            intent = new Intent(this, PhotoActivity.class);

        } else if (nav.NEXT_TO_CAMERA.equals(next)) {
            // ➜ 去拍照頁
            intent = new Intent(this, CameraActivity.class);

        } else {
            // fallback：若沒帶參數，回入口
            intent = new Intent(this, MainhealthyActivity.class);
        }

        startActivity(intent);
        finish(); // 不回到提醒頁
    }
}
