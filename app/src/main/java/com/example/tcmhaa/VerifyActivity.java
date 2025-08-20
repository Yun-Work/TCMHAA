package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.utils.api.ApiHelper;
import com.example.tcmhaa.dto.VerifyCodeRequestDto;
import com.example.tcmhaa.dto.VerifyCodeResponseDto;

public class VerifyActivity extends AppCompatActivity {

    private EditText editTextVerificationCode;
    private Button buttonVerify;

    private String email;   // 從上一頁帶入
    private String status;  // 若後端不需要可以不使用（保留給流程判斷）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_2_1_2);

        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        buttonVerify = findViewById(R.id.buttonVerify);

        // 取上一頁 extras；若你上一頁用的是 "gmail" 當 key，也一併相容
        Intent from = getIntent();
        email  = from.getStringExtra("email");
        if (email == null) email = from.getStringExtra("gmail");
        status = from.getStringExtra("status"); // 可能是 "2"（忘記密碼），看你前頁怎麼傳

        buttonVerify.setOnClickListener(v -> {
            String code = editTextVerificationCode.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                toast("缺少 Email，請從上一頁重新進入");
                return;
            }
            if (!code.matches("^\\d{6}$")) {
                toast("請輸入 6 位數驗證碼");
                return;
            }

            buttonVerify.setEnabled(false);

            ApiHelper.httpPost(
                    "users/verify_code",
                    new VerifyCodeRequestDto(email, code),
                    VerifyCodeResponseDto.class,
                    new ApiHelper.ApiCallback<VerifyCodeResponseDto>() {
                        @Override
                        public void onSuccess(VerifyCodeResponseDto resp) {
                            buttonVerify.setEnabled(true);
                            if (resp != null && resp.getError() == null) {
                                toast(resp.getMessage() != null ? resp.getMessage() : "驗證成功");
                                startActivity(new Intent(VerifyActivity.this, ProfileActivity.class));
                                finish();
                            } else {
                                String msg = (resp != null && resp.getError() != null) ? resp.getError() : "驗證失敗";
                                toast(msg);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            buttonVerify.setEnabled(true);
                            toast("連線錯誤：" + (t != null ? t.getMessage() : "未知錯誤"));
                        }
                    }
            );

        });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
