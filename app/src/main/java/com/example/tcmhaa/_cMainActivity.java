package com.example.tcmhaa;

import android.content.Intent;
import android.graphics.Color;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
    private LineChart lineChart; // ← 使用折線圖
    private TextView tvChartPlaceholder, tvTextResult;

    // 日期區間（毫秒）
    private Long selectedStartMillis;
    private Long selectedEndMillis;

    // 後端：多色折線（每天 0/1）
    private static final String HISTORY_API_PATH_COLOR_SERIES = "history/status-bar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        spOrgan            = findViewById(R.id.spOrgan);
        btnPickRange       = findViewById(R.id.btnPickRange);
        btnConfirm         = findViewById(R.id.btnConfirm);
        btnDownload        = findViewById(R.id.btnDownload);
        lineChart          = findViewById(R.id.lineChart); // ← 對應 XML 的 LineChart
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

        // 呼叫後端：organ + start + end → 回傳 x + series(五色)
        btnConfirm.setOnClickListener(v -> {
            if (selectedStartMillis == null || selectedEndMillis == null) {
                Toast.makeText(this, "請先選擇時間範圍", Toast.LENGTH_SHORT).show();
                return;
            }
            //  取出 user_id（登入時已存進 SharedPreferences）
            int uid = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
            String organ = spOrgan.getSelectedItem() == null ? "" : spOrgan.getSelectedItem().toString();
            String start = formatDate(selectedStartMillis); // YYYY-MM-DD
            String end   = formatDate(selectedEndMillis);
            fetchColorSeries(organ, start, end,uid);
        });

        setupBottomNav();
        setupLineChartStyle();
    }

    /** 呼叫 /api/history/organ-color-series 取得多色 0/1 折線資料 */
    private void fetchColorSeries(String organ, String start, String end, int userId) {
        HistoryStatusBarRequestDto req = new HistoryStatusBarRequestDto(organ, start, end, userId);

        ApiHelper.httpPost(
                HISTORY_API_PATH_COLOR_SERIES,
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
                        renderMultiColorLineChart(resp, organ, start, end);
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(_cMainActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /** 繪製多色折線（發紅/發黑/發黃/發白/發青 → 5 條 0/1 線） */
    private void renderMultiColorLineChart(HistoryStatusBarResponseDto r, String organ, String start, String end) {
        List<String> x = (r.x != null) ? r.x : new ArrayList<>();

        final float SHIFT_RED    = -0.24f;
        final float SHIFT_BLACK  = -0.12f;
        final float SHIFT_YELLOW =  0.00f;
        final float SHIFT_WHITE  =  0.12f;
        final float SHIFT_CYAN   =  0.24f;

        List<Entry> red    = toEntries(r.series != null ? r.series.red    : null,SHIFT_RED);
        List<Entry> black  = toEntries(r.series != null ? r.series.black  : null,SHIFT_BLACK);
        List<Entry> yellow = toEntries(r.series != null ? r.series.yellow : null,SHIFT_YELLOW);
        List<Entry> white  = toEntries(r.series != null ? r.series.white  : null,SHIFT_WHITE);
        List<Entry> cyan   = toEntries(r.series != null ? r.series.cyan   : null,SHIFT_CYAN);

        List<ILineDataSet> sets = new ArrayList<>();
        addIfNotAllZero(sets, makeDataSet(red,   "發紅",  Color.parseColor("#E53935")));
        addIfNotAllZero(sets, makeDataSet(black, "發黑",  Color.parseColor("#212121")));
        addIfNotAllZero(sets, makeDataSet(yellow,"發黃",  Color.parseColor("#FBC02D")));
        addIfNotAllZero(sets, makeDataSet(white, "發白",  Color.parseColor("#BDBDBD")));
        addIfNotAllZero(sets, makeDataSet(cyan,  "發青",  Color.parseColor("#26A69A")));

        if (sets.isEmpty()) { // 全為 0 時避免空圖
            sets.add(makeDataSet(new ArrayList<>(), "無異常", Color.parseColor("#90A4AE")));
        }

        lineChart.setData(new LineData(sets));

        // X 軸：日期字串
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(x.size(), 6));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(x));
        xAxis.setAxisMinimum(-0.6f);
        xAxis.setAxisMaximum(Math.max(0, x.size() - 1) + 0.6f);
        // 關掉垂直灰線
        xAxis.setDrawGridLines(false);
        // 保留軸線本體並可調色
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineWidth(1f);
        xAxis.setAxisLineColor(Color.parseColor("#888888")); // 想更深改這裡

        // Y 軸：0/1、整數刻度
        YAxis left = lineChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(1f);
        left.setGranularity(1f);
        left.setLabelCount(2, true);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });
        // 關掉水平灰線
        left.setDrawGridLines(false);
        // 保留軸線本體並可調色
        left.setDrawAxisLine(true);
        left.setAxisLineWidth(1f);
        left.setAxisLineColor(Color.parseColor("#888888"));
        lineChart.getAxisRight().setEnabled(false);

        // 圖例/描述
        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);
        lineChart.getDescription().setEnabled(false);

        tvChartPlaceholder.setVisibility(android.view.View.GONE);
        lineChart.setVisibility(android.view.View.VISIBLE);
        lineChart.invalidate();

        tvTextResult.setText(organ + "（" + start + " ~ " + end + "）\n顏色異常：1=有、0=無");
    }

    // ===== 小工具 =====

    private List<Entry> toEntries(List<Integer> ys, float xShift) {
        List<Entry> list = new ArrayList<>();
        int n = (ys != null) ? ys.size() : 0;
        for (int i = 0; i < n; i++) {
            // null 或 0 一律當 0，1 當 1 → 連續折線
            int v = (ys.get(i) == null) ? 0 : ys.get(i);
            list.add(new Entry(i + xShift, v == 1 ? 1f : 0f));
        }
        return list;
    }

    private LineDataSet makeDataSet(List<Entry> es, String label, int color) {
        LineDataSet ds = new LineDataSet(es, label);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setLineWidth(2f);

        ds.setDrawCircles(true);
        ds.setCircleRadius(4f);                 // 稍微大一點
        ds.setDrawCircleHole(true);             // 中空圈
        ds.setCircleHoleRadius(2f);
        ds.setCircleHoleColor(Color.WHITE);     // 白色洞，重疊時看得出層次

        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        ds.setDrawFilled(false);

        // 只在有點的地方畫線（前面我們把 0/無資料設為 NaN，自然就不連線）
        ds.setHighlightEnabled(true);           // 點擊高亮（可選）
        return ds;
    }

    private void addIfNotAllZero(List<ILineDataSet> sets, LineDataSet ds) {
        boolean anyOne = false;
        for (Entry e : ds.getValues()) { if (e.getY() >= 0.5f) { anyOne = true; break; } }
        if (anyOne) sets.add(ds);
    }

    // ===== 折線圖外觀 / 日期選擇 =====

    private void setupLineChartStyle() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setNoDataText("尚未載入資料");
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        // 背景色（可選）
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT); // 或 Color.WHITE
    }

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
                btnPickRange.setText(formatDate(selectedStartMillis) + " ~ " + formatDate(selectedEndMillis));
            }
        });

        picker.addOnNegativeButtonClickListener(v ->
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show());

        picker.show(getSupportFragmentManager(), "date_range_picker");
    }

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
