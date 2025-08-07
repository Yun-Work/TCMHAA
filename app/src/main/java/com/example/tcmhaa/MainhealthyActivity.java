package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainhealthyActivity extends AppCompatActivity {

    LinearLayout healthy_1, healthy_2, healthy_3, healthy_4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_5);

        healthy_1 = findViewById(R.id.nav_a);
        healthy_2 = findViewById(R.id.nav_b);
        healthy_3 = findViewById(R.id.nav_c);
        healthy_4 = findViewById(R.id.nav_d);

        healthy_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainhealthyActivity.this, _aMainActivity.class);
                startActivity(intent);
            }
        });

        healthy_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainhealthyActivity.this, _bMainActivity.class);
                startActivity(intent);
            }
        });

        healthy_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainhealthyActivity.this, _cMainActivity.class);
                startActivity(intent);
            }
        });

        healthy_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainhealthyActivity.this, _dMainActivity.class);
                startActivity(intent);
            }
        });
    }
}
