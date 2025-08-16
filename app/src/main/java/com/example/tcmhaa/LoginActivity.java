package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnNext, btnRegister;
    TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnNext = findViewById(R.id.btnNext);       // 左側按鈕：照你的需求 → CheckActivity
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnNext.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();

            if (username.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*")) {
                Toast.makeText(this, "密碼需包含英文字母與數字", Toast.LENGTH_SHORT).show();
                return;
            }
            // 進到 CheckActivity（註冊/設定 Gmail 與密碼）
            startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
        });

        // 你也可以把註冊鍵保留成一樣是去 CheckActivity，或之後改別的註冊頁
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CheckActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgetActivity.class)));
    }
}
