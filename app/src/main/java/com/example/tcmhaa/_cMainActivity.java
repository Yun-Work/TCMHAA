package com.example.tcmhaa;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private LineChart lineChart;
    private TextView tvChartPlaceholder;

    // 問答區
    private EditText etQuestion;
    private Button btnAskConfirm;
    private TextView tvAnswer;

    // 日期區間（毫秒）
    private Long selectedStartMillis;
    private Long selectedEndMillis;

    // ====== 後端：歷史折線圖 ======
    private static final String HISTORY_API_PATH_COLOR_SERIES = "history/status-bar"; // ApiHelper（自帶 /api 前綴）

    // ====== 後端：LLM 問答（新）======
    private static final String OLLAMA_ASK_API_PATH = "ollama/ask"; // 對應 Flask：POST /api/ollama/ask

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        // 綁定元件
        spOrgan            = findViewById(R.id.spOrgan);
        btnPickRange       = findViewById(R.id.btnPickRange);
        btnConfirm         = findViewById(R.id.btnConfirm);
        lineChart          = findViewById(R.id.lineChart);
        tvChartPlaceholder = findViewById(R.id.tvChartPlaceholder);
        etQuestion         = findViewById(R.id.etQuestion);
        btnAskConfirm      = findViewById(R.id.btnAskConfirm);
        tvAnswer           = findViewById(R.id.tvAnswer);

        // 讓回應內網址可點
        tvAnswer.setAutoLinkMask(Linkify.WEB_URLS);
        tvAnswer.setMovementMethod(LinkMovementMethod.getInstance());

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

        // 歷史顏色折線
        btnConfirm.setOnClickListener(v -> {
            if (selectedStartMillis == null || selectedEndMillis == null) {
                Toast.makeText(this, "請先選擇時間範圍", Toast.LENGTH_SHORT).show();
                return;
            }
            int uid = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
            String organ = spOrgan.getSelectedItem() == null ? "" : spOrgan.getSelectedItem().toString();
            String start = formatDate(selectedStartMillis);
            String end   = formatDate(selectedEndMillis);
            fetchColorSeries(organ, start, end, uid);
        });

        // 問答：改走後端 API（不再直連 Ollama）
        btnAskConfirm.setOnClickListener(v -> {
            String question = (etQuestion.getText() != null) ? etQuestion.getText().toString().trim() : "";
            if (TextUtils.isEmpty(question)) {
                Toast.makeText(this, "請先輸入問題", Toast.LENGTH_SHORT).show();
                return;
            }
            askViaBackend(question);
        });

        setupBottomNav();
        setupLineChartStyle();
    }

    /** /api/history/status-bar：多色 0/1 折線資料（維持用 ApiHelper） */
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
                        renderMultiColorLineChart(resp);
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(_cMainActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    // =========================
    // 後端 /api/ollama/ask（由後端再去叫 Ollama）
    // =========================
    private void askViaBackend(String question) {
        btnAskConfirm.setEnabled(false);
        tvAnswer.setText("思考中…");

        // 後端只需要問題字串；系統提示、模型名統一在後端處理
        AskOllamaRequest req = new AskOllamaRequest(question);

        ApiHelper.httpPost(
                OLLAMA_ASK_API_PATH,
                req,
                AskOllamaResponse.class,
                new ApiHelper.ApiCallback<AskOllamaResponse>() {
                    @Override
                    public void onSuccess(AskOllamaResponse resp) {
                        btnAskConfirm.setEnabled(true);
                        if (resp == null) {
                            tvAnswer.setText("");
                            Toast.makeText(_cMainActivity.this, "回應為空", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!resp.success) {
                            tvAnswer.setText("");
                            Toast.makeText(_cMainActivity.this,
                                    (resp.message != null ? resp.message : "伺服器處理失敗"),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        String content = (resp.answer != null) ? resp.answer.trim() : "";
                        tvAnswer.setText(content.isEmpty() ? "（沒有內容）" : content);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        btnAskConfirm.setEnabled(true);
                        tvAnswer.setText("");
                        Toast.makeText(_cMainActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /** 送往後端的最小請求/回應 DTO（就地定義，避免你另外建檔） */
    public static class AskOllamaRequest {
        public String question;
        public AskOllamaRequest() {}
        public AskOllamaRequest(String q) { this.question = q; }
    }
    public static class AskOllamaResponse {
        public boolean success;
        public String answer;
        public String message;
    }

    /** 繪製多色折線（發紅/發黑/發黃/發白/發青 → 5 條 0/1 線） */
    private void renderMultiColorLineChart(HistoryStatusBarResponseDto r) {
        List<String> x = (r.x != null) ? r.x : new ArrayList<>();
        List<String> xShort = toMonthDayList(x);

        final float SHIFT_RED    = -0.24f;
        final float SHIFT_BLACK  = -0.12f;
        final float SHIFT_YELLOW =  0.00f;
        final float SHIFT_WHITE  =  0.12f;
        final float SHIFT_CYAN   =  0.24f;

        List<Entry> red    = toEntries(r.series != null ? r.series.red    : null, SHIFT_RED);
        List<Entry> black  = toEntries(r.series != null ? r.series.black  : null, SHIFT_BLACK);
        List<Entry> yellow = toEntries(r.series != null ? r.series.yellow : null, SHIFT_YELLOW);
        List<Entry> white  = toEntries(r.series != null ? r.series.white  : null, SHIFT_WHITE);
        List<Entry> cyan   = toEntries(r.series != null ? r.series.cyan   : null, SHIFT_CYAN);

        List<ILineDataSet> sets = new ArrayList<>();
        addIfNotAllZero(sets, makeDataSet(red,   "發紅",  Color.parseColor("#E53935")));
        addIfNotAllZero(sets, makeDataSet(black, "發黑",  Color.parseColor("#212121")));
        addIfNotAllZero(sets, makeDataSet(yellow,"發黃",  Color.parseColor("#FBC02D")));
        addIfNotAllZero(sets, makeDataSet(white, "發白",  Color.parseColor("#BDBDBD")));
        addIfNotAllZero(sets, makeDataSet(cyan,  "發青",  Color.parseColor("#26A69A")));

        if (sets.isEmpty()) {
            sets.add(makeDataSet(new ArrayList<>(), "無異常", Color.parseColor("#90A4AE")));
        }

        lineChart.setData(new LineData(sets));

        // X 軸
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(x.size(), 6));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xShort));
        xAxis.setAxisMinimum(-0.6f);
        xAxis.setAxisMaximum(Math.max(0, x.size() - 1) + 0.6f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineWidth(1f);
        xAxis.setAxisLineColor(Color.parseColor("#888888"));

        // Y 軸
        YAxis left = lineChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(1f);
        left.setGranularity(1f);
        left.setLabelCount(2, true);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });
        left.setDrawGridLines(false);
        left.setDrawAxisLine(true);
        left.setAxisLineWidth(1f);
        left.setAxisLineColor(Color.parseColor("#888888"));
        lineChart.getAxisRight().setEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);
        lineChart.getDescription().setEnabled(false);

        tvChartPlaceholder.setVisibility(android.view.View.GONE);
        lineChart.setVisibility(android.view.View.VISIBLE);
        lineChart.invalidate();
    }

    // ===== 小工具 =====

    private List<Entry> toEntries(List<Integer> ys, float xShift) {
        List<Entry> list = new ArrayList<>();
        int n = (ys != null) ? ys.size() : 0;
        for (int i = 0; i < n; i++) {
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
        ds.setCircleRadius(4f);
        ds.setDrawCircleHole(true);
        ds.setCircleHoleRadius(2f);
        ds.setCircleHoleColor(Color.WHITE);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        ds.setDrawFilled(false);
        ds.setHighlightEnabled(true);
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
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
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
                long start = selection.first;
                long end   = selection.second;

                final long DAY_MS = 24L * 60 * 60 * 1000;
                long daysInclusive = (end - start) / DAY_MS + 1; // 含起訖天數

                if (daysInclusive > 7) {
                    long clampedEnd = start + 6 * DAY_MS;
                    Toast.makeText(this, "一次最多只能選 7 天，已自動縮短區間", Toast.LENGTH_SHORT).show();
                    selectedStartMillis = start;
                    selectedEndMillis   = clampedEnd;
                } else {
                    selectedStartMillis = start;
                    selectedEndMillis   = end;
                }

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
        // null-safe，避免 include 失敗造成 NPE
        LinearLayout navA = findViewById(R.id.nav_a);
        LinearLayout navB = findViewById(R.id.nav_b);
        LinearLayout navC = findViewById(R.id.nav_c);
        LinearLayout navD = findViewById(R.id.nav_d);

        if (navA != null) navA.setOnClickListener(v ->
                startActivity(new Intent(this, _aMainActivity.class)));
        if (navB != null) navB.setOnClickListener(v ->
                startActivity(new Intent(this, _bMainActivity.class)));
        if (navC != null) navC.setOnClickListener(v -> {
            // 本頁：不跳轉
        });
        if (navD != null) navD.setOnClickListener(v ->
                startActivity(new Intent(this, _dMainActivity.class)));
    }
    // 將 "2025-09-16" 或 "2025/09/16 14:30" 之類格式轉成 "09/16"
    private String toMonthDay(String s) {
        if (s == null) return "";
        // 抓 yyyy[-/年.]MM[-/月.]dd（後面可能還有時間）
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:19|20)\\d{2}[-/年.](\\d{1,2})[-/月.](\\d{1,2})"
        );
        java.util.regex.Matcher m = p.matcher(s);
        if (m.find()) {
            int mm = Integer.parseInt(m.group(1));
            int dd = Integer.parseInt(m.group(2));
            return String.format(java.util.Locale.TAIWAN, "%02d/%02d", mm, dd);
        }
        // 如果後端已經給「MM/dd」或「MM-dd」就直接回傳
        return s;
    }

    private List<String> toMonthDayList(List<String> src) {
        List<String> out = new ArrayList<>();
        if (src != null) {
            for (String s : src) out.add(toMonthDay(s));
        }
        return out;
    }

}