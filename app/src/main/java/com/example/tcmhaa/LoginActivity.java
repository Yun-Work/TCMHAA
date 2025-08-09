package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.api.ApiClient;
import com.example.tcmhaa.api.AuthApi;
import com.example.tcmhaa.api.dto.LoginRequest;
import com.example.tcmhaa.api.dto.LoginResponse;
import com.example.tcmhaa.api.dto.RegisterRequest;
import com.example.tcmhaa.api.dto.RegisterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnNext, btnRegister;

    private AuthApi api; // Retrofit 介面

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1); // ⚠️ 確保這 layout 名稱是對的

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etBirthday); // ⚠️ 你 layout 中雖然是密碼，但 id 還是叫 etBirthday，未來請改成 etPassword
        btnNext = findViewById(R.id.btnNext);
        btnRegister = findViewById(R.id.btnRegister);

        // 建立 Retrofit
        api = ApiClient.get().create(AuthApi.class);

        // app 啟動即測後端健康檢查（/status）
        api.status().enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "後端連線成功 (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "後端錯誤碼: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "連線失敗: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // 登入（呼叫 /api/users/login）
        btnNext.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }
            // 與後端一致的前端驗證（可選）
            boolean emailOk = email.matches("^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$");
            boolean pwdOk = password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$");
            if (!emailOk) { Toast.makeText(this, "Email 格式錯誤", Toast.LENGTH_SHORT).show(); return; }
            if (!pwdOk)   { Toast.makeText(this, "密碼需包含英文與數字，且至少6位", Toast.LENGTH_SHORT).show(); return; }

            api.login(new LoginRequest(email, password))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                            if (resp.isSuccessful() && resp.body()!=null) {
                                LoginResponse body = resp.body();
                                if (body.success) {
                                    // 存使用者資訊（必要時）
                                    if (body.user != null) {
                                        getSharedPreferences("auth", MODE_PRIVATE).edit()
                                                .putInt("user_id", body.user.user_id)
                                                .putString("name", body.user.name)
                                                .putString("email", body.user.email)
                                                .apply();
                                    }
                                    Toast.makeText(LoginActivity.this, body.message, Toast.LENGTH_SHORT).show();
                                    showPermissionDialog(); // 你的既有流程：彈窗 → 進主畫面
                                } else {
                                    Toast.makeText(LoginActivity.this, body.message, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "登入失敗（HTTP " + resp.code() + "）", Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });


           Toast.makeText(this, "歡迎 " + etUsername, Toast.LENGTH_SHORT).show();
           // 顯示彈跳視窗（權限）
            showPermissionDialog();
        });

        //註冊：/api/users/register
        btnRegister.setOnClickListener(v -> {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = email; // 如果日後有姓名輸入框，改用該欄位

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "請輸入 email 與密碼", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean okLen = password.length() >= 6;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit  = password.matches(".*\\d.*");
        if (!okLen || !hasLetter || !hasDigit) {
            Toast.makeText(this, "密碼格式錯誤：至少6碼，含英文與數字", Toast.LENGTH_SHORT).show();
            return;
        }

        api.register(new RegisterRequest(name, email, password))
                .enqueue(new Callback<RegisterResponse>() {
                    @Override public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> resp) {
                        if (resp.isSuccessful() && resp.body()!=null && resp.body().error == null) {
                            Integer uid = resp.body().user_id;
                            Toast.makeText(LoginActivity.this, "註冊成功，ID=" + uid, Toast.LENGTH_SHORT).show();
                            // 註冊成功後可導回登入或直接登入；此處保留原流程
                        } else {
                            String err = "註冊失敗";
                            if (resp.body()!=null && resp.body().error != null) {
                                err += "：" + resp.body().error;
                            } else {
                                err += "（HTTP " + resp.code() + "）";
                            }
                            Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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