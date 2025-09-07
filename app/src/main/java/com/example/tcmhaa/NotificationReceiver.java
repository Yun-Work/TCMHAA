package com.example.tcmhaa;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private static final String CHANNEL_ID = "daily_reminder_channel";
    private static final int ID_DAILY  = 5001;
    private static final int ID_ONESHOT = 5002;

    // 與 _dTimeActivity 對齊
    private static final String PREFS = "reminder_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MIN = "min";

    @Override
    public void onReceive(Context context, Intent intent) {
        // --- 0) 權限/通知開關檢查 ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted; skip notify()");
                return;
            }
        }
        NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
        if (!nmc.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled by user; skip notify()");
            return;
        }
        createChannelIfNeeded(context);

        // --- 1) 決定 action 分流 ---
        String action = intent != null ? intent.getAction() : null;
        if (ReminderScheduler.ACTION_ONESHOT.equals(action)) {
            // 一次性測試
            String msg = intent.getStringExtra("message");
            PendingIntent pi = buildContentPI(context);
            show(nmc, context, ID_ONESHOT, "測試通知", msg != null ? msg : "這是一則測試通知", pi);

            return; // oneshot 不需重排
        }

        // 預設/每日提醒
        PendingIntent pi = buildContentPI(context);
        show(nmc, context, ID_DAILY, "TCMHAA 每日提醒", "來使用本日的健康偵測/分析吧！", pi);

        // --- 2) 若採用「精準每日」：觸發後重排隔天 ---
        // 若你是用 setInexactRepeating()，下面這段可以不做。
        var sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean enabled = sp.getBoolean(KEY_ENABLED, false);
        int hour = sp.getInt(KEY_HOUR, 9);
        int min  = sp.getInt(KEY_MIN, 0);
        if (enabled) {
            // 這行只在你用 ReminderScheduler.scheduleDailyExact(...) 的情況下需要
            ReminderScheduler.scheduleDailyExact(context, hour, min);
        }
    }

    private PendingIntent buildContentPI(Context context) {
        // 點通知後要開啟的頁面（可改成 _dMainActivity / LoginActivity 等）
        Intent open = new Intent(context, MainhealthyActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context, 2001, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void show(NotificationManagerCompat nmc, Context ctx,
                      int id, String title, String text, PendingIntent contentPI) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ✅ 用實心單色 24dp 圖示，避免用 launcher_foreground
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentPI)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            nmc.notify(id, b.build());
        } catch (SecurityException se) {
            Log.e(TAG, "notify() SecurityException: " + se.getMessage());
        }
    }

    private void createChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "每日提醒", NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("每天在指定時間提醒你回到 App");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
