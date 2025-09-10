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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    // API
    private static final String HISTORY_API_PATH_COLOR_SERIES = "history/status-bar"; // 走 ApiHelper（自帶 /api 前綴）
    private static final String API_BASE = "http://10.0.2.2:6060/api/"; // 直連 OkHttp 用
    private static final String RAG_WEB_ASK_API_PATH = "rag/web-ask";   // 若你沒有 web-ask，可改成 "rag/ask"

    // OkHttp（拉長讀取逾時給 RAG 用）
    private static final MediaType JSON_MT = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient ragClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)   // 依後端推論時間調整
            .callTimeout(0, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        // 綁定元件（已無 btnDownload）
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

        // 問答：呼叫 /api/rag/web-ask（或 /api/rag/ask）
        btnAskConfirm.setOnClickListener(v -> {
            String question = (etQuestion.getText() != null) ? etQuestion.getText().toString().trim() : "";
            if (TextUtils.isEmpty(question)) {
                Toast.makeText(this, "請先輸入問題", Toast.LENGTH_SHORT).show();
                return;
            }
            askWebAsk(question);
        });

        setupBottomNav();     // 已做 null-safe
        setupLineChartStyle();
    }

    /** /api/history/status-bar：多色 0/1 折線資料 */
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
    // /api/rag/web-ask（或 rag/ask）
    // =========================
    private void askWebAsk(String question) {
        btnAskConfirm.setEnabled(false);
        tvAnswer.setText("從網路蒐集資料中…");

        try {
            JSONObject body = new JSONObject();
            body.put("question", question);

            Request req = new Request.Builder()
                    .url(API_BASE + RAG_WEB_ASK_API_PATH)
                    .post(RequestBody.create(body.toString(), JSON_MT))
                    .build();

            ragClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnAskConfirm.setEnabled(true);
                        tvAnswer.setText("");
                        Toast.makeText(_cMainActivity.this, "發問失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override public void onResponse(Call call, Response response) {
                    String respStr = "";
                    try { respStr = response.body() != null ? response.body().string() : ""; }
                    catch (Exception ignore) {}
                    finally { if (response.body() != null) response.close(); }

                    final boolean ok = response.isSuccessful();
                    final String finalResp = respStr;

                    runOnUiThread(() -> {
                        btnAskConfirm.setEnabled(true);

                        if (!ok) {
                            tvAnswer.setText("");
                            Toast.makeText(_cMainActivity.this, "伺服器錯誤：" + response.code(), Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            JSONObject obj = new JSONObject(finalResp);
                            // web-ask: {success, answer, sources: [{title,url}]}
                            boolean success = obj.optBoolean("success", true); // 若是 /rag/ask 可能沒有 success
                            String answer = obj.optString("answer", finalResp);

                            StringBuilder sb = new StringBuilder();
                            if (!TextUtils.isEmpty(answer)) sb.append(answer.trim());

                            JSONArray srcs = obj.optJSONArray("sources");
                            if (srcs != null && srcs.length() > 0) {
                                sb.append("\n\n參考來源：\n");
                                for (int i = 0; i < srcs.length(); i++) {
                                    JSONObject s = srcs.optJSONObject(i);
                                    if (s == null) continue;
                                    String title = s.optString("title", "來源 " + (i + 1));
                                    String url   = s.optString("url", "");
                                    sb.append(i + 1).append(". ").append(title);
                                    if (!TextUtils.isEmpty(url)) sb.append("\n").append(url);
                                    sb.append("\n");
                                }
                            }

                            tvAnswer.setText(sb.toString().trim());
                        } catch (Exception e) {
                            // 非預期 JSON，直接顯示原字串
                            tvAnswer.setText(finalResp);
                        }
                    });
                }
            });

        } catch (Exception e) {
            btnAskConfirm.setEnabled(true);
            tvAnswer.setText("");
            Toast.makeText(this, "參數錯誤：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 繪製多色折線（發紅/發黑/發黃/發白/發青 → 5 條 0/1 線） */
    private void renderMultiColorLineChart(HistoryStatusBarResponseDto r) {
        List<String> x = (r.x != null) ? r.x : new ArrayList<>();

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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(x));
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
}
