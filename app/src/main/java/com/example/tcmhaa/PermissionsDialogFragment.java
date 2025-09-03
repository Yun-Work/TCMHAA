package com.example.tcmhaa;

import android.Manifest;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 權限彈窗（必須全同意才可繼續）
 * - 只會在第一次登入時顯示，之後不再彈出
 * - 勾選「我同意所有必要權限」→ 自動全選
 * - 單項若被取消，會立即恢復勾選並提示
 * - 全部授權成功後，透過 OnAllGrantedListener 通知外部
 *
 * layout: res/layout/dialog_permissions_2_2.xml
 *   - checkbox_all, checkbox_privacy, checkbox_camera, checkbox_notify, checkbox_storage, btnConfirm
 */
public class PermissionsDialogFragment extends DialogFragment {

    public interface OnAllGrantedListener {
        void onAllGranted();
    }

    private OnAllGrantedListener onAllGrantedListener;
    public void setOnAllGrantedListener(OnAllGrantedListener l) {
        this.onAllGrantedListener = l;
    }

    private CheckBox cbAll, cbPrivacy, cbCamera, cbNotify, cbStorage;
    private Button btnConfirm;

    // 現代化 API：一次請多個權限
    private final ActivityResultLauncher<String[]> requestPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionsResult);

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_permissions_2_2, null, false);

        cbAll     = view.findViewById(R.id.checkbox_all);
        cbPrivacy = view.findViewById(R.id.checkbox_privacy);
        cbCamera  = view.findViewById(R.id.checkbox_camera);
        cbNotify  = view.findViewById(R.id.checkbox_notify);
        cbStorage = view.findViewById(R.id.checkbox_storage);
        btnConfirm = view.findViewById(R.id.btnConfirm);

        // Android 13 以下不需通知權限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            cbNotify.setEnabled(false);
            cbNotify.setChecked(true);
            cbNotify.setText("是否接收通知（此裝置不需額外授權）");
        }

        btnConfirm.setEnabled(false);

        // 總開關：勾選 = 全選
        cbAll.setOnCheckedChangeListener((button, checked) -> {
            setAllChecked(checked);
            btnConfirm.setEnabled(checked);
        });

        // 單項不可取消：若取消立即恢復
        Consumer<CheckBox> lockChecked = cb -> cb.setOnCheckedChangeListener((b, isChecked) -> {
            if (!isChecked) {
                toast("本服務需同意所有權限才能繼續。");
                b.setChecked(true);
                if (!cbAll.isChecked()) cbAll.setChecked(true);
                btnConfirm.setEnabled(true);
            }
        });
        lockChecked.accept(cbPrivacy);
        lockChecked.accept(cbCamera);
        lockChecked.accept(cbStorage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lockChecked.accept(cbNotify);
        }

        // 確認 → 發起權限請求
        btnConfirm.setOnClickListener(v -> {
            if (!cbAll.isChecked()) {
                toast("請先勾選「我同意所有必要權限」。");
                return;
            }
            requestAllNeededPermissions();
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    /** 勾選全選/全取消 */
    private void setAllChecked(boolean checked) {
        cbPrivacy.setChecked(checked);
        cbCamera.setChecked(checked);
        cbStorage.setChecked(checked);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cbNotify.setChecked(checked);
            cbNotify.setEnabled(true);
        } else {
            cbNotify.setChecked(true);
            cbNotify.setEnabled(false);
        }
    }

    /** 發起權限請求 */
    private void requestAllNeededPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
            list.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        requestPermsLauncher.launch(list.toArray(new String[0]));
    }

    /** 權限結果回傳 */
    private void onPermissionsResult(Map<String, Boolean> result) {
        boolean allGranted = true;

        for (Map.Entry<String, Boolean> e : result.entrySet()) {
            String perm = e.getKey();
            boolean granted = Boolean.TRUE.equals(e.getValue());

            if (Manifest.permission.CAMERA.equals(perm)) {
                toast(granted ? "相機權限已授予" : "相機權限被拒絕");
            } else if (Manifest.permission.POST_NOTIFICATIONS.equals(perm)) {
                toast(granted ? "通知權限已授予" : "通知權限被拒絕");
            } else if (Manifest.permission.READ_MEDIA_IMAGES.equals(perm)
                    || Manifest.permission.READ_EXTERNAL_STORAGE.equals(perm)) {
                toast(granted ? "圖片讀取權限已授予" : "圖片讀取權限被拒絕");
            }

            if (!granted) allGranted = false;
        }

        if (allGranted) {
            toast("所有必要權限已授予");

            // ✅ 記錄：完成首次權限導覽，以後不再顯示
            PrefsHelper.setPermissionsOnboardDone(requireContext(), true);

            if (onAllGrantedListener != null) onAllGrantedListener.onAllGranted();
            dismiss();
        } else {
            toast("需同意全部權限才能使用本服務");
        }
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}
