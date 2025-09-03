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
    private static final int NOTI_ID = 5001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1) Android 13+ 需要 POST_NOTIFICATIONS 權限，這裡僅檢查（Receiver 不能發起 runtime 要求）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
            );
            if (granted != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted; skip notify()");
                return; // 直接跳過，避免 SecurityException
            }
        }

        // 2) 檢查是否被使用者關閉了 App 通知
        NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
        if (!nmc.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled by user; skip notify()");
            return;
        }

        // 3) 建立通知頻道（Android 8.0+）
        createChannelIfNeeded(context);

        // 4) 點通知後要開啟的頁面（依你的需求替換）
        Intent open = new Intent(context, MainhealthyActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPI = PendingIntent.getActivity(
                context,
                2001,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 5) 建立通知內容
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // ← 請替換成你專案存在的小圖示
                .setContentTitle("TCMHAA 每日提醒")
                .setContentText("來使用本日的健康偵測/分析吧！")
                .setContentIntent(contentPI)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // 6) 安全地送出通知
        try {
            nmc.notify(NOTI_ID, builder.build());
        } catch (SecurityException se) {
            // 萬一裝置廠商有客製權限策略，這裡再保護一次
            Log.e(TAG, "notify() SecurityException: " + se.getMessage());
        }
    }

    private void createChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "每日提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("每天在指定時間提醒你回到 App");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
