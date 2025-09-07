package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.dto.ResetPasswordRequestDto;
import com.example.tcmhaa.dto.ResetPasswordResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class Forget12Activity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnConfirm;

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_1_2);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnConfirm = findViewById(R.id.btnConfirm);
        Intent from = getIntent();
        if (from != null) {
            userId = from.getIntExtra("user_id", -1);
        }
        if (userId <= 0) {
            Toast.makeText(this, "請重新操作", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        btnConfirm.setOnClickListener(v -> handleConfirm());
    }

    private void handleConfirm() {
        String pwd = etNewPassword.getText().toString().trim();
        String pwd2 = etConfirmPassword.getText().toString().trim();

        // 驗證：必填、長度、英文+數字、兩次一致
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
        // 關鍵盤 + 鎖按鈕
        hideKeyboard();
        btnConfirm.setEnabled(false);

        ApiHelper.httpPost(
                "users/reset_password",
                new ResetPasswordRequestDto(userId, pwd),
                ResetPasswordResponseDto.class,
                new ApiHelper.ApiCallback<ResetPasswordResponseDto>() {
                    @Override
                    public void onSuccess(ResetPasswordResponseDto resp) {
                        btnConfirm.setEnabled(true);

                        if (resp != null && resp.getError() != null) {
                            Toast.makeText(getApplicationContext(), resp.getError(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String msg = (resp != null && resp.getMessage() != null)
                                ? resp.getMessage() : "密碼已重設";
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                        // 成功後跳回登入頁
                        Intent intent = new Intent(Forget12Activity.this, LoginActivity.class);
                        // 清掉舊的 activity stack，避免按返回再回到忘記密碼流程
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        btnConfirm.setEnabled(true);
                        String msg = (t != null && t.getMessage() != null) ? t.getMessage() : "連線失敗";
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                }
        );
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


    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v == null) v = new View(this);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}