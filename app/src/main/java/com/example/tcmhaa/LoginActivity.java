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
        if (findViewById(R.id.btnNext) == null) {
            throw new IllegalStateException("btnNext not found in activity_login_1 layout (check layout variants and ids).");
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword); // 請記得之後換成 etPassword
        btnNext = findViewById(R.id.btnNext);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnNext.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
                Toast.makeText(this, "密碼需包含英文字母與數字", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "歡迎 " + username, Toast.LENGTH_SHORT).show();
            showPermissionDialog();
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CheckActivity.class);
            startActivity(intent);
        });

        // ➕ 忘記密碼導向
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgetActivity.class);
            startActivity(intent);
        });
    }

    private void showPermissionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_permissions_2_2);
        dialog.setCancelable(true);

        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        });

        dialog.show();
    }
}
