package com.example.tcmhaa;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tcmhaa.dto.LoginRequestDto;
import com.example.tcmhaa.dto.LoginResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    // 偏好設定：逐權限紀錄
    private static final String PREFS = "perm_prefs";
    private static final String KEY_ASKED_PREFIX = "asked_";          // asked_<permission>
    private static final String KEY_PERM_DENY_PERM_PREFIX = "deny_";   // deny_<permission> (永久拒絕旗標)

    private EditText etUsername, etPassword;
    private Button btnNext, btnRegister;
    private TextView tvForgotPassword;

    // 1) 請求多個權限
    private final ActivityResultLauncher<String[]> requestPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionsResult);

    // 2) 從系統設定返回後再檢查一次
    private final ActivityResultLauncher<Intent> appSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // 回來後再檢查是否還有缺
                String[] missing = buildMissingPermissions();
                if (missing.length == 0) {
                    Toast.makeText(this, "已啟用必要權限。", Toast.LENGTH_SHORT).show();
                    goWelcome();
                } else {
                    // 仍缺，視需求決定要不要再次提示或留在本頁
                    Toast.makeText(this, "仍缺少權限：" + Arrays.toString(missing), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_1);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnNext    = findViewById(R.id.btnNext);
        btnRegister= findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // 登入
        btnNext.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            btnNext.setEnabled(false);

            ApiHelper.httpPost(
                    "users/login",
                    new LoginRequestDto(email, password),
                    LoginResponseDto.class,
                    new ApiHelper.ApiCallback<>() {
                        @Override
                        public void onSuccess(LoginResponseDto resp) {
                            btnNext.setEnabled(true);

                            if (resp == null || !resp.success) {
                                Toast.makeText(LoginActivity.this, "帳號或密碼錯誤", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (resp.user != null) {
                                // 先拿到同一個 prefs 物件
                                SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);

                                // 寫入 user_id / name / email
                                sp.edit()
                                        .putInt("user_id", resp.user.user_id)
                                        .putString("name",  resp.user.name)
                                        .putString("email", resp.user.email)
                                        .apply();

                                // ★ 就加在這裡：立刻讀回來印出，確認有存成功
                                int saved = sp.getInt("user_id", -1);
                                Log.d("LOGIN", "saved user_id = " + saved);
                            }
                            Toast.makeText(LoginActivity.this,
                                    resp.message != null ? resp.message : "登入成功",
                                    Toast.LENGTH_SHORT).show();

                            // 登入成功 → 檢查權限（仍缺就彈，已有就直接進）
                            maybeRequestRuntimePermsThenGo();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            btnNext.setEnabled(true);
                            Log.e(TAG, "login fail: " + t.getMessage(), t);
                            Toast.makeText(LoginActivity.this, "連線錯誤：" + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        // 前往註冊
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, CheckActivity.class);
            i.putExtra("prefill_email", etUsername.getText().toString().trim());
            startActivity(i);
        });

        // 忘記密碼
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgetActivity.class))
        );
    }

    /** 登入後：若有缺權限 → 彈窗；若都足夠 → 進首頁 */
    private void maybeRequestRuntimePermsThenGo() {
        String[] missing = buildMissingPermissions();
        Log.d(TAG, "missing=" + Arrays.toString(missing));

        if (missing.length == 0) {
            goWelcome();
            return;
        }
        // 檢查是否有「永久拒絕」的權限（包含使用者勾不再詢問）
        List<String> permanentlyDenied = new ArrayList<>();
        for (String p : missing) {
            if (isPermissionPermanentlyDenied(p)) {
                permanentlyDenied.add(p);
            }
        }

        if (!permanentlyDenied.isEmpty()) {
            // 有永久拒絕 → 導去設定
            showGoSettingsDialog(permanentlyDenied);
        } else {
            // 還沒永久拒絕 → 直接彈系統請求
            markAsked(missing); // 記錄這些權限已詢問過一次
            requestPermsLauncher.launch(missing);
        }
    }

    /** 依系統版本回傳需要的權限；再過濾目前仍缺的 */
    private String[] buildMissingPermissions() {
        List<String> need = new ArrayList<>();
        // 相機
        need.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            need.add(Manifest.permission.READ_MEDIA_IMAGES);
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // 過濾：只保留尚未授權的
        List<String> missing = new ArrayList<>();
        for (String p : need) {
            if (ContextCompat.checkSelfPermission(this, p) != PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        return missing.toArray(new String[0]);
    }

    /** 處理使用者點選允許/拒絕後的結果 */
    private void onPermissionsResult(Map<String, Boolean> result) {
        // 更新「永久拒絕」旗標
        for (Map.Entry<String, Boolean> e : result.entrySet()) {
            String perm = e.getKey();
            boolean granted = Boolean.TRUE.equals(e.getValue());

            if (!granted) {
                // 若 shouldShowRequestPermissionRationale 為 false，代表勾了「不再詢問」或系統封鎖
                boolean rationale = shouldShowRequestPermissionRationaleCompat(perm);
                if (!rationale) {
                    setPermanentlyDenied(perm, true);
                }
            }
        }

        String[] missing = buildMissingPermissions();
        if (missing.length == 0) {
            Toast.makeText(this, "權限已授予。", Toast.LENGTH_SHORT).show();
            goWelcome();
            return;
        }

        // 仍缺 → 有些可能被永久拒絕了
        List<String> permanentlyDenied = new ArrayList<>();
        for (String p : missing) {
            if (isPermissionPermanentlyDenied(p)) {
                permanentlyDenied.add(p);
            }
        }

        if (!permanentlyDenied.isEmpty()) {
            showGoSettingsDialog(permanentlyDenied);
        } else {
            Toast.makeText(this, "部分權限被拒絕，可於系統設定稍後開啟。", Toast.LENGTH_SHORT).show();
            // 視需求：若核心權限（例如相機）很重要，可直接留在本頁；這裡示範仍可放行
            goWelcome();
        }
    }

    /** 檢查 shouldShowRequestPermissionRationale 的兼容封裝 */
    private boolean shouldShowRequestPermissionRationaleCompat(@NonNull String permission) {
        // Android 13+ 的 POST_NOTIFICATIONS 也支援 rationale；其餘權限同理
        return shouldShowRequestPermissionRationale(permission);
    }

    /** 是否被永久拒絕：曾經詢問過 + 目前未授權 + 不應顯示解釋（代表勾了不再詢問或系統封鎖）或已被我們標記 */
    private boolean isPermissionPermanentlyDenied(String permission) {
        boolean notGranted = ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED;
        boolean asked = getPrefs().getBoolean(KEY_ASKED_PREFIX + permission, false);
        boolean rationale = shouldShowRequestPermissionRationaleCompat(permission);
        boolean flagged = getPrefs().getBoolean(KEY_PERM_DENY_PERM_PREFIX + permission, false);
        return notGranted && asked && (!rationale || flagged);
    }

    /** 對話框：引導去系統設定頁開啟權限 */
    private void showGoSettingsDialog(List<String> perms) {
        String msg = "以下權限被永久拒絕或停用：\n" + permsToReadable(perms) +
                "\n請前往系統設定 > 應用程式 > 本 App > 權限，手動開啟後再回來。";

        new AlertDialog.Builder(this)
                .setTitle("需要權限")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("前往設定", (dialog, which) -> openAppSettings())
                .setNegativeButton("稍後再說", (dialog, which) -> {
                    // 視需求可導向首頁或留在本頁
                    goWelcome();
                })
                .show();
    }
    /** 打開本 App 的系統設定頁 */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            appSettingsLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            // 後備：開一般設定
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            appSettingsLauncher.launch(intent);
        }
    }

    /** 標記這批權限為「已詢問過」 */
    private void markAsked(String[] permissions) {
        SharedPreferences.Editor ed = getPrefs().edit();
        for (String p : permissions) {
            ed.putBoolean(KEY_ASKED_PREFIX + p, true);
        }
        ed.apply();
    }

    /** 設置永久拒絕旗標（當 onResult 判斷 rationale=false 時可標記） */
    private void setPermanentlyDenied(String permission, boolean denied) {
        getPrefs().edit().putBoolean(KEY_PERM_DENY_PERM_PREFIX + permission, denied).apply();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    /** 將權限代碼轉成易讀行（可自行客製） */
    private String permsToReadable(List<String> perms) {
        StringBuilder sb = new StringBuilder();
        for (String p : perms) {
            switch (p) {
                case Manifest.permission.CAMERA:
                    sb.append("• 相機權限\n"); break;
                case Manifest.permission.READ_MEDIA_IMAGES:
                    sb.append("• 讀取圖片（Android 13+）\n"); break;
                case Manifest.permission.POST_NOTIFICATIONS:
                    sb.append("• 發送通知（Android 13+）\n"); break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                    sb.append("• 讀取儲存空間（Android 12-）\n"); break;
                default:
                    sb.append("• ").append(p).append("\n");
            }
        }
        return sb.toString();
    }

    private void goWelcome() {
        Intent i = new Intent(LoginActivity.this, WelcomeActivity.class);
        startActivity(i);
        finish();
    }
}