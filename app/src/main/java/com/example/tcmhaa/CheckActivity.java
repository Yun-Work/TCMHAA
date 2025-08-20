package com.example.tcmhaa;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.dto.RegisterRequestDto;
import com.example.tcmhaa.dto.RegisterResponseDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.OkHttpClient;

public class CheckActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword,etUsername, etBirthday;
    private RadioGroup rgGender;
    private Button btnSubmit;
    //private AuthApi api;
    private final SimpleDateFormat ymd = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_2_1);

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etUsername        = findViewById(R.id.etUsername);
        etBirthday        = findViewById(R.id.etBirthday);
        rgGender          = findViewById(R.id.rgGender);
        btnSubmit         = findViewById(R.id.btnSubmit);

        //api = ApiClient.get().create(AuthApi.class);
        // 生日 DatePicker
        etBirthday.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(view -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm  = etConfirmPassword.getText().toString();
            String name     = etUsername.getText().toString().trim();
            String birthStr = etBirthday.getText().toString().trim();

            // 性別：把「男 / 女」映射為後端要的「男生 / 女生」
            int checkedId = rgGender.getCheckedRadioButtonId();
            String gender = null;
            if (checkedId == R.id.rbMale)   gender = "男生";
            if (checkedId == R.id.rbFemale) gender = "女生";

            // 前端驗證（與後端對齊）
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            { toast("請輸入正確的 Email");
            } else if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                toast("密碼需包含英文字母與數字，長度至少6位");
            } else if (!password.equals(confirm)) { toast("密碼與確認密碼不一致");
            } else if (name.isEmpty()) { toast("請輸入用戶名");
            } else if (gender == null) { toast("請選擇性別");
            } else if (!isValidDate(birthStr)) { toast("生日格式需為 YYYY/MM/DD");
            }else{
                loginToServer(email, password, name, gender, birthStr);
            }

        });
    }

    private void loginToServer(String email,String password,String name,String gender,String birthStr) {
        OkHttpClient client = new OkHttpClient();

        btnSubmit.setEnabled(false);

        ApiHelper.httpPost(
                "users/register",                                // 依你的後端路徑調整
                new RegisterRequestDto(email, password, name, gender, birthStr),
                RegisterResponseDto.class,
                new ApiHelper.ApiCallback<>() {
                    @Override
                    public void onSuccess(RegisterResponseDto resp) {
                        btnSubmit.setEnabled(true);

                        // 成功條件：success=true，或沒有 error 且有 userId（相容不同後端實作）


                        if (resp != null) {
                            if (resp.success) {
                                showSuccessDialog();
                            } else {
                                // 後端邏輯失敗（例如 Email 已被註冊）
                                String msg = (resp.message != null && !resp.message.isEmpty()) ? resp.message : "註冊失敗";
                                toast(msg);
                                if ("Email 已被註冊".equals(resp.message) || "EMAIL_TAKEN".equals(resp.code)) {
                                    etEmail.setError("該 Email 已註冊過");
                                    etEmail.requestFocus();
                                }
                            }
                        } else {

                            toast(resp.message);
                        }
                    }

                        @Override
                        public void onFailure(Throwable t) {
                            btnSubmit.setEnabled(true);
                            toast("連線錯誤：" + t.getMessage());

                        }
                    }
            );
    }private boolean isValidDate(String s) {
        try {
            ymd.setLenient(false);
            ymd.parse(s);                 // 只能接受 yyyy/MM/dd
            return s.matches("^\\d{4}/\\d{2}/\\d{2}$");
        } catch (ParseException e) {
            return false;
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, day) -> {
            String mm = String.format(Locale.TAIWAN, "%02d", month + 1);
            String dd = String.format(Locale.TAIWAN, "%02d", day);
            etBirthday.setText(year + "/" + mm + "/" + dd); // 後端要單斜線：YYYY/MM/DD
        }, y, m, d);
        dlg.show();
    }

    private void showSuccessDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("註冊成功")
                .setMessage("已成功註冊，請重新登入。")
                .setCancelable(false)
                .setPositiveButton("確定", (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // 回登入頁
                })
                .show();
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}

