package com.example.teacherapp;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.teacherapp.classdetail.ClassDetailActivity;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityMainBinding;
import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.ui.ClassAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirestoreRepo firetoreRepo;
    private PrefManager prefManager;
    private List<Classroom> classroomList;
    private ClassAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firetoreRepo = new FirestoreRepo();
        prefManager = new PrefManager(this);
        classroomList = new ArrayList<>();

        loadClassrooms();
        binding.fabCreateClass.setOnClickListener( v -> showCreateClassDialog());

        MaterialToolbar toolbarMain = findViewById(R.id.toolbar_main);
        toolbarMain.setNavigationOnClickListener(v -> {
            // show dialog with more information
            Toast.makeText(MainActivity.this, FirebaseAuth.getInstance()
                    .getCurrentUser().getDisplayName(), Toast.LENGTH_SHORT).show();
        });

    }

    private void loadClassrooms() {
        showLoading();
        firetoreRepo.fetchClassrooms(
                prefManager.getUserId(),
                querySnapshot -> handleClassroomsLoaded(querySnapshot.toObjects(Classroom.class)),
                e -> {
                    showNoClassroom("Error loading classrooms!");
                    Log.e("Classrooms", "Error fetching classrooms", e);
                }
        );
    }

    private void handleClassroomsLoaded(List<Classroom> classrooms) {
        classroomList.clear();
        classroomList.addAll(classrooms);

        if (classroomList.isEmpty()) {
            showNoClassroom("No classes found. Create one by clicking '+'");
        } else {
            showMainView();
            setupRecyclerView();
        }
    }

    private void setupRecyclerView() {
        adapter = new ClassAdapter(classroomList, classroom -> {
            Intent detailActivity = new Intent(MainActivity.this, ClassDetailActivity.class)
                    .putExtra("class_name", classroom.getClassName())
                    .putExtra("class_code", classroom.getClassCode());
            startActivity(detailActivity);
        });

        binding.rvClassroom.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClassroom.setAdapter(adapter);
    }

    private void showCreateClassDialog() {
        View dialogLayout = View.inflate(this, R.layout.dialog_create_classroom, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(dialogLayout)
                .setCancelable(false)
                .setTitle("Create Classroom")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextInputEditText etClassName = dialogLayout.findViewById(R.id.et_class_name);
        TextInputEditText etClassCode = dialogLayout.findViewById(R.id.et_class_code);
        TextInputLayout tilClassCode = dialogLayout.findViewById(R.id.til_class_code);

        String uuid = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8);
        String classCode = getString(R.string.class_code_display, prefManager.getUserId()
                .substring(0, 8), uuid);
        etClassCode.setText(classCode);

        tilClassCode.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = etClassCode.getEditableText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData data = ClipData.newPlainText("classroom_code", code);
                clipboard.setPrimaryClip(data);
                Toast.makeText(MainActivity.this, "Copied: " + code, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String className = etClassName.getText() != null ? etClassName.getText().toString().trim() : "";

                    if (!className.isEmpty()) {
                        createClassroom(className, classCode, dialog);
                    } else {
                        Toast.makeText(this, "Enter classroom name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createClassroom(String className, String classCode, AlertDialog dialog) {
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        Classroom classroom = new Classroom(className, classCode, name, prefManager.getUserId(), System.currentTimeMillis());
        firetoreRepo.createClassroom(classroom,
                queryDocumentSnapshots -> {
                    Toast.makeText(this, "Class created", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadClassrooms();
                },
                e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading() {
        binding.pbLoadingClass.setVisibility(View.VISIBLE);
        binding.rvClassroom.setVisibility(View.GONE);
        binding.tvNoClassroom.setVisibility(View.GONE);
    }
    private void showMainView() {
        binding.pbLoadingClass.setVisibility(View.GONE);
        binding.rvClassroom.setVisibility(View.VISIBLE);
        binding.tvNoClassroom.setVisibility(View.GONE);
    }
    private void showNoClassroom(String message) {
        binding.pbLoadingClass.setVisibility(View.GONE);
        binding.rvClassroom.setVisibility(View.GONE);
        binding.tvNoClassroom.setText(message);
        binding.tvNoClassroom.setVisibility(View.VISIBLE);
    }
}