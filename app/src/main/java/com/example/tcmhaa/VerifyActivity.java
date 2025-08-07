package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyActivity extends AppCompatActivity {

    private EditText editTextVerificationCode;
    private Button buttonVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        buttonVerify = findViewById(R.id.buttonVerify);

        buttonVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = editTextVerificationCode.getText().toString().trim();

                if (code.equals("123456")) { // 假設驗證碼為 123456，可根據實際邏輯修改
                    Toast.makeText(VerifyActivity.this, "驗證成功", Toast.LENGTH_SHORT).show();
                    // 導向下一頁面
                    Intent intent = new Intent(VerifyActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyActivity.this, "驗證碼錯誤", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
