package com.example.tcmhaa.utils.toast;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

public final class ToastHelper {
    private static volatile Toast sToast;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ToastHelper() {}

    public static void show(@NonNull Context context, @NonNull CharSequence msg, int duration) {
        Context appCtx = context.getApplicationContext();
        MAIN.post(() -> {
            if (sToast != null) sToast.cancel();
            sToast = Toast.makeText(appCtx, msg, duration);
            sToast.show();
        });
    }
}
