package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class _dAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_aboutus);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 顯示版本號
        TextView tvVersion = findViewById(R.id.tvVersion);
        String versionName;
        try {
            versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "-";
        }
        tvVersion.setText(getString(R.string.about_version, versionName));

        // 返回按鈕
        Button btnBack = findViewById(R.id.btnBackAbout);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(_dAboutActivity.this, _dMainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
