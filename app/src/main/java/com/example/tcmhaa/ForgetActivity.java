package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgetActivity extends AppCompatActivity {

    private EditText editTextGmail;
    private Button buttonSendCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_1_1);

        editTextGmail = findViewById(R.id.editTextGmail);
        buttonSendCode = findViewById(R.id.buttonSendCode);

        buttonSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gmail = editTextGmail.getText().toString().trim();

                if (TextUtils.isEmpty(gmail)) {
                    Toast.makeText(ForgetActivity.this, "請輸入 Gmail", Toast.LENGTH_SHORT).show();
                } else if (!gmail.contains("@") || !gmail.contains(".com")) {
                    Toast.makeText(ForgetActivity.this, "請輸入正確的 Gmail 格式", Toast.LENGTH_SHORT).show();
                } else {
                    // 模擬寄送驗證碼的流程
                    Toast.makeText(ForgetActivity.this, "驗證碼已寄送到 " + gmail, Toast.LENGTH_LONG).show();

                    // 寄送成功後跳轉至驗證碼輸入畫面（例如 VerifyActivity）
                    Intent intent = new Intent(ForgetActivity.this, VerifyActivity.class);
                    intent.putExtra("gmail", gmail);
                    startActivity(intent);
                }
            }
        });
    }
}
