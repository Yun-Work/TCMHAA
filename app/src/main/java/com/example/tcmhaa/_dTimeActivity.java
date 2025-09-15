package com.example.tcmhaa;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.AlarmManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
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

//        ReminderScheduler.scheduleOneShotInSeconds(this, 10, "10 秒測試：如果你看到這則代表排程 OK！");
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

            ReminderScheduler.scheduleDailyExact(this, h, m);
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
    private boolean canUseExactAlarm() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms();
        }
        return true; // 31 以下不需要
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                i.setData(Uri.parse("package:" + getPackageName())); // 指向本 App
                startActivity(i);
                Toast.makeText(this, "請在「鬧鐘與提醒」中允許本 App 的精準鬧鐘", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // 某些機型可能沒有此設定入口，至少提示使用者
                Toast.makeText(this, "請到系統設定→應用程式→特殊應用程式存取→鬧鐘與提醒，允許本 App", Toast.LENGTH_LONG).show();
            }
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
