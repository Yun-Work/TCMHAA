package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_4); // 這是你給的 XML layout

        Button nextButton = findViewById(R.id.btn_next);

        nextButton.setOnClickListener(view -> {
            // 👉 點擊「下一步」跳轉到 WarningActivity
            Intent intent = new Intent(WelcomeActivity.this, WarningActivity.class);
            startActivity(intent);
        });
    }
}
