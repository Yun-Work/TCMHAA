package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WarningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_n); // 確認 XML 名稱與 btn_yes / btn_no 存在

        Button btnYes = findViewById(R.id.btn_yes);
        Button btnNo  = findViewById(R.id.btn_no);

        // 不論按「是」或「否」→ 前往 _bMainActivity，並把 CameraActivity 傳來的資料一併帶過去
        btnYes.setOnClickListener(v -> goNext());
        btnNo.setOnClickListener(v -> goNext());
    }

    /** 將 CameraActivity 傳來的 extras 原樣轉交給 _bMainActivity */
    private void goNext() {
        Intent from = getIntent();
        Intent intent = new Intent(WarningActivity.this, _bMainActivity.class);

        if (from != null) {
            intent.putExtras(from); // analysis_result / source_type / original_image_base64 等
        }

        // 可選：避免返回鍵回到相機或警示頁
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
        finish(); // 關閉 WarningActivity
    }
}
