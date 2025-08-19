package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.api.ApiHelper;
import com.example.tcmhaa.dto.RegisterRequestDto;
import com.example.tcmhaa.dto.RegisterResponseDto;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckActivity extends AppCompatActivity {

    private EditText etGmail, etPassword, etConfirmPassword;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_2_1);

        etGmail           = findViewById(R.id.etGmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit         = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(view -> {
            String email    = etGmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();


//            // 前端驗證（與後端規則對齊）
//            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("請輸入正確的 Email"); return; }
//            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
//                toast("密碼需包含英文字母與數字，長度至少 6 位"); return;
//            }
//            if (!password.equals(confirmPassword)) { toast("密碼與確認密碼不一致"); return; }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("請輸入正確的 Gmail");
            } else if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                showToast("密碼需包含英文字母與數字，長度至少6位");
            } else if (!password.equals(confirmPassword)) {
                showToast("密碼與確認密碼不一致");
            } else {
                loginToServer(email, password);
            }
        });
    }


    private void showToast(String message) {
        Toast.makeText(CheckActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void loginToServer(String email, String password) {
        OkHttpClient client = new OkHttpClient();

            btnSubmit.setEnabled(false);

            ApiHelper.httpPost(
                    "users/register",                                // 依你的後端路徑調整
                    new RegisterRequestDto(email.toLowerCase(), password),
                    RegisterResponseDto.class,
                    new ApiHelper.ApiCallback<>() {
                        @Override
                        public void onSuccess(RegisterResponseDto resp) {
                            btnSubmit.setEnabled(true);

                            // 成功條件：success=true，或沒有 error 且有 userId（相容不同後端實作）
                            boolean ok = resp != null &&
                                    (resp.isSuccess() || (resp.getUserId() != null && resp.getError() == null));

                            if (ok) {
                                // 便於回登入頁自動帶入
                                getSharedPreferences("tmp", MODE_PRIVATE).edit()
                                        .putString("email", email.toLowerCase())
                                        .putString("password", password)
                                        .apply();

                                toast("註冊成功，請登入");
                                finish(); // 回到 LoginActivity
                            } else {
                                String msg = (resp != null && resp.getError() != null)
                                        ? resp.getError()
                                        : "註冊失敗";
                                toast(msg);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            btnSubmit.setEnabled(true);
                            toast("連線錯誤：" + (t != null ? t.getMessage() : "未知錯誤"));
                        }
                    }
            );
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
