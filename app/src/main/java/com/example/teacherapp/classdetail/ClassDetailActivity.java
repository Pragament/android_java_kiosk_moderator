package com.example.teacherapp.classdetail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.teacherapp.R;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityClassDetailBinding;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepo = new FirestoreRepo();
        usageLogList = new ArrayList<>();
        classroom = new Classroom();

        Intent intent = getIntent();
        MaterialToolbar toolbar = findViewById(R.id.toolbar_class_detail);

        String classCode = intent.getStringExtra("class_code");
        toolbar.setTitle(intent.getStringExtra("class_name"));

        loadUsageLog(classCode);

        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.btn_menu_detail_refresh) {
                loadUsageLog(classCode);
            } else if (id == R.id.btn_menu_detail_about) {
                showClassDetailsDialog(classroom);
            }
            return false;
        });


        firestoreRepo.fetchSpecificClass(classCode, querySnapshot -> {
            classroom = querySnapshot.toObjects(Classroom.class).get(0);
        }, e -> classroom = null);
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

        new MaterialAlertDialogBuilder(this).setView(dialogView).show();
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