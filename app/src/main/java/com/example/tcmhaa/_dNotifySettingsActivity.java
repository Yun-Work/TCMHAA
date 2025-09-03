package com.example.tcmhaa;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class _dNotifySettingsActivity extends AppCompatActivity {

    private static final String PREFS = "reminder_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MIN = "min";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int hour = sp.getInt(KEY_HOUR, 9);
        int min = sp.getInt(KEY_MIN, 0);
        boolean enabled = sp.getBoolean(KEY_ENABLED, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(getLayoutInflater().inflate(R.layout.activity_d_notify, null))
                .setPositiveButton(enabled ? "關閉" : "開啟", (d, which) -> {
                    if (enabled) {
                        // 關閉
                        sp.edit().putBoolean(KEY_ENABLED, false).apply();
                        ReminderScheduler.cancelDailyReminder(this);
                        Toast.makeText(this, "已關閉每日提醒", Toast.LENGTH_SHORT).show();
                    } else {
                        // 開啟（沿用已設定時間，若從未設定過，預設 09:00）
                        sp.edit().putBoolean(KEY_ENABLED, true).apply();
                        ReminderScheduler.scheduleDailyReminder(this, hour, min);
                        Toast.makeText(this, "已開啟每日提醒", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                })
                .setNegativeButton("取消", (d, w) -> {
                    d.dismiss();
                    finish();
                })
                .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
