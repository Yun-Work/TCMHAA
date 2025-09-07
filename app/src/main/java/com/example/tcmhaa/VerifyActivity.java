package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyActivity extends AppCompatActivity {

    private EditText otpEditText;     // 對應 XML: @+id/otpEditText
    private Button   confirmButton;   // 對應 XML: @+id/confirmButton
    private TextView resendText;      // 對應 XML: @+id/resendText

    private String email;   // 從上一頁帶入
    private String status;  // "register"/"1" -> 註冊；"forgot"/"2" -> 忘記密碼

    public static final String EXTRA_EMAIL  = "email";
    public static final String EXTRA_STATUS = "status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_2_1_1);

        otpEditText   = findViewById(R.id.otpEditText);
        confirmButton = findViewById(R.id.confirmButton);
        resendText    = findViewById(R.id.resendText);

        Intent from = getIntent();
        if (from != null) {
            email  = from.getStringExtra(EXTRA_EMAIL);
            status = from.getStringExtra(EXTRA_STATUS);
        }
        if (status == null || status.trim().isEmpty()) {
            status = "register"; // 預設走註冊驗證流程
        }

        // 重新寄送驗證碼（可在此接後端 API）
        resendText.setOnClickListener(v ->
                Toast.makeText(this, "已重新寄送驗證碼到：" + (email != null ? email : "你的信箱"), Toast.LENGTH_SHORT).show()
        );

        // 確認驗證碼
        confirmButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                otpEditText.setError("請輸入驗證碼");
                otpEditText.requestFocus();
                return;
            }

            // TODO: 呼叫後端 API 驗證 (email, code, status)
            // 成功後才呼叫 onOtpVerifiedSuccess()

            onOtpVerifiedSuccess();
        });
    }

    /** 驗證成功後依來源導向 */
    private void onOtpVerifiedSuccess() {
        Toast.makeText(this, "驗證成功！", Toast.LENGTH_SHORT).show();

        if (isForgotFlow(status)) {
            // 忘記密碼流程 -> 前往重設密碼
            Intent i = new Intent(VerifyActivity.this, Forget12Activity.class);
            i.putExtra(EXTRA_EMAIL, email);
            startActivity(i);
            finish(); // 關閉驗證頁
        } else {
            // 註冊流程 -> 回登入頁重新登入
            Intent i = new Intent(VerifyActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finishAffinity(); // 關閉目前 task 內其他頁面
        }
    }

    /** 支援你現在兩種帶法：forgot/2 為忘記密碼，其餘視為註冊 */
    private boolean isForgotFlow(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        return "forgot".equals(v) || "2".equals(v);
    }
}
