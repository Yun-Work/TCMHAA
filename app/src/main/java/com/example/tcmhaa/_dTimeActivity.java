package com.example.tcmhaa;

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

        // 改在程式裡設定 24 小時制
        timePicker.setIs24HourView(true);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = sp.getBoolean(KEY_ENABLED, false);
        int hour = sp.getInt(KEY_HOUR, 9);
        int min = sp.getInt(KEY_MIN, 0);

        // 設定初始時間
        timePicker.setHour(hour);
        timePicker.setMinute(min);

        updateStatusText(enabled, hour, min);

        btnSave.setOnClickListener(v -> {
            int h = timePicker.getHour();
            int m = timePicker.getMinute();

            // 存檔並啟用提醒
            sp.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putInt(KEY_HOUR, h)
                    .putInt(KEY_MIN, m)
                    .apply();

            // 呼叫我們的提醒排程工具
            ReminderScheduler.scheduleDailyReminder(this, h, m);
            updateStatusText(true, h, m);

            Toast.makeText(this, "已儲存並啟用每日提醒", Toast.LENGTH_SHORT).show();
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
}
