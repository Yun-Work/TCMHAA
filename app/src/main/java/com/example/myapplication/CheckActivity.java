package com.example.myapplication;


import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CheckActivity extends AppCompatActivity {

    private EditText etUsername, etGmail, etPassword, etConfirmPassword;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);

        etUsername = findViewById(R.id.etUsername);
        etGmail = findViewById(R.id.etGmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String gmail = etGmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (username.isEmpty()) {
                showToast("請輸入用戶名");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches() || !gmail.endsWith("@gmail.com")) {
                showToast("請輸入正確的 Gmail");
            } else if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                showToast("密碼需包含英文字母與數字，長度至少6位");
            } else if (!password.equals(confirmPassword)) {
                showToast("密碼與確認密碼不一致");
            } else {
                showToast("註冊成功！");
                // TODO: 可跳轉頁面或清空表單
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(CheckActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
