package com.example.teacherapp.classdetail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.teacherapp.R;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityClassDetailBinding;
import com.example.teacherapp.model.BlacklistedApp;
import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.model.UsageLog;
import com.example.teacherapp.ui.UsageLogAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassDetailActivity extends AppCompatActivity {

    private ActivityClassDetailBinding binding;
    private FirestoreRepo firestoreRepo;
    private List<UsageLog> usageLogList;
    private UsageLogAdapter adapter;
    private Classroom classroom;
    private String classCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyStatusBarInsets();

        firestoreRepo = new FirestoreRepo();
        usageLogList = new ArrayList<>();
        classroom = new Classroom();

        Intent intent = getIntent();
        MaterialToolbar toolbar = findViewById(R.id.toolbar_class_detail);

        classCode = intent.getStringExtra("class_code");
        toolbar.setTitle(intent.getStringExtra("class_name"));

        loadUsageLog(classCode);

        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.btn_menu_detail_refresh) {
                loadUsageLog(classCode);
            } else if (id == R.id.btn_menu_detail_blacklist) {
                showBlacklistedAppsDialog();
            } else if (id == R.id.btn_menu_detail_about) {
                showClassDetailsDialog(classroom);
            }
            return false;
        });


        firestoreRepo.fetchSpecificClass(classCode, querySnapshot -> {
            classroom = querySnapshot.toObjects(Classroom.class).get(0);
        }, e -> classroom = null);
    }

    private void applyStatusBarInsets() {
        View appBar = findViewById(R.id.app_bar_class_detail);
        int initialLeft = appBar.getPaddingLeft();
        int initialTop = appBar.getPaddingTop();
        int initialRight = appBar.getPaddingRight();
        int initialBottom = appBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(
                    initialLeft,
                    initialTop + statusBars.top,
                    initialRight,
                    initialBottom);
            return insets;
        });
    }

    private void showClassDetailsDialog(Classroom classroom) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about_classroom, null);

        MaterialTextView tvName = dialogView.findViewById(R.id.tv_class_name);
        MaterialTextView tvCode = dialogView.findViewById(R.id.tv_class_code);
        MaterialTextView tvCreatedBy = dialogView.findViewById(R.id.tv_created_by);
        MaterialTextView tvCreatedDate = dialogView.findViewById(R.id.tv_created_date);
        ImageButton btnCopy = dialogView.findViewById(R.id.btn_copy_code);

        tvName.setText(classroom.getClassName());
        tvCode.setText("Code: " + classroom.getClassCode());
        tvCreatedBy.setText("Teacher: " + classroom.getCreatedBy());
        tvCreatedDate.setText("Date: " + formattedDate(classroom.getCreatedDate()));

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Class Code", classroom.getClassCode());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Class code copied!", Toast.LENGTH_SHORT).show();
        });

        new MaterialAlertDialogBuilder(this).setView(dialogView).show();
    }

    private void loadUsageLog(String code) {
        showLoading();
        firestoreRepo.fetchUsageLogs(code, querySnapshot -> {
            handleUsageLogsLoaded(querySnapshot.toObjects(UsageLog.class));
        }, e -> {
            showNoClassroom("Error loading usage log!");
        });
    }

    private void handleUsageLogsLoaded(List<UsageLog> usageLogs) {
        usageLogList.clear();
        usageLogList.addAll(usageLogs);
        if (usageLogList.isEmpty()) {
            showNoClassroom("No usage log.");
        } else {
            showMainView();
            setupRecyclerView();
        }
    }

    private void setupRecyclerView() {
        adapter = new UsageLogAdapter(usageLogList, this::showUsageLogDialog);
        binding.rvUsageLog.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsageLog.setAdapter(adapter);
    }

    private void showUsageLogDialog(UsageLog usageLog) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_usage_log, null);

        MaterialTextView tvStudentName = dialogView.findViewById(R.id.tv_student_name);
        MaterialTextView tvStudentLog = dialogView.findViewById(R.id.tv_student_log);
        MaterialTextView tvAppName = dialogView.findViewById(R.id.tv_app_name);
        MaterialTextView tvPackageName = dialogView.findViewById(R.id.tv_package_name);
        MaterialTextView tvTimestamp = dialogView.findViewById(R.id.tv_timestamp);

        tvStudentName.setText(usageLog.getStudentName());
        tvStudentLog.setText("Log: " + usageLog.getStudentLog());
        tvAppName.setText("App Name: " + usageLog.getAppName());
        tvPackageName.setText("Package: " + usageLog.getPackageName());
        tvTimestamp.setText("Timestamp: " + formattedDate(usageLog.getTimestamp()));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Blacklist App", (dialog, which) -> addAppToBlacklist(usageLog))
                .setNegativeButton("Close", null)
                .show();
    }

    private void addAppToBlacklist(UsageLog usageLog) {
        String packageName = usageLog.getPackageName();
        if (packageName == null || packageName.trim().isEmpty()) {
            Toast.makeText(this, "No package name found for this app", Toast.LENGTH_SHORT).show();
            return;
        }

        String usageAppName = usageLog.getAppName();
        final String appName = usageAppName == null || usageAppName.trim().isEmpty()
                ? packageName
                : usageAppName;

        BlacklistedApp app = new BlacklistedApp(appName, packageName, System.currentTimeMillis());
        firestoreRepo.addBlacklistedApp(classCode, app,
                unused -> Toast.makeText(this, appName + " blacklisted", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "Could not blacklist app", Toast.LENGTH_SHORT).show());
    }

    private void showBlacklistedAppsDialog() {
        firestoreRepo.fetchBlacklistedApps(classCode, querySnapshot -> {
            List<BlacklistedApp> apps = querySnapshot.toObjects(BlacklistedApp.class);
            if (apps.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Blacklisted Apps")
                        .setMessage("No apps are blacklisted for this class.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            String[] appLabels = new String[apps.size()];
            for (int i = 0; i < apps.size(); i++) {
                BlacklistedApp app = apps.get(i);
                appLabels[i] = app.getAppName() + "\n" + app.getPackageName();
            }

            final int[] selectedIndex = {-1};
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Blacklisted Apps")
                    .setSingleChoiceItems(appLabels, -1, (choiceDialog, which) -> selectedIndex[0] = which)
                    .setPositiveButton("Remove", null)
                    .setNegativeButton("Close", null)
                    .show();

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        if (selectedIndex[0] == -1) {
                            Toast.makeText(this, "Select an app to remove", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        removeAppFromBlacklist(apps.get(selectedIndex[0]));
                        dialog.dismiss();
                    });
        }, e -> Toast.makeText(this, "Could not load blacklisted apps", Toast.LENGTH_SHORT).show());
    }

    private void removeAppFromBlacklist(BlacklistedApp app) {
        firestoreRepo.removeBlacklistedApp(classCode, app.getPackageName(),
                unused -> Toast.makeText(this, app.getAppName() + " removed", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "Could not remove app", Toast.LENGTH_SHORT).show());
    }


    private String formattedDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private void showLoading() {
        binding.pbLoadingUsageLog.setVisibility(View.VISIBLE);
        binding.rvUsageLog.setVisibility(View.GONE);
        binding.tvNoUsageLog.setVisibility(View.GONE);
    }

    private void showMainView() {
        binding.pbLoadingUsageLog.setVisibility(View.GONE);
        binding.rvUsageLog.setVisibility(View.VISIBLE);
        binding.tvNoUsageLog.setVisibility(View.GONE);
    }

    private void showNoClassroom(String message) {
        binding.pbLoadingUsageLog.setVisibility(View.GONE);
        binding.rvUsageLog.setVisibility(View.GONE);
        binding.tvNoUsageLog.setText(message);
        binding.tvNoUsageLog.setVisibility(View.VISIBLE);
    }
}
