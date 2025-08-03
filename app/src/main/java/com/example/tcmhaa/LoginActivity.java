package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etBirthday;
    Button btnNext, btnRegister, btnConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername = findViewById(R.id.etUsername);
        etBirthday = findViewById(R.id.etBirthday);
        btnNext = findViewById(R.id.btnNext);
        btnRegister = findViewById(R.id.btnRegister);
//        btnConfirmPassword = findViewById(R.id.btnConfirmPassword);

        // 點選生日欄位出現日期選擇器
        etBirthday.setOnClickListener(v -> showDatePicker());

        // 按下確認按鈕，檢查密碼格式
        btnConfirmPassword.setOnClickListener(v -> {
            String password = etBirthday.getText().toString();
            if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
                Toast.makeText(this, "密碼需包含英文字母與數字", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "密碼格式正確", Toast.LENGTH_SHORT).show();
            }
        });

        // 登入按鈕 -> 檢查欄位是否填寫，然後跳轉
        btnNext.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String birthday = etBirthday.getText().toString();

            if (username.isEmpty() || birthday.isEmpty()) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "歡迎 " + username, Toast.LENGTH_SHORT).show();
                // ✅ 跳轉到通知設定 MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btnRegister.setOnClickListener(v -> {
            // ✅ 跳轉到註冊輸入畫面 CheckActivity
            Intent intent = new Intent(LoginActivity.this, CheckActivity.class);
            startActivity(intent);
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR), month = calendar.get(Calendar.MONTH), day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) ->
                etBirthday.setText(y + "-" + (m + 1) + "-" + d),
                year, month, day).show();
    }
}
