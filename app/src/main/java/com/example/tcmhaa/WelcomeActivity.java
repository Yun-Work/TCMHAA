package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class WelcomeActivity extends AppCompatActivity {

    private Button nextButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_4);

        nextButton = findViewById(R.id.btn_next);
        nextButton.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, MainhealthyActivity.class)));

        maybeShowPermissionsDialog();
    }

    private void maybeShowPermissionsDialog() {
        // ★ 只要顯示過一次，就不再顯示
        if (!PrefsHelper.isPermissionsDialogShownOnce(this)) {
            nextButton.setEnabled(false);

            PermissionsDialogFragment dialog = new PermissionsDialogFragment();
            dialog.setOnAllGrantedListener(() -> nextButton.setEnabled(true));
            dialog.setCancelable(false);
            dialog.show(getSupportFragmentManager(), "perms");

            // ★ 記錄：已顯示過一次（無論是否授權成功）
            PrefsHelper.setPermissionsDialogShownOnce(this, true);
        } else {
            nextButton.setEnabled(true);
        }
    }
}
