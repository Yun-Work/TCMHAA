package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.dto.VerifyCodeRequestDto;
import com.example.tcmhaa.dto.VerifyCodeResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;

public class VerifyActivity extends AppCompatActivity {

    private EditText otpEditText;     // 對應 XML: @+id/otpEditText
    private Button   confirmButton;   // 對應 XML: @+id/confirmButton
    private TextView resendText;      // 對應 XML: @+id/resendText

    private String email;   // 從上一頁帶入
    private String status;  // "register"/"1" -> 註冊；"forgot"/"2" -> 忘記密碼

    public static final String EXTRA_EMAIL  = "email";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_USER_ID = "user_id";
    private int userId;     // 從上一頁帶入 user_id

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
            userId = from.getIntExtra("user_id",-1);
            status = from.getStringExtra("status");
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

                    // 呼叫後端驗證 API
                    // 你提供的版本是用 userId + code 驗證，沿用如下：
                    ApiHelper.httpPost(
                            "users/verify_code",
                            new VerifyCodeRequestDto(userId, code),   // 若後端註冊流程不需 userId，請在 DTO/後端另行對應（註解見下）
                            VerifyCodeResponseDto.class,
                            new ApiHelper.ApiCallback<VerifyCodeResponseDto>() {
                                @Override
                                public void onSuccess(VerifyCodeResponseDto resp) {
                                    if (resp != null && resp.getError() != null) {
                                        Toast.makeText(getApplicationContext(), resp.getError(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String msg = (resp != null && resp.getMessage() != null) ? resp.getMessage() : "驗證成功";
                                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                                    // 依來源流程導向
                                    if (isForgotFlow(status)) {
                                        // 忘記密碼 → 前往重設密碼（把 user_id 帶過去）
                                        Intent i = new Intent(VerifyActivity.this, Forget12Activity.class);
                                        i.putExtra(EXTRA_USER_ID, userId);
                                        i.putExtra(EXTRA_EMAIL, email); // 若下一頁也想用到 email 可一併帶上
                                        startActivity(i);
                                        finish();
                                    } else {
                                        // 註冊 → 回登入頁重新登入（清堆疊）
                                        Intent i = new Intent(VerifyActivity.this, LoginActivity.class);
                                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(i);
                                        finishAffinity();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            (t != null && t.getMessage() != null) ? t.getMessage() : "連線失敗",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                    );
                });
            }

            /** 支援兩種帶法：forgot/2 為忘記密碼，其餘視為註冊 */
            private boolean isForgotFlow(String s) {
                if (s == null) return false;
                String v = s.trim().toLowerCase();
                return "forgot".equals(v) || "2".equals(v);
            }
        }