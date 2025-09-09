package com.example.tcmhaa;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tcmhaa.dto.LoginRequestDto;
import com.example.tcmhaa.dto.LoginResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private static final String PREFS = "tcmhaa_prefs";
    private static final String KEY_PERMS_REQUESTED_ONCE = "perms_requested_once"; // ★ 只請求一次

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;
    private TextView tvForgotPassword;

    // 一次請多個權限（系統原生彈窗）
    private final ActivityResultLauncher<String[]> requestPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnNext    = findViewById(R.id.btnNext);
        btnRegister= findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // 登入
        btnNext.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            btnNext.setEnabled(false);

            ApiHelper.httpPost(
                    "users/login",
                    new LoginRequestDto(email, password),
                    LoginResponseDto.class,
                    new ApiHelper.ApiCallback<>() {
                        @Override
                        public void onSuccess(LoginResponseDto resp) {
                            btnNext.setEnabled(true);

                            if (resp == null || !resp.success) {
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (resp.user != null) {
                                getSharedPreferences("auth", MODE_PRIVATE).edit()
                                        .putInt("user_id", resp.user.user_id)
                                        .putString("name", resp.user.name)
                                        .putString("email", resp.user.email)
                                        .apply();
                            }

                            Toast.makeText(LoginActivity.this,
                                    resp.message != null ? resp.message : "登入成功",
                                    Toast.LENGTH_SHORT).show();

                            // 登入成功 → 只在第一次觸發系統原生權限彈窗
                            maybeRequestRuntimePermsThenGo();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            btnNext.setEnabled(true);
                            Log.e(TAG, "login fail: " + t.getMessage(), t);
                            Toast.makeText(LoginActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        // 前往註冊
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, CheckActivity.class);
            i.putExtra("prefill_email", etUsername.getText().toString().trim());
            startActivity(i);
        });

        // 忘記密碼
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgetActivity.class))
        );
    }

    /** 登入後：只在「從未請求過」且「有缺權限」時彈一次系統原生權限；其他情況直接進首頁 */
    private void maybeRequestRuntimePermsThenGo() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean alreadyAskedOnce = sp.getBoolean(KEY_PERMS_REQUESTED_ONCE, false);

        String[] missing = buildMissingPermissions();
        if (!alreadyAskedOnce && missing.length > 0) {
            // 標記成已請求過（不管使用者結果如何，以後都不再彈）
            sp.edit().putBoolean(KEY_PERMS_REQUESTED_ONCE, true).apply();
            requestPermsLauncher.launch(missing);
        } else {
            goWelcome();
        }
    }

    /** 根據版本組合需要的權限，回傳仍缺少的清單 */
    private String[] buildMissingPermissions() {
        List<String> need = new ArrayList<>();
        // 相機
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            need.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                need.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        return need.toArray(new String[0]);
    }

    /** 系統原生權限結果（只做提示，無論同不同意都進首頁；通知與檔案權限後續程式已做保護） */
    private void onPermissionsResult(Map<String, Boolean> result) {
        // 可選：簡單彙整提示
        boolean anyDenied = false;
        for (Boolean granted : result.values()) {
            if (granted == null || !granted) {
                anyDenied = true;
                break;
            }
        }
        if (anyDenied) {
            Toast.makeText(this, "部分權限被拒絕，可於系統設定稍後開啟。", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "權限已授予。", Toast.LENGTH_SHORT).show();
        }
        goWelcome();
    }

    private void goWelcome() {
        Intent i = new Intent(LoginActivity.this, WelcomeActivity.class);
        startActivity(i);
        finish(); // 關閉登入頁，避免返回
    }
}
