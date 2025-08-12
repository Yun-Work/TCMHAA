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
    private AuthApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername  = findViewById(R.id.etUsername);
        etPassword  = findViewById(R.id.etPassword);
        btnNext     = findViewById(R.id.btnNext);
        btnRegister = findViewById(R.id.btnRegister);

        api = ApiClient.get().create(AuthApi.class);

        // 健康檢查（可留著當連線指示燈）
        api.status().enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                Log.d(TAG, "status code=" + r.code());
                toast(r.isSuccessful() ? "後端連線成功 (200)" : "後端錯誤碼: " + r.code());
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
                Log.e(TAG, "status fail: " + t.getMessage(), t);
                toast("連線失敗: " + t.getMessage());
            }
        });

        // 登入
        btnNext.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 本地驗證
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("Email 格式錯誤"); return; }
            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                toast("密碼需包含英文與數字，且至少6位"); return;
            }

            btnNext.setEnabled(false);

            api.login(new LoginRequest(email, password))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                            btnNext.setEnabled(true);

                            // 除錯：看原始回傳
                            String raw = null;
                            try {
                                if (resp.errorBody()!=null) raw = resp.errorBody().string();
                                else if (resp.body()!=null) raw = new com.google.gson.Gson().toJson(resp.body());
                            } catch (Exception ignored) {}
                            Log.d(TAG, "code=" + resp.code() + " raw=" + raw);

                            if (!resp.isSuccessful() || resp.body()==null) {
                                toast("登入失敗（HTTP " + resp.code() + "）" + (raw!=null?(" "+raw):""));
                                return;
                            }

                            LoginResponse body = resp.body();
                            if (body.success) {
                                if (body.user != null) {
                                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                                            .putInt("user_id", body.user.user_id)
                                            .putString("name", body.user.name)
                                            .putString("email", body.user.email)
                                            .apply();
                                }
                                Toast.makeText(LoginActivity.this,
                                        "歡迎 " + (body.user != null ? body.user.name : email),
                                        Toast.LENGTH_SHORT).show();
                                showPermissionDialog(); // 成功才進下一頁
                            } else {
                                toast(body.message != null ? body.message : "登入失敗");
                            }
                        }
                        @Override public void onFailure(Call<LoginResponse> call, Throwable t) {
                            btnNext.setEnabled(true);
                            toast("連線錯誤：" + t.getMessage());
                        }
                    });
        });

        // 跳到註冊畫面（CheckActivity）
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CheckActivity.class))
        );
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private void showPermissionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_permissions_2_2);
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