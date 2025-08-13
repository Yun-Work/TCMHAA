package com.example.tcmhaa;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.api.ApiClient;
import com.example.tcmhaa.api.AuthApi;
import com.example.tcmhaa.api.dto.RegisterRequest;
import com.example.tcmhaa.api.dto.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckActivity extends AppCompatActivity {

    private EditText etUsername, etGmail, etPassword, etConfirmPassword;
    private Button btnSubmit;
    private AuthApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_2_1);

        etUsername        = findViewById(R.id.etUsername);
        etGmail           = findViewById(R.id.etGmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit         = findViewById(R.id.btnSubmit);

        api = ApiClient.get().create(AuthApi.class);

        btnSubmit.setOnClickListener(view -> {
            String name     = etUsername.getText().toString().trim();
            String email    = etGmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm  = etConfirmPassword.getText().toString();

            // 前端驗證（與後端對齊）
            if (name.isEmpty()) { toast("請輸入用戶名"); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("請輸入正確的 Email"); return; }
            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                toast("密碼需包含英文字母與數字，長度至少6位"); return;
            }
            if (!password.equals(confirm)) { toast("密碼與確認密碼不一致"); return; }

            btnSubmit.setEnabled(false);

            api.register(new RegisterRequest(name, email, password))
                    .enqueue(new Callback<RegisterResponse>() {
                        @Override public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> resp) {
                            btnSubmit.setEnabled(true);

                            if (resp.isSuccessful() && resp.body()!=null && resp.body().error == null) {
                                // ✅ 註冊成功：把帳密暫存，回到登入頁自動帶入（僅開發測試用）
                                getSharedPreferences("tmp", MODE_PRIVATE).edit()
                                        .putString("email", email.trim().toLowerCase())
                                        .putString("password", password)
                                        .apply();
                                toast("註冊成功，請登入");
                                finish(); // 返回 LoginActivity
                            } else {
                                String msg = "註冊失敗（HTTP " + resp.code() + "）";
                                if (resp.body()!=null && resp.body().error != null) msg = resp.body().error;
                                toast(msg);
                            }
                        }

                        @Override public void onFailure(Call<RegisterResponse> call, Throwable t) {
                            btnSubmit.setEnabled(true);
                            toast("連線錯誤：" + t.getMessage());
                        }
                    });
        });
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
