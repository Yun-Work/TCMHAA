package com.example.tcmhaa;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Forget12Activity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_1_2);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> handleConfirm());
    }

    private void handleConfirm() {
        String pwd = etNewPassword.getText().toString().trim();
        String pwd2 = etConfirmPassword.getText().toString().trim();

        // 簡單驗證：必填、長度、英文+數字、兩次一致
        if (TextUtils.isEmpty(pwd)) {
            etNewPassword.setError("請輸入新密碼");
            etNewPassword.requestFocus();
            return;
        }
        if (!isValidPassword(pwd)) {
            etNewPassword.setError("至少6碼，需包含英文與數字");
            etNewPassword.requestFocus();
            return;
        }
        if (!pwd.equals(pwd2)) {
            etConfirmPassword.setError("兩次密碼不一致");
            etConfirmPassword.requestFocus();
            return;
        }

        // TODO: 在這裡呼叫後端 API 送出重設密碼請求
        // 成功後可結束或跳轉
        Toast.makeText(this, "密碼已更新（示意）", Toast.LENGTH_SHORT).show();
        // finish();
    }

    // 至少6碼，含英數
    private boolean isValidPassword(String pwd) {
        if (pwd.length() < 6) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }
}
