package com.example.tcmhaa;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WarningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_5); // 請確認 layout 名稱

        Button nextButton = findViewById(R.id.btn_next);

        nextButton.setOnClickListener(v -> {
            // 👉 這邊你可以寫跳到下一個 Activity 的程式碼
            // 目前先顯示提示（你可以改為跳下一頁）
            Toast.makeText(this, "尚未設定下一步動作", Toast.LENGTH_SHORT).show();
        });
    }
}
