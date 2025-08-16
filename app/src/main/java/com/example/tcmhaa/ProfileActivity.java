package com.example.tcmhaa;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEditText, birthdayEditText;
    private RadioGroup genderGroup;
    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_3);

        nameEditText = findViewById(R.id.nameEditText);
        birthdayEditText = findViewById(R.id.birthdayEditText);
        genderGroup = findViewById(R.id.genderGroup);
        confirmButton = findViewById(R.id.confirmButton);

        birthdayEditText.setOnClickListener(v -> showDatePicker());

        confirmButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String birthday = birthdayEditText.getText().toString().trim();
            int genderId = genderGroup.getCheckedRadioButtonId();

            if (name.isEmpty() || birthday.isEmpty() || genderId == -1) {
                Toast.makeText(ProfileActivity.this, "請填寫完整資料", Toast.LENGTH_SHORT).show();
                return;
            }

            String gender = (genderId == R.id.radioMale) ? "男" : "女";

            // 寫入：已完成首次登入資料
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_login", false)
                    .apply();

            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("birthday", birthday);
            intent.putExtra("gender", gender);
            startActivity(intent);
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> birthdayEditText.setText(y + "/" + (m + 1) + "/" + d),
                year, month, day
        );
        datePickerDialog.show();
    }
}
