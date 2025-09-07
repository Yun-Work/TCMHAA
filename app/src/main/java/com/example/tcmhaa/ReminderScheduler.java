package com.example.tcmhaa;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * 支援：
 * 1) 不精準每日：scheduleDailyInexact(...)
 * 2) 精準每日：scheduleDailyExact(...)  (Doze 下仍盡力準點；需在 Receiver 內「隔天重排」)
 * 3) 一次性測試：scheduleOneShotInSeconds(...)
 * 4) 相容舊呼叫：scheduleDailyReminder(...) / cancelDailyReminder(...)
 */
public final class ReminderScheduler {

    // Action 與 reqCode 統一管理
    public static final String ACTION_DAILY = "com.example.tcmhaa.ACTION_DAILY_REMINDER";
    public static final String ACTION_ONESHOT = "com.example.tcmhaa.ACTION_ONESHOT";

    private static final int REQ_DAILY = 1001;
    private static final int REQ_ONESHOT = 1002;

    private ReminderScheduler() {}

    /* ------------------------ 共用工具 ------------------------ */

    private static PendingIntent buildDailyPI(Context ctx) {
        Intent i = new Intent(ctx, NotificationReceiver.class).setAction(ACTION_DAILY);
        return PendingIntent.getBroadcast(
                ctx, REQ_DAILY, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildOneShotPI(Context ctx, String message) {
        Intent i = new Intent(ctx, NotificationReceiver.class)
                .setAction(ACTION_ONESHOT)
                .putExtra("message", message);
        return PendingIntent.getBroadcast(
                ctx, REQ_ONESHOT, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /** 算下一次觸發時間（今天已過就 +1 天） */
    private static long nextTriggerAt(int hour24, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, minute);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    private static void setExactCompat(AlarmManager am, long triggerAt, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /* ------------------------ 不精準每日（保留） ------------------------ */

    /** 不精準每日（系統可能延遲，優點：不用精準權限、省電） */
    public static void scheduleDailyInexact(Context ctx, int hour24, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildDailyPI(ctx);
        long triggerAt = nextTriggerAt(hour24, minute);
        if (am != null) {
            am.cancel(pi);
            am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    AlarmManager.INTERVAL_DAY,
                    pi
            );
        }
    }

    /* ------------------------ 精準每日（建議） ------------------------ */

    /**
     * 精準每日：今天排一次；觸發後請在 NotificationReceiver 內再呼叫本方法排隔天。
     * Android 12+ 若需要準點，建議要求 SCHEDULE_EXACT_ALARM。
     */
    public static void scheduleDailyExact(Context ctx, int hour24, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildDailyPI(ctx);
        long triggerAt = nextTriggerAt(hour24, minute);
        if (am != null) {
            am.cancel(pi);
            setExactCompat(am, triggerAt, pi);
        }
    }

    /* ------------------------ 一次性測試 ------------------------ */

    /** N 秒後打一發，用來驗證通知/Receiver 流程 */
    public static void scheduleOneShotInSeconds(Context ctx, int seconds, String message) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildOneShotPI(ctx, message);
        long triggerAt = System.currentTimeMillis() + (seconds * 1000L);
        if (am != null) {
            setExactCompat(am, triggerAt, pi);
        }
    }

    /* ------------------------ 取消 ------------------------ */

    public static void cancelDaily(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildDailyPI(ctx);
        if (am != null) am.cancel(pi);
    }

    /* ------------------------ 舊名稱相容（別名） ------------------------ */

    /** 舊版相容：預設用精準每日 */
    public static void scheduleDailyReminder(Context ctx, int hour24, int minute) {
        scheduleDailyExact(ctx, hour24, minute);
    }

    /** 舊版相容：取消每日提醒 */
    public static void cancelDailyReminder(Context ctx) {
        cancelDaily(ctx);
    }
}
