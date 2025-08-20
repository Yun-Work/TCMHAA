package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.api.ApiClient;
import com.example.tcmhaa.api.AuthApi;
import com.example.tcmhaa.api.dto.LoginRequest;
import com.example.tcmhaa.api.dto.LoginResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;
    private AuthApi api; // Retrofit 介面

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1); // 確保 layout 名稱正確

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnNext    = findViewById(R.id.btnNext);
        btnRegister= findViewById(R.id.btnRegister);

        // 建立 Retrofit
        api = ApiClient.get().create(AuthApi.class);

        // 啟動即測後端健康檢查（/status）
        api.status().enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> r) {
                Log.d(TAG, "status code=" + r.code());
                Toast.makeText(LoginActivity.this,
                        r.isSuccessful() ? "後端連線成功 (200)" : "後端錯誤碼: " + r.code(),
                        Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "status fail: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "連線失敗: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // 登入
        btnNext.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 本地格式驗證
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email 格式錯誤", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                Toast.makeText(this, "密碼需包含英文與數字，且至少6位", Toast.LENGTH_SHORT).show();
                return;
            }

            btnNext.setEnabled(false);

            api.login(new LoginRequest(email, password))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                            btnNext.setEnabled(true);

                            // 除錯：把原始回傳記錄在 Logcat（使用者仍只看到統一訊息）
                            String raw = null;
                            try {
                                if (resp.errorBody() != null) raw = resp.errorBody().string();
                                else if (resp.body() != null)
                                    raw = new com.google.gson.Gson().toJson(resp.body());
                            } catch (Exception ignored) {}
                            Log.d(TAG, "login resp code=" + resp.code() + " raw=" + raw);

                            if (!resp.isSuccessful() || resp.body() == null) {
                                // HTTP 4xx/5xx 或 body 為空 → 統一提示
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                                return;
                            }

                            LoginResponse body = resp.body();
                            if (body.success) {
                                // 成功：可選儲存使用者資訊
                                if (body.user != null) {
                                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                                            .putInt("user_id", body.user.user_id)
                                            .putString("name", body.user.name)
                                            .putString("email", body.user.email)
                                            .apply();
                                }
                                Toast.makeText(LoginActivity.this,
                                        body.message != null ? body.message : "登入成功",
                                        Toast.LENGTH_SHORT).show();
                                showPermissionDialog(); // 只有成功才跳下一步
                            } else {
                                // 失敗：固定顯示
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            btnNext.setEnabled(true);
                            Log.e(TAG, "login fail: " + t.getMessage(), t);
                            Toast.makeText(LoginActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // 跳到註冊畫面（CheckActivity）
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, CheckActivity.class);
            // 可選：把已輸入的 email 傳給註冊頁預填
            i.putExtra("prefill_email", etUsername.getText().toString().trim());
            startActivity(i);
        });
    }

    private void showPermissionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_permissions_2_2); // 確保這個 layout 存在且有 btnConfirm
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