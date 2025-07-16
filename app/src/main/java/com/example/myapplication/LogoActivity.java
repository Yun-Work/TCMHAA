package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class LogoActivity extends AppCompatActivity {

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo); // 對應 logo.xml

        @SuppressLint("ResourceType") ImageView logoImageView = findViewById(R.drawable.newlogo);

        // 點擊 logo 進入登入畫面
        logoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogoActivity.this, LogoActivity.class);
                startActivity(intent);
                finish(); // 關閉 LogoActivity，避免返回
            }
        });
    }
}

