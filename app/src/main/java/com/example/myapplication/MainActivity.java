package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 123; // 任意值
    private boolean requestCamera = false;
    private boolean requestStorage = false;
    private boolean requestNotification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btn_showDialog); // 要在 layout 加這顆按鈕
        btn.setOnClickListener(v -> showPermissionDialog());
    }

    private void showPermissionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permissions, null);
        CheckBox cbCamera = dialogView.findViewById(R.id.checkbox_camera);
        CheckBox cbStorage = dialogView.findViewById(R.id.checkbox_storage);
        CheckBox cbNotify = dialogView.findViewById(R.id.checkbox_notify);

        new AlertDialog.Builder(this)
                .setTitle("請選擇需要的權限")
                .setView(dialogView)
                .setPositiveButton("確認", (dialog, which) -> {
                    requestCamera = cbCamera.isChecked();
                    requestStorage = cbStorage.isChecked();
                    requestNotification = cbNotify.isChecked();

                    checkAndRequestPermissions();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void checkAndRequestPermissions() {
        String[] permissionsToRequest = new String[]{
                Manifest.permission.CAMERA,
                Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS
        };

        // 根據勾選過濾權限
        java.util.List<String> neededPermissions = new java.util.ArrayList<>();
        if (requestCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Manifest.permission.CAMERA);
        if (requestStorage && ContextCompat.checkSelfPermission(this,
                Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE);
        if (requestNotification && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS);

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), REQUEST_CODE);
        } else {
            Toast.makeText(this, "所有權限已授予", Toast.LENGTH_SHORT).show();
        }
    }

    // 接收使用者回應
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            StringBuilder granted = new StringBuilder("已授予：\n");
            StringBuilder denied = new StringBuilder("被拒絕：\n");

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    granted.append("✔ ").append(permissions[i]).append("\n");
                else
                    denied.append("✘ ").append(permissions[i]).append("\n");
            }

            Toast.makeText(this, granted.toString() + "\n" + denied, Toast.LENGTH_LONG).show();
        }
    }
}
