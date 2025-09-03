package com.example.tcmhaa;

import android.content.Context;
import android.content.SharedPreferences;
public class PrefsHelper {
    private static final String PREFS = "tcmhaa_prefs";
    private static final String KEY_PERMS_DONE = "perms_onboard_done";
    private static final String KEY_PERMS_SHOWN_ONCE = "perms_shown_once"; // ★ 新增

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // —— 是否完成授權 —— //
    public static boolean isPermissionsOnboardDone(Context c) {
        return sp(c).getBoolean(KEY_PERMS_DONE, false);
    }
    public static void setPermissionsOnboardDone(Context c, boolean done) {
        sp(c).edit().putBoolean(KEY_PERMS_DONE, done).apply();
    }

    // —— 是否顯示過一次 —— //
    public static boolean isPermissionsDialogShownOnce(Context c) {
        return sp(c).getBoolean(KEY_PERMS_SHOWN_ONCE, false);
    }
    public static void setPermissionsDialogShownOnce(Context c, boolean shown) {
        sp(c).edit().putBoolean(KEY_PERMS_SHOWN_ONCE, shown).apply();
    }

    public static void resetPermissionsOnboard(Context c) {
        sp(c).edit()
                .remove(KEY_PERMS_DONE)
                .remove(KEY_PERMS_SHOWN_ONCE)
                .apply();
    }
}
