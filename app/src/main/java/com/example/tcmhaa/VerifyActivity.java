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
    private String status;  // 狀態碼（例如 "2" 代表忘記密碼流程）

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
            email  = from.getStringExtra("email");
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

            // TODO: 在這裡呼叫後端驗證 API 驗證 code
            // 假設驗證通過 → 跳轉到 Forget12Activity
            Toast.makeText(this, "驗證成功！", Toast.LENGTH_SHORT).show();

            Intent i = new Intent(VerifyActivity.this, Forget12Activity.class);
            i.putExtra("email", email);   // 把 email 傳過去，方便重設密碼用
            startActivity(i);
            finish(); // 關掉驗證頁，避免返回
        });
    }
}
