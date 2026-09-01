package com.example.teacherapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.auth.LoginActivity;
import com.example.teacherapp.classdetail.ClassDetailActivity;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityMainBinding;
import com.example.teacherapp.databinding.DialogEditClassroomBinding;
import com.example.teacherapp.model.ClassSection;
import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.model.Student;
import com.example.teacherapp.ui.ClassAdapter;
import com.example.teacherapp.ui.ClassSectionAdapter;
import com.example.teacherapp.ui.StudentAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirestoreRepo firetoreRepo;
    private PrefManager prefManager;
    private List<Classroom> classroomList;
    private List<ClassSection> classSectionList;
    private ClassAdapter classroomPreviewAdapter;
    private ClassSectionAdapter classSectionPreviewAdapter;
    private ClassSection pendingImportSection;
    private ActivityResultLauncher<String[]> csvPickerLauncher;
    private boolean classroomsLoaded;
    private boolean classSectionsLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyStatusBarInsets();

        firetoreRepo = new FirestoreRepo();
        prefManager = new PrefManager(this);
        classroomList = new ArrayList<>();
        classSectionList = new ArrayList<>();
        csvPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleCsvPicked);

        binding.fabCreateClass.setOnClickListener(v -> showCreateClassDialog());
        binding.btnMoreSections.setOnClickListener(v -> showClassSectionsDialog());
        binding.btnMoreClassrooms.setOnClickListener(v -> showClassroomsDialog());
        setupDashboardAdapters();
        setupDrawer();

        MaterialToolbar toolbarMain = findViewById(R.id.toolbar_main);
        toolbarMain.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        toolbarMain.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.btn_menu_main_sections) {
                showClassSectionsDialog();
                return true;
            }
            return false;
        });

        showLoading();
        loadClassrooms();
        loadClassSections();
    }

    private void applyStatusBarInsets() {
        View appBar = findViewById(R.id.app_bar_main);
        int initialLeft = appBar.getPaddingLeft();
        int initialTop = appBar.getPaddingTop();
        int initialRight = appBar.getPaddingRight();
        int initialBottom = appBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(initialLeft, initialTop + statusBars.top, initialRight, initialBottom);
            return insets;
        });
    }

    private void loadClassrooms() {
        firetoreRepo.fetchClassrooms(
                prefManager.getUserId(),
                querySnapshot -> handleClassroomsLoaded(querySnapshot.toObjects(Classroom.class)),
                e -> {
                    classroomsLoaded = true;
                    renderDashboard();
                    Log.e("Classrooms", "Error fetching classrooms", e);
                }
        );
    }

    private void loadClassSections() {
        firetoreRepo.fetchClassSections(prefManager.getUserId(), querySnapshot -> {
            classSectionList.clear();
            classSectionList.addAll(querySnapshot.toObjects(ClassSection.class));
            classSectionList.sort((left, right) -> Long.compare(
                    right.getCreatedAt() == null ? 0 : right.getCreatedAt(),
                    left.getCreatedAt() == null ? 0 : left.getCreatedAt()));
            classSectionsLoaded = true;
            renderDashboard();
        }, e -> {
            classSectionsLoaded = true;
            renderDashboard();
            Toast.makeText(this, "Could not load class-sections", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleClassroomsLoaded(List<Classroom> classrooms) {
        classroomList.clear();
        classroomList.addAll(classrooms);
        classroomList.sort((left, right) -> Long.compare(right.getCreatedDate(), left.getCreatedDate()));
        classroomsLoaded = true;
        renderDashboard();
    }

    private void setupDashboardAdapters() {
        classSectionPreviewAdapter = new ClassSectionAdapter(new ArrayList<>(), this::showStudentsDialog);
        binding.rvClassSectionsPreview.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClassSectionsPreview.setNestedScrollingEnabled(false);
        binding.rvClassSectionsPreview.setAdapter(classSectionPreviewAdapter);

        classroomPreviewAdapter = new ClassAdapter(new ArrayList<>(), this::openClassroom, this::showEditClassroomDialog);
        binding.rvClassroomsPreview.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClassroomsPreview.setNestedScrollingEnabled(false);
        binding.rvClassroomsPreview.setAdapter(classroomPreviewAdapter);
    }

    private void renderDashboard() {
        if (!classroomsLoaded || !classSectionsLoaded) {
            return;
        }

        classSectionPreviewAdapter.setItems(firstItems(classSectionList, 3));
        classroomPreviewAdapter.setItems(firstItems(classroomList, 3));

        binding.rvClassSectionsPreview.setVisibility(classSectionList.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvSectionEmpty.setVisibility(classSectionList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnMoreSections.setVisibility(classSectionList.size() > 3 ? View.VISIBLE : View.GONE);

        binding.rvClassroomsPreview.setVisibility(classroomList.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvClassroomEmpty.setVisibility(classroomList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnMoreClassrooms.setVisibility(classroomList.size() > 3 ? View.VISIBLE : View.GONE);

        showMainView();
    }

    private <T> List<T> firstItems(List<T> values, int limit) {
        int end = Math.min(values.size(), limit);
        return new ArrayList<>(values.subList(0, end));
    }

    private void openClassroom(Classroom classroom) {
        Intent detailActivity = new Intent(MainActivity.this, ClassDetailActivity.class)
                    .putExtra("class_name", classroom.getClassName())
                    .putExtra("class_code", classroom.getClassCode());
        startActivity(detailActivity);
    }

    private void setupDrawer() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        NavigationView navigationView = binding.navViewMain;
        View header = navigationView.getHeaderView(0);
        MaterialTextView tvName = header.findViewById(R.id.tv_nav_user_name);
        MaterialTextView tvEmail = header.findViewById(R.id.tv_nav_user_email);

        if (user != null) {
            String name = user.getDisplayName();
            tvName.setText(name == null || name.trim().isEmpty() ? "Teacher" : name);
            String email = user.getEmail();
            tvEmail.setText(email == null || email.trim().isEmpty() ? user.getUid() : email);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            if (id == R.id.nav_class_sections) {
                showClassSectionsDialog();
            } else if (id == R.id.nav_create_classroom) {
                showCreateClassDialog();
            } else if (id == R.id.nav_logout) {
                confirmLogout();
            }
            return true;
        });
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout?")
                .setMessage("You will return to the login screen.")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        prefManager.clearLogin();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showClassSectionsDialog() {
        String[] sectionLabels = buildSectionLabels();
        final int[] selectedIndex = {-1};

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Class-Sections")
                .setSingleChoiceItems(sectionLabels, -1, (choiceDialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Import CSV", null)
                .setNeutralButton("Create", null)
                .setNegativeButton("Close", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (classSectionList.isEmpty() || selectedIndex[0] == -1) {
                Toast.makeText(this, "Select a class-section first", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingImportSection = classSectionList.get(selectedIndex[0]);
            csvPickerLauncher.launch(new String[]{"text/*", "text/comma-separated-values", "application/csv"});
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
            showCreateClassSectionDialog();
            dialog.dismiss();
        });
    }

    private String[] buildSectionLabels() {
        if (classSectionList.isEmpty()) {
            return new String[]{"No class-sections yet"};
        }

        String[] labels = new String[classSectionList.size()];
        for (int i = 0; i < classSectionList.size(); i++) {
            ClassSection section = classSectionList.get(i);
            long studentCount = section.getStudentCount() == null ? 0 : section.getStudentCount();
            labels[i] = section.getSectionName() + "\n" + studentCount + " students";
        }
        return labels;
    }

    private void showClassroomsDialog() {
        if (classroomList.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Classrooms")
                    .setMessage("No classrooms yet.")
                    .setPositiveButton("Create", (dialog, which) -> showCreateClassDialog())
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }

        String[] labels = new String[classroomList.size()];
        for (int i = 0; i < classroomList.size(); i++) {
            Classroom classroom = classroomList.get(i);
            labels[i] = classroom.getClassName() + "\nCode: " + classroom.getClassCode();
        }

        final int[] selectedIndex = {-1};
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Classrooms")
                .setSingleChoiceItems(labels, -1, (choiceDialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Open", null)
                .setNeutralButton("Edit", null)
                .setNegativeButton("Close", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (selectedIndex[0] == -1) {
                Toast.makeText(this, "Select a classroom", Toast.LENGTH_SHORT).show();
                return;
            }
            openClassroom(classroomList.get(selectedIndex[0]));
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
            if (selectedIndex[0] == -1) {
                Toast.makeText(this, "Select a classroom to edit", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditClassroomDialog(classroomList.get(selectedIndex[0]));
            dialog.dismiss();
        });
    }

    private void showEditClassroomDialog(Classroom classroom) {
        DialogEditClassroomBinding dialogBinding = DialogEditClassroomBinding.inflate(getLayoutInflater());
        dialogBinding.etEditClassName.setText(classroom.getClassName());
        dialogBinding.switchClassEnabled.setChecked(classroom.isClassEnabledOrDefault());
        dialogBinding.switchQuizModeEnabled.setChecked(classroom.isQuizModeEnabledOrDefault());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Classroom")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String className = dialogBinding.etEditClassName.getText() == null
                    ? ""
                    : dialogBinding.etEditClassName.getText().toString().trim();
            if (className.isEmpty()) {
                Toast.makeText(this, "Enter classroom name", Toast.LENGTH_SHORT).show();
                return;
            }
            updateClassroom(classroom,
                    className,
                    dialogBinding.switchClassEnabled.isChecked(),
                    dialogBinding.switchQuizModeEnabled.isChecked(),
                    dialog);
        });
    }

    private void updateClassroom(Classroom classroom,
                                 String className,
                                 boolean classEnabled,
                                 boolean quizModeEnabled,
                                 AlertDialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("className", className);
        updates.put("classEnabled", classEnabled);
        updates.put("quizModeEnabled", quizModeEnabled);

        firetoreRepo.updateClassroom(classroom.getClassCode(), updates, unused -> {
            classroom.setClassName(className);
            classroom.setClassEnabled(classEnabled);
            classroom.setQuizModeEnabled(quizModeEnabled);
            renderDashboard();
            Toast.makeText(this, "Classroom updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }, e -> Toast.makeText(this, "Could not update classroom", Toast.LENGTH_SHORT).show());
    }

    private void showStudentsDialog(ClassSection section) {
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setPadding(0, 8, 0, 8);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(section.getSectionName())
                .setMessage("Loading students...")
                .setView(recyclerView)
                .setPositiveButton("Import CSV", null)
                .setNegativeButton("Close", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            pendingImportSection = section;
            csvPickerLauncher.launch(new String[]{"text/*", "text/comma-separated-values", "application/csv"});
            dialog.dismiss();
        });

        firetoreRepo.fetchStudents(section.getSectionId(), querySnapshot -> {
            List<Student> students = querySnapshot.toObjects(Student.class);
            dialog.setMessage(students.isEmpty() ? "No students imported yet." : null);
            recyclerView.setAdapter(new StudentAdapter(students));
        }, e -> dialog.setMessage("Could not load students."));
    }

    private void showCreateClassSectionDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Class-Section Name");
        input.setSingleLine(true);
        input.setPadding(40, 20, 40, 0);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Create Class-Section")
                .setView(input)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String sectionName = input.getText() == null ? "" : input.getText().toString().trim();
            if (sectionName.isEmpty()) {
                Toast.makeText(this, "Enter class-section name", Toast.LENGTH_SHORT).show();
                return;
            }
            createClassSection(sectionName, dialog);
        });
    }

    private void createClassSection(String sectionName, AlertDialog dialog) {
        DocumentReference reference = firetoreRepo.newClassSectionReference();
        ClassSection section = new ClassSection(
                reference.getId(),
                sectionName,
                prefManager.getUserId(),
                System.currentTimeMillis(),
                0L);
        firetoreRepo.createClassSection(section, unused -> {
            classSectionList.add(0, section);
            Toast.makeText(this, "Class-section created", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            renderDashboard();
        }, e -> Toast.makeText(this, "Could not create class-section", Toast.LENGTH_SHORT).show());
    }

    private void handleCsvPicked(Uri uri) {
        if (uri == null || pendingImportSection == null) {
            pendingImportSection = null;
            return;
        }

        try {
            List<Student> students = parseStudentsCsv(uri);
            if (students.isEmpty()) {
                Toast.makeText(this, "No students found in CSV", Toast.LENGTH_SHORT).show();
                pendingImportSection = null;
                return;
            }

            ClassSection importSection = pendingImportSection;
            firetoreRepo.importStudents(importSection.getSectionId(), students, unused -> {
                importSection.setStudentCount((long) students.size());
                Toast.makeText(this, students.size() + " students imported", Toast.LENGTH_SHORT).show();
                pendingImportSection = null;
                loadClassSections();
            }, e -> {
                Toast.makeText(this, "Could not import students", Toast.LENGTH_SHORT).show();
                pendingImportSection = null;
            });
        } catch (Exception e) {
            Log.e("ClassSections", "CSV import failed", e);
            Toast.makeText(this, "Invalid CSV file", Toast.LENGTH_SHORT).show();
            pendingImportSection = null;
        }
    }

    private List<Student> parseStudentsCsv(Uri uri) throws Exception {
        List<Student> students = new ArrayList<>();
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return students;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return students;
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerIndex.put(headers.get(i).trim().toLowerCase(Locale.US), i);
            }

            int admissionIndex = getColumnIndex(headerIndex, "admission_no", "admission no", "admission");
            int nameIndex = getColumnIndex(headerIndex, "name", "student_name", "student name");
            int phoneIndex = getColumnIndex(headerIndex, "phone", "mobile", "phone_no", "phone no");

            String line;
            int generatedId = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                String admissionNo = getCsvValue(values, admissionIndex);
                String name = getCsvValue(values, nameIndex);
                String phone = getCsvValue(values, phoneIndex);
                if (admissionNo.isEmpty() && name.isEmpty() && phone.isEmpty()) {
                    continue;
                }
                if (admissionNo.isEmpty()) {
                    admissionNo = "student_" + generatedId++;
                }
                students.add(new Student(sanitizeDocumentId(admissionNo), name, phone));
            }
        }
        return students;
    }

    private int getColumnIndex(Map<String, Integer> headerIndex, String... names) {
        for (String name : names) {
            Integer index = headerIndex.get(name);
            if (index != null) {
                return index;
            }
        }
        return -1;
    }

    private String getCsvValue(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char value = line.charAt(i);
            if (value == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (value == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String sanitizeDocumentId(String value) {
        String sanitized = value.trim().replace("/", "_");
        return sanitized.isEmpty() ? String.valueOf(System.currentTimeMillis()) : sanitized;
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
        TextInputEditText etClassSection = dialogLayout.findViewById(R.id.et_class_section);
        TextInputEditText etClassCode = dialogLayout.findViewById(R.id.et_class_code);
        TextInputLayout tilClassSection = dialogLayout.findViewById(R.id.til_class_section);
        TextInputLayout tilClassCode = dialogLayout.findViewById(R.id.til_class_code);

        String classCode = generateClassCode();
        final ClassSection[] selectedSection = {null};
        etClassCode.setText(classCode);
        etClassSection.setOnClickListener(view -> showSectionPicker(selectedSection, etClassSection));
        tilClassSection.setEndIconOnClickListener(view -> showSectionPicker(selectedSection, etClassSection));
        tilClassCode.setEndIconOnClickListener(view -> copyClassCode(etClassCode));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String className = etClassName.getText() != null ? etClassName.getText().toString().trim() : "";

                    if (className.isEmpty()) {
                        Toast.makeText(this, "Enter classroom name", Toast.LENGTH_SHORT).show();
                    } else if (selectedSection[0] == null) {
                        Toast.makeText(this, "Choose a class-section", Toast.LENGTH_SHORT).show();
                    } else {
                        createClassroom(className, classCode, selectedSection[0], dialog);
                    }
                });
    }

    private void showSectionPicker(ClassSection[] selectedSection, TextInputEditText etClassSection) {
        if (classSectionList.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("No Class-Sections")
                    .setMessage("Create a class-section before creating a classroom.")
                    .setPositiveButton("Create", (dialog, which) -> showCreateClassSectionDialog())
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String[] labels = buildSectionLabels();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Class-Section")
                .setItems(labels, (dialog, which) -> {
                    selectedSection[0] = classSectionList.get(which);
                    etClassSection.setText(selectedSection[0].getSectionName());
                })
                .show();
    }

    private void copyClassCode(TextInputEditText etClassCode) {
        String code = etClassCode.getEditableText() == null ? "" : etClassCode.getEditableText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("classroom_code", code);
        clipboard.setPrimaryClip(data);
        Toast.makeText(MainActivity.this, "Copied: " + code, Toast.LENGTH_SHORT).show();
    }

    private String generateClassCode() {
        String code;
        do {
            code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        } while (isClassCodeAlreadyLoaded(code));
        return code;
    }

    private boolean isClassCodeAlreadyLoaded(String code) {
        for (Classroom classroom : classroomList) {
            if (code.equals(classroom.getClassCode())) {
                return true;
            }
        }
        return false;
    }

    private void createClassroom(String className, String classCode, ClassSection section, AlertDialog dialog) {
        String name = FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        Classroom classroom = new Classroom(
                className,
                classCode,
                name,
                prefManager.getUserId(),
                System.currentTimeMillis(),
                section.getSectionId(),
                section.getSectionName());
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
        binding.dashboardContent.setVisibility(View.GONE);
        binding.tvNoClassroom.setVisibility(View.GONE);
    }

    private void showMainView() {
        binding.pbLoadingClass.setVisibility(View.GONE);
        binding.dashboardContent.setVisibility(View.VISIBLE);
        binding.tvNoClassroom.setVisibility(View.GONE);
    }

    private void showNoClassroom(String message) {
        binding.pbLoadingClass.setVisibility(View.GONE);
        binding.dashboardContent.setVisibility(View.GONE);
        binding.tvNoClassroom.setText(message);
        binding.tvNoClassroom.setVisibility(View.VISIBLE);
    }
}
