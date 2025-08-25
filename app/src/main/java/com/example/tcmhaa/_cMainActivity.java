package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class _cMainActivity extends AppCompatActivity {

    private Button btnPickRange;
    private ImageButton btnDownload;

    // 紀錄目前的區間（給你後續查資料或再次打開維持選取）
    private Long selectedStartMillis;
    private Long selectedEndMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        btnPickRange = findViewById(R.id.btnPickRange);
        btnDownload = findViewById(R.id.btnDownload);

        // 預設區間：最近 7 天
        Pair<Long, Long> last7Days = getLast7Days();
        selectedStartMillis = last7Days.first;
        selectedEndMillis = last7Days.second;
        btnPickRange.setText(formatDate(selectedStartMillis) + " ~ " + formatDate(selectedEndMillis));

        btnPickRange.setOnClickListener(v -> showDateRangePicker());
        btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "這裡執行匯出/下載", Toast.LENGTH_SHORT).show());

        setupBottomNav();
    }

    /** 打開 Material 日期「區間」選擇器 */
    private void showDateRangePicker() {
        // 限制：最大日期為今天（避免挑未來）
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        // 如果之前有選過，就維持該選取；否則預設最近 7 天
        Pair<Long, Long> defaultRange = (selectedStartMillis != null && selectedEndMillis != null)
                ? new Pair<>(selectedStartMillis, selectedEndMillis)
                : getLast7Days();

        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("選擇日期區間")
                        .setCalendarConstraints(constraints)
                        .setSelection(defaultRange)
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                selectedStartMillis = selection.first;
                selectedEndMillis = selection.second;

                String startDateStr = formatDate(selectedStartMillis);
                String endDateStr = formatDate(selectedEndMillis);
                btnPickRange.setText(startDateStr + " ~ " + endDateStr);

                // TODO: 在這裡用 startDateStr / endDateStr 去查歷史資料與更新圖表
                // fetchHistory(startDateStr, endDateStr);
                Toast.makeText(this, "已選擇：" + startDateStr + " ~ " + endDateStr, Toast.LENGTH_SHORT).show();
            }
        });

        picker.addOnNegativeButtonClickListener(v ->
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show());

        picker.show(getSupportFragmentManager(), "date_range_picker");
    }

    /** 取得「最近 7 天」的起訖毫秒（包含今天） */
    private Pair<Long, Long> getLast7Days() {
        Calendar end = Calendar.getInstance(); // 今天
        setToStartOfDay(end);
        long endMs = end.getTimeInMillis();

        Calendar start = Calendar.getInstance();
        setToStartOfDay(start);
        start.add(Calendar.DAY_OF_YEAR, -6); // 含今天共 7 天
        long startMs = start.getTimeInMillis();

        return new Pair<>(startMs, endMs);
    }

    private void setToStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN).format(new Date(millis));
    }

    private void setupBottomNav() {
        LinearLayout navA = findViewById(R.id.nav_a);
        LinearLayout navB = findViewById(R.id.nav_b);
        LinearLayout navC = findViewById(R.id.nav_c);
        LinearLayout navD = findViewById(R.id.nav_d);

        navA.setOnClickListener(v -> startActivity(new Intent(this, _aMainActivity.class)));
        navB.setOnClickListener(v -> startActivity(new Intent(this, _bMainActivity.class)));
        navC.setOnClickListener(v -> { /* 留在本頁 */ });
        navD.setOnClickListener(v -> startActivity(new Intent(this, _dMainActivity.class)));
    }
}
