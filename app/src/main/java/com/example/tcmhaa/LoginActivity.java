package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.dto.LoginRequestDto;
import com.example.tcmhaa.dto.LoginResponseDto;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1); // 確保 layout 名稱正確

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

            // 呼叫共用 API（POST）
            ApiHelper.httpPost(
                    "users/login",                         // API路徑
                    new LoginRequestDto(email, password),         // LoginRequestDto
                    LoginResponseDto.class,                       // LoginResponseDTO
                    new ApiHelper.ApiCallback<>() {
                        @Override
                        public void onSuccess(LoginResponseDto resp) {
                            btnNext.setEnabled(true);

                            // 除錯：把原始回傳記錄在 Logcat（使用者仍只看到統一訊息）
                            String raw = null;
//                            try {
//                                if (resp != null && resp.isSuccess()) raw = resp.errorBody().string();
//                                else if (resp.body() != null)
//                                    raw = new com.google.gson.Gson().toJson(resp.body());
//                            } catch (Exception ignored) {}
//                            Log.d(TAG, "login resp code=" + resp.code() + " raw=" + raw);

                            if (!resp.isSuccess() || resp == null) {
                                // HTTP 4xx/5xx 或 body 為空 → 統一提示
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                                return;
                            }

//                            LoginResponse body = resp.body();
                            if (resp.success) {
                                // 成功：可選儲存使用者資訊
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
                                showPermissionDialog(); // 只有成功才跳下一步
                            } else {
                                // 失敗：固定顯示
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
            // 可選：把已輸入的 email 傳給註冊頁預填
            i.putExtra("prefill_email", etUsername.getText().toString().trim());
            startActivity(i);
        });

        // 忘記密碼
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgetActivity.class))
        );
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void showPermissionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_permissions_2_2); // 確保這個 layout 存在且有 btnConfirm
        dialog.setCancelable(true);

        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        dialog.show();
    }
}