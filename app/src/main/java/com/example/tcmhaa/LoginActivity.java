package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnNext, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1); // ⚠️ 確保這 layout 名稱是對的

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etBirthday); // ⚠️ 你 layout 中雖然是密碼，但 id 還是叫 etBirthday，未來請改成 etPassword
        btnNext = findViewById(R.id.btnNext);
        btnRegister = findViewById(R.id.btnRegister);

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
            // 顯示彈跳視窗（權限）
            showPermissionDialog();
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CheckActivity.class);
            startActivity(intent);
        });
    }

    private void showPermissionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_permissions_2_2); // ⚠️ 確保這個 layout 存在
        dialog.setCancelable(true);

        Button btnConfirm = dialog.findViewById(R.id.btnConfirm); // ⚠️ layout 要有這個按鈕
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        });

        dialog.show();
    }
}