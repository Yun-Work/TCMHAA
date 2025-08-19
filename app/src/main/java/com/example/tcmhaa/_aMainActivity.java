package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class _aMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 若想顯示 activity_mainhealthy_a 的畫面，可改為：
        // setContentView(R.layout.activity_mainhealthy_a);
        startActivity(new Intent(this, MainhealthyActivity.class));
        finish();
    }
}
