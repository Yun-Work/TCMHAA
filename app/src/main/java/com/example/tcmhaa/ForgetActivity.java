package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.dto.SendCodeRequestDto;
import com.example.tcmhaa.dto.SendCodeResponseDto;
import com.example.tcmhaa.utils.toast.ToastHelper;

import java.util.Objects;

public class ForgetActivity extends AppCompatActivity {

    private EditText editTextGmail;
    private Button buttonSendCode;
    private CountDownTimer timer;     // 防止連發的 60 秒倒數

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_1_1);

        editTextGmail = findViewById(R.id.editTextGmail);
        buttonSendCode = findViewById(R.id.buttonSendCode);

        buttonSendCode.setOnClickListener(v -> {
            String email = editTextGmail.getText().toString().trim();

            // 用內建 email regex 驗證
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(getApplicationContext(), "請輸入正確的 Email", Toast.LENGTH_SHORT);
//                toast("請輸入正確的 Email");
                return;
            }

            buttonSendCode.setEnabled(false);

            // 忘記密碼流程：status = "2"；若是註冊頁，改成 "1"
            ApiHelper.httpPost(
                    "users/send_code",
                    new SendCodeRequestDto(email, "2"),
                    SendCodeResponseDto.class,
                    new ApiHelper.ApiCallback<SendCodeResponseDto>() {
                        @Override
                        public void onSuccess(SendCodeResponseDto resp) {
                            // 成功：提示 + 倒數 + 導頁
                            startCountdown();

                            Integer userId = resp.getUserId();
                            String msg = (resp != null && resp.getMessage() != null)
                                    ? resp.getMessage() : "驗證碼已寄出";
                            ToastHelper.show(getApplicationContext(), msg, Toast.LENGTH_SHORT);
                            Intent intent = new Intent(ForgetActivity.this, VerifyActivity.class);
                            intent.putExtra("status", "2");
                            intent.putExtra("user_id", userId);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            buttonSendCode.setEnabled(true);
                            ToastHelper.show(getApplicationContext(), Objects.requireNonNull(t.getMessage()), Toast.LENGTH_SHORT);
                        }
                    }
            );
        });
    }

    // 60 秒倒數，避免重複寄送
    private void startCountdown() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(60_000, 1_000) {
            @Override public void onTick(long ms) {
                buttonSendCode.setText("已送出（" + (ms / 1000) + "s）");
            }
            @Override public void onFinish() {
                buttonSendCode.setText("寄送驗證碼");
                buttonSendCode.setEnabled(true);
            }
        }.start();
    }

//    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}