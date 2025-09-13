package com.example.tcmhaa;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tcmhaa.dto.GetProfileRequestDto;
import com.example.tcmhaa.dto.UpdateProfileRequestDto;
import com.example.tcmhaa.dto.ProfileResponseDto;
import com.example.tcmhaa.utils.api.ApiHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class _dProfileActivity extends AppCompatActivity {

    private EditText etName, etBirthday;
    private RadioGroup rgGender;

    // 原始值（用來判斷是否有修改）
    private String originName = "", originGender = "", originBirth = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_profile);

        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnBack = findViewById(R.id.btnBackProfile);
        etName = findViewById(R.id.etName);
        etBirthday = findViewById(R.id.etBirthday);
        rgGender = findViewById(R.id.rgGender);

        // 日期選擇器
        etBirthday.setOnClickListener(v -> showDatePicker());

        // 進頁就撈個資
        loadProfile();

        // 儲存
        btnSave.setOnClickListener(v -> submitUpdate());

        // 返回
        btnBack.setOnClickListener(v -> finish());
    }

    /** 叫後端取得個資（POST /api/users/get_profile） */
    private void loadProfile() {
        int uid = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
        if (uid <= 0) { toast("未登入"); finish(); return; }

        ApiHelper.httpPost(
                "users/get_profile",
                new GetProfileRequestDto(uid),
                ProfileResponseDto.class,
                new ApiHelper.ApiCallback<ProfileResponseDto>() {
                    @Override
                    public void onSuccess(ProfileResponseDto u) {
                        if (u == null) { toast("讀取失敗"); return; }

                        // 記住原始值
                        originName   = safe(u.name);
                        originGender = safe(u.gender);
                        originBirth  = safe(u.birthDate); // YYYY-MM-DD

                        // 填入畫面（顯示 YYYY/MM/DD）
                        etName.setText(originName);
                        etBirthday.setText(toUiDate(originBirth));
                        setGenderByApiValue(originGender);
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        toast("連線錯誤：" + t.getMessage());
                    }
                }
        );
    }

    /** 叫後端更新個資（POST /api/users/update_profile） */
    private void submitUpdate() {
        int uid = getSharedPreferences("auth", MODE_PRIVATE).getInt("user_id", -1);
        if (uid <= 0) { toast("未登入"); return; }

        String name     = etName.getText().toString().trim();
        String genderUi = getSelectedGenderUi();                 // "男" / "女" / ""
        String birthUi  = etBirthday.getText().toString().trim();// YYYY/MM/DD 或空

        String apiGender = uiGenderToApi(genderUi);              // "male"/"female"/""
        String apiBirth  = toApiDate(birthUi);                   // YYYY-MM-DD 或空

        boolean changed = !(Objects.equals(name, originName)
                && Objects.equals(apiGender, originGender)
                && Objects.equals(apiBirth, originBirth));
        if (!changed) { toast("沒有變更"); return; }

        // 只帶有更動的欄位；沒改就留 null（後端視為不更新）
        UpdateProfileRequestDto req = new UpdateProfileRequestDto(uid);
        if (!isBlank(name)      && !Objects.equals(name, originName))       req.name = name;
        if (!isBlank(apiGender) && !Objects.equals(apiGender, originGender)) req.gender = apiGender;
        if (!isBlank(apiBirth)  && !Objects.equals(apiBirth, originBirth))   req.birthDate = apiBirth;

        ApiHelper.httpPost(
                "users/update_profile",
                req,
                ProfileResponseDto.class,   // 後端已改成回傳純使用者物件
                new ApiHelper.ApiCallback<ProfileResponseDto>() {
                    @Override
                    public void onSuccess(ProfileResponseDto u) {
                        if (u == null) { toast("更新失敗"); return; }

                        // 更新原始值，避免重複送
                        originName   = safe(u.name);
                        originGender = safe(u.gender);
                        originBirth  = safe(u.birthDate);

                        toast("已更新");
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        toast("連線錯誤：" + t.getMessage());
                    }
                }
        );
    }

    // ===== 小工具 =====

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    cal.set(y, m, d);
                    String s = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).format(cal.getTime());
                    etBirthday.setText(s);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dlg.show();
    }

    private void setGenderByApiValue(String g) {
        if ("male".equalsIgnoreCase(g))        rgGender.check(R.id.rbMale);
        else if ("female".equalsIgnoreCase(g)) rgGender.check(R.id.rbFemale);
        else                                   rgGender.clearCheck();
    }

    private String getSelectedGenderUi() {
        int id = rgGender.getCheckedRadioButtonId();
        if (id == R.id.rbMale)   return "男";
        if (id == R.id.rbFemale) return "女";
        return "";
    }

    // UI → API
    private String uiGenderToApi(String ui) {
        if ("男".equals(ui) || "M".equalsIgnoreCase(ui)) return "male";
        if ("女".equals(ui) || "F".equalsIgnoreCase(ui)) return "female";
        return "";
    }

    // "YYYY/MM/DD" ↔ "YYYY-MM-DD"
    private String toApiDate(String ui) {
        return isBlank(ui) ? "" : ui.replace('/', '-');
    }
    private String toUiDate (String api){
        return isBlank(api)? "" : api.replace('-', '/');
    }

    private boolean isBlank(String s){
        return s == null || s.trim().isEmpty();
    }
    private String  safe(String s){
        return s == null ? "" : s;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
