package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class _cMainActivity extends AppCompatActivity {

    private Spinner spOrgan;
    private Button btnPickRange, btnConfirm;
    private ImageButton btnDownload;

    // 新增：問答區元件
    private EditText etQuestion;
    private Button btnAskConfirm;
    private TextView tvAnswer;

    // 選擇的日期區間
    private Long selectedStartMillis;
    private Long selectedEndMillis;

    // 後端 API（依你的實際路徑調整）
    private static final String HISTORY_API = "http://10.0.2.2:6060/api/history";   // TODO: 改成你的實際端點
    private static final String RAG_ASK_API = "http://10.0.2.2:6060/api/rag/ask";    // TODO: 改成你的實際端點
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // ← 最重要：拉長讀取逾時
            .callTimeout(0, TimeUnit.SECONDS)     // ← 不設整體逾時（可視需要設 180s）
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainhealthy_c);

        spOrgan      = findViewById(R.id.spOrgan);
        btnPickRange = findViewById(R.id.btnPickRange);
        btnConfirm   = findViewById(R.id.btnConfirm);
        btnDownload  = findViewById(R.id.btnDownload);

        // 綁定新UI
        etQuestion    = findViewById(R.id.etQuestion);
        btnAskConfirm = findViewById(R.id.btnAskConfirm);
        tvAnswer      = findViewById(R.id.tvAnswer);

        // 下拉選單資料
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.organs_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrgan.setAdapter(adapter);

        // 預設日期：最近7天
        Pair<Long, Long> last7Days = getLast7Days();
        selectedStartMillis = last7Days.first;
        selectedEndMillis   = last7Days.second;
        btnPickRange.setText(formatDate(selectedStartMillis) + " ~ " + formatDate(selectedEndMillis));

        btnPickRange.setOnClickListener(v -> showDateRangePicker());

        // 下載（你原本的邏輯）
        btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "這裡執行匯出/下載", Toast.LENGTH_SHORT).show());

        // 查歷史資料（保留原邏輯）
        btnConfirm.setOnClickListener(v -> {
            if (selectedStartMillis == null || selectedEndMillis == null) {
                Toast.makeText(this, "請先選擇時間範圍", Toast.LENGTH_SHORT).show();
                return;
            }
            String organ = spOrgan.getSelectedItem() == null
                    ? "" : spOrgan.getSelectedItem().toString();
            String startDate = formatDate(selectedStartMillis);
            String endDate   = formatDate(selectedEndMillis);

            fetchHistory(organ, startDate, endDate);
        });

        // 送出問題給後端 RAG
        btnAskConfirm.setOnClickListener(v -> {
            String question = etQuestion.getText() != null ? etQuestion.getText().toString().trim() : "";
            if (TextUtils.isEmpty(question)) {
                Toast.makeText(this, "請先輸入問題", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedStartMillis == null || selectedEndMillis == null) {
                Toast.makeText(this, "請先選擇時間範圍", Toast.LENGTH_SHORT).show();
                return;
            }
            String organ = spOrgan.getSelectedItem() == null
                    ? "" : spOrgan.getSelectedItem().toString();
            String startDate = formatDate(selectedStartMillis);
            String endDate   = formatDate(selectedEndMillis);

            askRag(question, organ, startDate, endDate);
        });

        setupBottomNav();
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

    /** 呼叫後端取歷史資料（器官 + 起訖日期） */
    private void fetchHistory(String organ, String startDate, String endDate) {
        try {
            JSONObject body = new JSONObject();
            body.put("organ", organ);
            body.put("start_date", startDate);
            body.put("end_date", endDate);

            Request request = new Request.Builder()
                    .url(HISTORY_API)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            http.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(_cMainActivity.this, "連線失敗：" + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(_cMainActivity.this, "查詢失敗：" + response.code(), Toast.LENGTH_LONG).show());
                        return;
                    }
                    final String resp = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        // TODO: 將 resp 解析後，更新 blockChart
                        Toast.makeText(_cMainActivity.this, "已載入歷史資料", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "參數錯誤：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 問 RAG：question + organ + date range */
    private void askRag(String question, String organ, String startDate, String endDate) {
        // UI 狀態
        btnAskConfirm.setEnabled(false);
        tvAnswer.setText("思考中…");

        try {
            JSONObject body = new JSONObject();
            body.put("question", question);
            body.put("organ", organ);
            body.put("start_date", startDate);
            body.put("end_date", endDate);

            Request request = new Request.Builder()
                    .url(RAG_ASK_API)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            http.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnAskConfirm.setEnabled(true);
                        tvAnswer.setText("");
                        Toast.makeText(_cMainActivity.this, "發問失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    final String resp = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        btnAskConfirm.setEnabled(true);
                        String answer = resp;
                        try {
                            JSONObject obj = new JSONObject(resp);
                            // 後端 rag_routes.py /rag/ask 回 {"answer": "..."}
                            answer = obj.optString("answer", resp);
                        } catch (Exception ignore) { }
                        tvAnswer.setText(answer != null ? answer : "");
                    });
                }
            });

        } catch (JSONException e) {
            btnAskConfirm.setEnabled(true);
            tvAnswer.setText("");
            Toast.makeText(this, "參數錯誤：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 最近 7 天 */
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
