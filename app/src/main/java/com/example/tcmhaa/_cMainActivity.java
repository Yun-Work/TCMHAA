package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class _cMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        Button btnPickRange = findViewById(R.id.btnPickRange);
        ImageButton btnDownload = findViewById(R.id.btnDownload);

        btnPickRange.setOnClickListener(v ->
                Toast.makeText(this, "開日期區間選擇器", Toast.LENGTH_SHORT).show());

        btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "這裡執行匯出/下載", Toast.LENGTH_SHORT).show());

        setupBottomNav();
    }

    private void setupBottomNav() {
        LinearLayout navA = findViewById(R.id.nav_a);
        LinearLayout navB = findViewById(R.id.nav_b);
        LinearLayout navC = findViewById(R.id.nav_c);
        LinearLayout navD = findViewById(R.id.nav_d);

        navA.setOnClickListener(v -> startActivity(new Intent(this, _aMainActivity.class)));
        navB.setOnClickListener(v -> startActivity(new Intent(this, _bMainActivity.class)));
        navC.setOnClickListener(v -> { /* 留在本頁 */ });
        navD.setOnClickListener(v -> startActivity(new Intent(this, _dMainActivity.class)));
    }
}
