// utils/auth/AuthStore.java
package com.example.tcmhaa.utils.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class AuthStore {
    private static final String PREF = "auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ACCESS = "access_token";   // 後端若無 JWT，可不存
    private static final String KEY_REFRESH = "refresh_token"; // 同上
    private static final String KEY_EXPIRES_AT = "access_expires_at"; // 毫秒

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveLogin(Context ctx, int userId, String name, String email,
                                 String accessToken, String refreshToken, long expiresAtMs) {
        sp(ctx).edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAtMs)
                .apply();
    }

    public static boolean isLoggedIn(Context ctx) {
        int uid = sp(ctx).getInt(KEY_USER_ID, -1);
        // 若有 token/過期時間可一起判斷
        String token = sp(ctx).getString(KEY_ACCESS, null);
        long exp = sp(ctx).getLong(KEY_EXPIRES_AT, 0L);

        // 沒 JWT 的專案，可單純以 user_id 是否存在為準
        if (uid > 0) {
            // 若你有 JWT，就再檢查是否過期
            if (!TextUtils.isEmpty(token) && exp > 0) {
                return System.currentTimeMillis() < exp;
            }
            return true;
        }
        return false;
    }

    public static void logout(Context ctx) {
        sp(ctx).edit().clear().apply();
    }

    public static int userId(Context ctx) {
        return sp(ctx).getInt(KEY_USER_ID, -1);
    }
}
