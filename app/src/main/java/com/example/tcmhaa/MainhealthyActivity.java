package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainhealthyActivity extends AppCompatActivity {

    // 上方兩顆按鈕
    private Button buttonChoosePhoto, buttonTakePhoto;

    // 底部四個導覽
    private LinearLayout healthy_1, healthy_2, healthy_3, healthy_4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_5);

        // 1) 綁定上方按鈕
        buttonChoosePhoto = findViewById(R.id.buttonChoosePhoto);
        buttonTakePhoto   = findViewById(R.id.buttonTakePhoto);

        buttonChoosePhoto.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, PhotoActivity.class)));   // activity_photo_5_1
//        buttonChoosePhoto.setOnClickListener(v ->
//                startActivity(new Intent(MainhealthyActivity.this, WarningActivity.class)));   // activity_photo_5_1

        buttonTakePhoto.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, CameraActivity.class)));  // activity_camera_5_1

        // 2) 綁定底部四個導覽
        healthy_1 = findViewById(R.id.nav_a);
        healthy_2 = findViewById(R.id.nav_b);
        healthy_3 = findViewById(R.id.nav_c);
        healthy_4 = findViewById(R.id.nav_d);

        healthy_1.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, _aMainActivity.class)));

        healthy_2.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, _bMainActivity.class)));

        healthy_3.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, _cMainActivity.class)));

        healthy_4.setOnClickListener(v ->
                startActivity(new Intent(MainhealthyActivity.this, _dMainActivity.class)));
    }
}
