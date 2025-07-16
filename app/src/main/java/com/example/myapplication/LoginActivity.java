package com.example.myapplication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etBirthday;
    RadioGroup genderGroup;
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etBirthday = findViewById(R.id.etBirthday);
        genderGroup = findViewById(R.id.genderGroup);
        btnNext = findViewById(R.id.btnNext);

        etBirthday.setOnClickListener(v -> showDatePicker());

        btnNext.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String birthday = etBirthday.getText().toString();
            int genderId = genderGroup.getCheckedRadioButtonId();

            if (username.isEmpty() || birthday.isEmpty() || genderId == -1) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "歡迎 " + username, Toast.LENGTH_SHORT).show();
                // 可以加上跳轉到主畫面 MainActivity
            }
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR), month = calendar.get(Calendar.MONTH), day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) ->
                etBirthday.setText(y + "-" + (m + 1) + "-" + d),
                year, month, day).show();
    }
}
