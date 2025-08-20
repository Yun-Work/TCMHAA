package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.dto.LoginRequestDto;
import com.example.tcmhaa.dto.LoginResponseDto;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnNext = findViewById(R.id.btnNext);       // 左側按鈕：照你的需求 → CheckActivity
        btnRegister = findViewById(R.id.btnRegister);
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

                            if (resp != null && resp.isSuccess()) {
                                // 需要的話把使用者資訊存起來
                                if (resp.getUser() != null) {
                                    getSharedPreferences("auth", MODE_PRIVATE)
                                            .edit()
                                            .putInt("user_id",  resp.getUser().getUserId())
                                            .putString("name",  resp.getUser().getName())
                                            .putString("email", resp.getUser().getEmail())

                                            .apply();
                                }
                                toast("歡迎 " + (resp.getUser() != null ? resp.getUser().getName() : email));
                                showPermissionDialog();
                            } else {
                                String msg = (resp != null && resp.getMessage() != null)
                                        ? resp.getMessage() : "帳號或密碼錯誤";
                                toast(msg);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            btnNext.setEnabled(true);
                            toast("連線錯誤：" + (t != null ? t.getMessage() : "未知錯誤"));
                        }
                    }
            );
        });

        // 前往註冊
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CheckActivity.class))
        );

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
        dialog.setContentView(R.layout.dialog_permissions_2_2);
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
