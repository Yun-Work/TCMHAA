package com.example.tcmhaa;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tcmhaa.utils.auth.AuthStore;

public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = new Intent(
                this,
                AuthStore.isLoggedIn(this) ? WelcomeActivity.class : LoginActivity.class
        );
        // 清任務棧，避免返回回到登入/啟動頁
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
