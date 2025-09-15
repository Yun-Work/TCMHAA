package com.example.tcmhaa;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.dto.RegisterRequestDto;
import com.example.tcmhaa.dto.RegisterResponseDto;
import com.example.tcmhaa.dto.SendCodeRequestDto;
import com.example.tcmhaa.dto.SendCodeResponseDto;
import com.example.tcmhaa.dto.VerifyCodeRequestDto;
import com.example.tcmhaa.dto.VerifyCodeResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.utils.toast.ToastHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class CheckActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etUsername, etBirthday,etVerifyCode;
    private RadioGroup rgGender;
    private Button btnSubmit,btnVerifyCode;

    private final SimpleDateFormat ymd = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_2_1);

        etEmail           = findViewById(R.id.etEmail);
        etVerifyCode         = findViewById(R.id.etVerifyCode);
        btnVerifyCode         = findViewById(R.id.btnVerifyCode);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etUsername        = findViewById(R.id.etUsername);
        etBirthday        = findViewById(R.id.etBirthday);
        rgGender          = findViewById(R.id.rgGender);
        btnSubmit         = findViewById(R.id.btnSubmit);

        // 生日 DatePicker
        etBirthday.setOnClickListener(v -> showDatePicker());

        btnVerifyCode.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("請先輸入正確 Email");
                etEmail.requestFocus();
                return;
            }
            btnVerifyCode.setEnabled(false);
            ApiHelper.httpPost(
                    "users/send_code",
                    new SendCodeRequestDto(email, "1"),
                    SendCodeResponseDto.class,
                    new ApiHelper.ApiCallback<SendCodeResponseDto>() {
                        @Override public void onSuccess(SendCodeResponseDto resp) {
                            String msg = (resp!=null && resp.getMessage()!=null) ? resp.getMessage() : "驗證碼已寄出";
                            toast(msg);
                            startCountdown(btnVerifyCode, 60); // 60秒倒數
                        }
                        @Override public void onFailure(Throwable t) {
                            btnVerifyCode.setEnabled(true);
                            toast(t!=null && t.getMessage()!=null ? t.getMessage() : "重送失敗");
                        }
                    }
            );
        });

        btnSubmit.setOnClickListener(view -> {
            String email = safeTrim(etEmail.getText());
            String code     = safeTrim(etVerifyCode.getText());
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
            String name = safeTrim(etUsername.getText());
            String birthStr = safeTrim(etBirthday.getText());

            // 性別：把「男 / 女」映射為後端要的「男生 / 女生」（請依你後端實際值調整）
            int checkedId = rgGender.getCheckedRadioButtonId();
            String gender = checkedId == R.id.rbMale ? "男生" : checkedId == R.id.rbFemale ? "女生" : null;
//            if (checkedId == R.id.rbMale) gender = "男生";
//            if (checkedId == R.id.rbFemale) gender = "女生";

            // 前端驗證（與後端對齊）
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("請輸入正確的 Email");return;
            }else if (!code.matches("\\d{6}")) {
                toast("請輸入 6 位數驗證碼"); etVerifyCode.requestFocus();return;
            }else if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
                toast("密碼需包含英文字母與數字，長度至少6位");return;
            } else if (!password.equals(confirm)) {
                toast("密碼與確認密碼不一致");return;
            } else if (name.isEmpty()) {
                toast("請輸入用戶名");return;
            } else if (gender == null) {
                toast("請選擇性別");return;
            } else if (!isValidDate(birthStr)) {
                toast("生日格式需為 YYYY/MM/DD");return;
            }
            btnSubmit.setEnabled(false);
            // 1) 驗證驗證碼（email + code + status="1"）
            ApiHelper.httpPost(
                    "users/verify_code",
                    new VerifyCodeRequestDto(email, code),
                    VerifyCodeResponseDto.class,
                    new ApiHelper.ApiCallback<VerifyCodeResponseDto>() {
                        @Override public void onSuccess(VerifyCodeResponseDto verifyResp) {
                            if (verifyResp != null && verifyResp.getError() != null) {
                                btnSubmit.setEnabled(true);
                                toast(verifyResp.getError());
                                return;
                            }
                            // 2) 驗證通過 → 送註冊
                            registerToServer(email, password, name, gender, birthStr);
                        }
                        @Override public void onFailure(Throwable t) {
                            btnSubmit.setEnabled(true);
                            toast(t != null && t.getMessage() != null ? t.getMessage() : "驗證失敗");
                        }
                    }
            );
        });
    }

    private void registerToServer(String email, String password, String name, String gender, String birthStr) {


        ApiHelper.httpPost(
                "users/register", // 依你的後端路徑調整
                new RegisterRequestDto(email, password, name, gender, birthStr),
                RegisterResponseDto.class,
                new ApiHelper.ApiCallback<RegisterResponseDto>() {
                    @Override
                    public void onSuccess(RegisterResponseDto resp) {
                        btnSubmit.setEnabled(true);

                        if (resp == null) {
                            toast("註冊失敗：伺服器未回傳內容");
                            return;
                        }

                        if (Boolean.TRUE.equals(resp.success)) {
                            toast(resp.message != null ? resp.message : "註冊成功");
                            // 直接回登入頁
                            Intent i = new Intent(CheckActivity.this, LoginActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();
                        } else {
                            String msg = (resp.message != null && !resp.message.isEmpty()) ? resp.message : "註冊失敗";
                            toast(msg);
                            if ("Email 已被註冊".equals(resp.message) || "EMAIL_TAKEN".equals(resp.code)) {
                                etEmail.setError("該 Email 已註冊過");
                                etEmail.requestFocus();
                            }
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

    private boolean isValidDate(String s) {
        try {
            ymd.setLenient(false);
            ymd.parse(s); // 只能接受 yyyy/MM/dd
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

//    // ✅ 新增：導向 VerifyActivity（把 email 與狀態帶過去）
//    private void goToVerifyActivity(String email) {
//        Intent i = new Intent(CheckActivity.this, VerifyActivity.class);
//        i.putExtra("email", email);
//        i.putExtra("status", "register"); // 或 "1"；依你後端/流程的定義
//        startActivity(i);
//        // 視需求決定是否 finish()：若要禁止返回註冊頁就開啟
//        // finish();
//    }

    private String safeTrim(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void startCountdown(Button btn, int sec){
        btn.setEnabled(false);
        new android.os.CountDownTimer(sec*1000L,1000){
            public void onTick(long ms){ btn.setText((ms/1000)+" 秒後可重送"); }
            public void onFinish(){ btn.setText("取得驗證碼"); btn.setEnabled(true); }
        }.start();
    }

}
