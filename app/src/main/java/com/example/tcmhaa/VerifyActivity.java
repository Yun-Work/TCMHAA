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
    private String status;  // 狀態碼（例如 "2" 代表忘記密碼流程）
    private int userId;     // 從上一頁帶入 user_id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_2_1_1); // 對應 XML 檔名

        // 綁定對應 XML 的 ID
        otpEditText   = findViewById(R.id.otpEditText);
        confirmButton = findViewById(R.id.confirmButton);
        resendText    = findViewById(R.id.resendText);

        // 取得上一頁 extras
        Intent from = getIntent();
        if (from != null) {
            userId = from.getIntExtra("user_id",-1);
            status = from.getStringExtra("status");
        }

        // 重新寄送驗證碼（TODO: 呼叫後端 API）
        resendText.setOnClickListener(v -> {
            Toast.makeText(this, "已重新寄送驗證碼到：" + (email != null ? email : "你的信箱"), Toast.LENGTH_SHORT).show();
        });

        // 確認驗證碼
        confirmButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                otpEditText.setError("請輸入驗證碼");
                otpEditText.requestFocus();
                return;
            }


            ApiHelper.httpPost(
                    "users/verify_code",
                    new VerifyCodeRequestDto(userId, code),
                    VerifyCodeResponseDto.class,
                    new ApiHelper.ApiCallback<VerifyCodeResponseDto>() {
                        @Override
                        public void onSuccess(VerifyCodeResponseDto resp) {
                            if (resp.getError() != null) {
                                Toast.makeText(getApplicationContext(),
                                        resp.getError(),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String msg = resp.getMessage() != null ? resp.getMessage() : "驗證成功";
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                            // 驗證成功 → 跳轉到重設密碼頁
                            Intent i = new Intent(VerifyActivity.this, Forget12Activity.class);
                            i.putExtra("user_id", userId);  // 改成帶 user_id
                            startActivity(i);
                            finish(); // 關掉驗證頁，避免返回
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Toast.makeText(getApplicationContext(),
                                    (t != null && t.getMessage() != null) ? t.getMessage() : "連線失敗",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }
}
//            // 假設驗證通過 → 跳轉到 Forget12Activity
//            Toast.makeText(this, "驗證成功！", Toast.LENGTH_SHORT).show();
//
//            Intent i = new Intent(VerifyActivity.this, Forget12Activity.class);
//            i.putExtra("email", email);   // 把 email 傳過去，方便重設密碼用
//            startActivity(i);
//            finish(); // 關掉驗證頁，避免返回
//        });
//    }
//}
