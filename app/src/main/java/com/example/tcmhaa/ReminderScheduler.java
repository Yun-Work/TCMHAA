package com.example.tcmhaa;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class ReminderScheduler {

    private static PendingIntent buildPendingIntent(Context ctx) {
        Intent i = new Intent(ctx, NotificationReceiver.class);
        i.setAction("DAILY_REMINDER");
        return PendingIntent.getBroadcast(
                ctx,
                1001,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void scheduleDailyReminder(Context ctx, int hour24, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(ctx);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, minute);

        // 若今天已過該時間，改為明天
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 每天重複（不需精準權限）
        if (am != null) {
            am.cancel(pi);
            am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pi
            );
        }
    }

    public static void cancelDailyReminder(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(ctx);
        if (am != null) {
            am.cancel(pi);
        }
    }
}
