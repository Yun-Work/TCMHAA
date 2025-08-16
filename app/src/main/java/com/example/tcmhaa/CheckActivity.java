package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_2_1);

        etGmail = findViewById(R.id.etGmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(view -> {
            String gmail = etGmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches() || !gmail.endsWith("@gmail.com")) {
                showToast("請輸入正確的 Gmail");
            } else if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                showToast("密碼需包含英文字母與數字，長度至少6位");
            } else if (!password.equals(confirmPassword)) {
                showToast("密碼與確認密碼不一致");
            } else {
                loginToServer(gmail, password);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(CheckActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void loginToServer(String email, String password) {
        OkHttpClient client = new OkHttpClient();

        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("http://10.0.2.2:6060/api/users/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("無法連線：" + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resStr = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            showToast("伺服器錯誤：" + response.code());
                            return;
                        }
                        JSONObject obj = new JSONObject(resStr);
                        boolean success = obj.optBoolean("success", false);
                        String message = obj.optString("message", "");

                        if (success) {
                            showToast("登入成功！");
                            boolean first = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .getBoolean("first_login", true);
                            Intent intent = new Intent(
                                    CheckActivity.this,
                                    first ? ProfileActivity.class : WelcomeActivity.class
                            );
                            startActivity(intent);
                        } else {
                            showToast(message.isEmpty() ? "登入失敗" : message);
                        }
                    } catch (Exception e) {
                        showToast("解析回應錯誤：" + e.getMessage());
                    }
                });
            }
        });
    }
}
