package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class _dMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_d);

        // ====== 功能列點擊（每一項都能跳頁） ======
        findViewById(R.id.row_profile).setOnClickListener(v ->
                startActivity(new Intent(this, _dProfileActivity.class)));

        findViewById(R.id.row_daily_notify).setOnClickListener(v ->
                startActivity(new Intent(this, _dTimeActivity.class)));

        findViewById(R.id.row_popup_notify).setOnClickListener(v ->
                startActivity(new Intent(this, _dNotifySettingsActivity.class)));

        // 修改密碼 → 走你們忘記密碼第 2 步的頁面（activity_forget_1_2 對應的 Activity）
        findViewById(R.id.row_change_password).setOnClickListener(v -> {
            Intent i = new Intent(this, Forget12Activity.class);
            // 若需要可一起帶 email：i.putExtra("email", savedEmail);
            startActivity(i);
        });

        findViewById(R.id.row_privacy).setOnClickListener(v ->
                startActivity(new Intent(this, _dPrivacyActivity.class)));

        findViewById(R.id.row_about).setOnClickListener(v ->
                startActivity(new Intent(this, _dAboutActivity.class)));

        // ====== 登出 ======
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // TODO: 清除登入狀態 / Token
            getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "已登出", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity(); // 關閉返回堆疊
        });

        // ====== 底部導覽 ======
        setupBottomNav();
    }

    private void setupBottomNav() {
        LinearLayout navA = findViewById(R.id.nav_a);
        LinearLayout navB = findViewById(R.id.nav_b);
        LinearLayout navC = findViewById(R.id.nav_c);
        LinearLayout navD = findViewById(R.id.nav_d);

        navA.setOnClickListener(v -> startActivity(new Intent(this, _aMainActivity.class)));
        navB.setOnClickListener(v -> startActivity(new Intent(this, _bMainActivity.class)));
        navC.setOnClickListener(v -> startActivity(new Intent(this, _cMainActivity.class)));
        navD.setOnClickListener(v -> { /* 留在本頁 */ });
    }
}
