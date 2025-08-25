package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.dto.LoginRequestDto;
import com.example.tcmhaa.dto.LoginResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;
    private TextView tvForgotPassword;

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

                            if (resp == null) {
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (resp.success) {
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

                                // 登入成功 → 顯示權限 Dialog（全同意後才進首頁）
                                showPermissionDialog();
                            } else {
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                            }
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

    private void showPermissionDialog() {
        PermissionsDialogFragment dlg = new PermissionsDialogFragment();
        dlg.setOnAllGrantedListener(() -> {
            // ✅ 全同意後導頁
            Intent i = new Intent(LoginActivity.this, WelcomeActivity.class);
            startActivity(i);
            finish(); // 關閉登入頁，避免返回
        });
        dlg.show(getSupportFragmentManager(), "perm_dialog");
    }

}
