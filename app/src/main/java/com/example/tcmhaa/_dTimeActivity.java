package com.example.tcmhaa;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class _dTimeActivity extends AppCompatActivity {

    private static final String PREFS = "reminder_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MIN = "min";

    private TimePicker timePicker;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_time);

        timePicker = findViewById(R.id.timePicker);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnBack = findViewById(R.id.btnBack); // ← XML 新增的返回按鈕

        // 設定 24 小時制
        timePicker.setIs24HourView(true);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = sp.getBoolean(KEY_ENABLED, false);
        int hour = sp.getInt(KEY_HOUR, 9);
        int min = sp.getInt(KEY_MIN, 0);

        // 初始時間
        timePicker.setHour(hour);
        timePicker.setMinute(min);

        updateStatusText(enabled, hour, min);

        // 儲存並啟用提醒
        btnSave.setOnClickListener(v -> {
            int h = timePicker.getHour();
            int m = timePicker.getMinute();

            sp.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putInt(KEY_HOUR, h)
                    .putInt(KEY_MIN, m)
                    .apply();

            ReminderScheduler.scheduleDailyReminder(this, h, m);
            updateStatusText(true, h, m);

            Toast.makeText(this, "已儲存並啟用每日提醒", Toast.LENGTH_SHORT).show();
        });

        // 返回按鈕 → 回到 _dMainActivity
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(_dTimeActivity.this, _dMainActivity.class);
            startActivity(intent);
            finish(); // 關閉自己，避免返回堆疊
        });
    }

    private void updateStatusText(boolean enabled, int hour, int min) {
        if (enabled) {
            String hh = String.format("%02d", hour);
            String mm = String.format("%02d", min);
            tvStatus.setText("目前：已開啟，每天 " + hh + ":" + mm + " 提醒");
        } else {
            tvStatus.setText("目前：尚未開啟每日提醒");
        }
    }

    // 實體返回鍵 → 同樣回到 _dMainActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(_dTimeActivity.this, _dMainActivity.class);
        startActivity(intent);
        finish();
    }
}
