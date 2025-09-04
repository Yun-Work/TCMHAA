package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.tcmhaa.dto.HistoryStatusBarRequestDto;
import com.example.tcmhaa.dto.HistoryStatusBarResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class _cMainActivity extends AppCompatActivity {

    private Spinner spOrgan;
    private Button btnPickRange, btnConfirm;
    private ImageButton btnDownload;
    private BarChart barChart;
    private TextView tvChartPlaceholder, tvTextResult;

    // 日期區間（毫秒）
    private Long selectedStartMillis;
    private Long selectedEndMillis;

    // ApiHelper 只需要 path；BASE_URL 在 ApiHelper 內是 http://10.0.2.2:6060/api/
    private static final String HISTORY_API_PATH = "history/status-bar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        spOrgan            = findViewById(R.id.spOrgan);
        btnPickRange       = findViewById(R.id.btnPickRange);
        btnConfirm         = findViewById(R.id.btnConfirm);
        btnDownload        = findViewById(R.id.btnDownload);
        barChart           = findViewById(R.id.barChart);
        tvChartPlaceholder = findViewById(R.id.tvChartPlaceholder);
        tvTextResult       = findViewById(R.id.tvTextResult);

        // 下拉選單
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.organs_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrgan.setAdapter(adapter);

        // 預設日期：最近 7 天
        Pair<Long, Long> last7Days = getLast7Days();
        selectedStartMillis = last7Days.first;
        selectedEndMillis   = last7Days.second;
        btnPickRange.setText(formatDate(selectedStartMillis) + " ~ " + formatDate(selectedEndMillis));

        btnPickRange.setOnClickListener(v -> showDateRangePicker());
        btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "這裡執行匯出/下載", Toast.LENGTH_SHORT).show());

        // ✅ 使用 ApiHelper + DTO 呼叫後端
        btnConfirm.setOnClickListener(v -> {
            if (selectedStartMillis == null || selectedEndMillis == null) {
                Toast.makeText(this, "請先選擇時間範圍", Toast.LENGTH_SHORT).show();
                return;
            }
            String organ = spOrgan.getSelectedItem() == null ? "" : spOrgan.getSelectedItem().toString();
            String start = formatDate(selectedStartMillis); // YYYY-MM-DD
            String end   = formatDate(selectedEndMillis);
            fetchHistoryWithApiHelper(organ, start, end);
        });

        setupBottomNav();
        setupChartStyle();
    }

    /** 用 ApiHelper + DTO 呼叫 /api/history/status-bar */
    private void fetchHistoryWithApiHelper(String organ, String start, String end) {
        HistoryStatusBarRequestDto req = new HistoryStatusBarRequestDto(organ, start, end);

        ApiHelper.httpPost(
                HISTORY_API_PATH,
                req,
                HistoryStatusBarResponseDto.class,
                new ApiHelper.ApiCallback<HistoryStatusBarResponseDto>() {
                    @Override
                    public void onSuccess(HistoryStatusBarResponseDto resp) {
                        if (resp == null || resp.hasError()) {
                            String msg = (resp != null && resp.error != null) ? resp.error : "查詢失敗";
                            Toast.makeText(_cMainActivity.this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        renderChart(resp, organ, start, end);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(_cMainActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /** 把 categories/data 畫成長條圖（X=狀態，Y=次數） */
    private void renderChart(HistoryStatusBarResponseDto r, String organ, String start, String end) {
        List<String> labels = (r.categories != null) ? r.categories : new ArrayList<>();
        List<Integer> vals  = (r.data != null) ? r.data : new ArrayList<>();

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < vals.size(); i++) {
            entries.add(new BarEntry(i, vals.get(i)));
        }

        BarDataSet set = new BarDataSet(entries, organ + "（" + start + " ~ " + end + "）");
        BarData data = new BarData(set);
        data.setBarWidth(0.9f);

        barChart.setData(data);

        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setLabelCount(labels.size());
        x.setValueFormatter(new IndexAxisValueFormatter(labels));

        tvChartPlaceholder.setVisibility(android.view.View.GONE);
        barChart.setVisibility(android.view.View.VISIBLE);
        barChart.invalidate();

        // 可選：顯示簡單摘要
        tvTextResult.setText("各狀態件數：" + labels + " -> " + vals);
    }

    /** 圖表外觀 */
    private void setupChartStyle() {
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setNoDataText("尚未載入資料");

        // ⬇️ Y 軸：整數刻度、間隔=1
        YAxis y = barChart.getAxisLeft();
        y.setAxisMinimum(0f);            // 可選：從 0 開始
        y.setGranularity(1f);            // 刻度間隔（要 2、5、10 就改這裡）
        y.setGranularityEnabled(true);
        y.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // 刻度顯示整數
            }
        });
    }

    /** 打開日期區間選擇器 */
    private void showDateRangePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

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
                selectedEndMillis   = selection.second;

                String startDateStr = formatDate(selectedStartMillis);
                String endDateStr   = formatDate(selectedEndMillis);
                btnPickRange.setText(startDateStr + " ~ " + endDateStr);
            }
        });

        picker.addOnNegativeButtonClickListener(v ->
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show());

        picker.show(getSupportFragmentManager(), "date_range_picker");
    }

    /** 最近 7 天（含今天） */
    private Pair<Long, Long> getLast7Days() {
        Calendar end = Calendar.getInstance();
        setToStartOfDay(end);
        long endMs = end.getTimeInMillis();

        Calendar start = Calendar.getInstance();
        setToStartOfDay(start);
        start.add(Calendar.DAY_OF_YEAR, -6);
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
