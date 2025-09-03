package com.example.tcmhaa;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class _dPrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_privacy);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 最後更新日期（自動帶今天）
        TextView tvLast = findViewById(R.id.tvLastUpdated);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN).format(new Date());
        tvLast.setText("最後更新：" + today);

        // 主要內文（使用 HTML 格式排版）
        TextView tvBody = findViewById(R.id.tvBody);
        tvBody.setText(HtmlCompat.fromHtml(getString(R.string.privacy_policy_body),
                HtmlCompat.FROM_HTML_MODE_LEGACY));
        tvBody.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
