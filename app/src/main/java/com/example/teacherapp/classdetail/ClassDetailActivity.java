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
import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.model.UsageLog;
import com.example.teacherapp.model.WhitelistedApp;
import com.example.teacherapp.submissions.QuizSubmissionsActivity;
import com.example.teacherapp.webusage.WebUsageActivity;
import com.example.teacherapp.ui.UsageLogAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class ClassDetailActivity extends AppCompatActivity {

    private enum DateFilter {
        TODAY,
        ALL
    }

    private enum GroupMode {
        NONE,
        APP,
        STUDENT
    }

    private enum SortMode {
        TIME_DESC,
        TIME_ASC,
        APP,
        STUDENT
    }

    private ActivityClassDetailBinding binding;
    private FirestoreRepo firestoreRepo;
    private final List<UsageLog> allUsageLogList = new ArrayList<>();
    private final List<UsageLogAdapter.DisplayItem> displayItems = new ArrayList<>();
    private final Set<String> whitelistedPackages = new HashSet<>();
    private UsageLogAdapter adapter;
    private Classroom classroom;
    private String classCode;
    private DateFilter dateFilter = DateFilter.TODAY;
    private GroupMode groupMode = GroupMode.NONE;
    private SortMode sortMode = SortMode.TIME_DESC;
    private UsageLogAdapter.ViewMode viewMode = UsageLogAdapter.ViewMode.CARD;
    private String appNameFilter;
    private String studentNameFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyStatusBarInsets();

        firestoreRepo = new FirestoreRepo();
        classroom = new Classroom();

        Intent intent = getIntent();
        classCode = intent.getStringExtra("class_code");

        MaterialToolbar toolbar = findViewById(R.id.toolbar_class_detail);
        toolbar.setTitle(intent.getStringExtra("class_name"));
        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.btn_menu_detail_refresh) {
                loadClassData();
            } else if (id == R.id.btn_menu_detail_whitelist) {
                showWhitelistedAppsDialog();
            } else if (id == R.id.btn_menu_detail_date_filter) {
                showDateFilterDialog();
            } else if (id == R.id.btn_menu_detail_filter) {
                showFilterTypeDialog();
            } else if (id == R.id.btn_menu_detail_group) {
                showGroupDialog();
            } else if (id == R.id.btn_menu_detail_view_mode) {
                showViewModeDialog();
            } else if (id == R.id.btn_menu_detail_sort) {
                showSortDialog();
            } else if (id == R.id.btn_menu_detail_about) {
                showClassDetailsDialog(classroom);
            }
            return true;
        });
        setupBottomNavigation(toolbar);

        binding.swipeRefreshUsageLog.setOnRefreshListener(this::loadClassData);
        setupRecyclerView();
        loadClassData();

        firestoreRepo.fetchSpecificClass(classCode, querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                classroom = querySnapshot.toObjects(Classroom.class).get(0);
            }
        }, e -> classroom = null);
    }

    private void setupBottomNavigation(MaterialToolbar toolbar) {
        binding.bottomNavClassroom.setSelectedItemId(R.id.bottom_nav_apps);
        binding.bottomNavClassroom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.bottom_nav_apps) {
                return true;
            }
            if (id == R.id.bottom_nav_websites) {
                Intent webUsageIntent = new Intent(this, WebUsageActivity.class)
                        .putExtra("class_code", classCode)
                        .putExtra("class_name", toolbar.getTitle());
                startActivity(webUsageIntent);
                return true;
            }
            if (id == R.id.bottom_nav_quiz) {
                Intent submissionsIntent = new Intent(this, QuizSubmissionsActivity.class)
                        .putExtra("class_code", classCode)
                        .putExtra("class_name", toolbar.getTitle());
                startActivity(submissionsIntent);
                return true;
            }
            return false;
        });
    }

    private void applyStatusBarInsets() {
        View appBar = findViewById(R.id.app_bar_class_detail);
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

    private void setupRecyclerView() {
        adapter = new UsageLogAdapter(displayItems, whitelistedPackages, viewMode,
                new UsageLogAdapter.OnUsageLogActionListener() {
                    @Override
                    public void onLogClick(UsageLog usageLog) {
                        showUsageLogDialog(usageLog);
                    }

                    @Override
                    public void onWhitelistClick(UsageLog usageLog) {
                        confirmToggleWhitelist(usageLog);
                    }
                });
        binding.rvUsageLog.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsageLog.setAdapter(adapter);
    }

    private void loadClassData() {
        if (!binding.swipeRefreshUsageLog.isRefreshing()) {
            showLoading();
        }

        firestoreRepo.fetchWhitelistedApps(classCode, whitelistSnapshot -> {
            whitelistedPackages.clear();
            List<WhitelistedApp> apps = whitelistSnapshot.toObjects(WhitelistedApp.class);
            for (WhitelistedApp app : apps) {
                whitelistedPackages.add(app.getPackageName());
            }
            loadUsageLogs();
        }, e -> {
            whitelistedPackages.clear();
            loadUsageLogs();
        });
    }

    private void loadUsageLogs() {
        firestoreRepo.fetchUsageLogs(classCode, querySnapshot -> {
            binding.swipeRefreshUsageLog.setRefreshing(false);
            allUsageLogList.clear();
            allUsageLogList.addAll(querySnapshot.toObjects(UsageLog.class));
            applyFiltersAndRender();
        }, e -> {
            binding.swipeRefreshUsageLog.setRefreshing(false);
            showNoClassroom("Error loading usage log!");
        });
    }

    private void applyFiltersAndRender() {
        List<UsageLog> filteredLogs = new ArrayList<>();
        for (UsageLog usageLog : allUsageLogList) {
            if (dateFilter == DateFilter.TODAY && !isToday(usageLog.getTimestamp())) {
                continue;
            }
            if (appNameFilter != null && !appNameFilter.equals(safeText(usageLog.getAppName()))) {
                continue;
            }
            if (studentNameFilter != null && !studentNameFilter.equals(safeText(usageLog.getStudentName()))) {
                continue;
            }
            filteredLogs.add(usageLog);
        }

        filteredLogs.sort(getComparator());

        List<UsageLogAdapter.DisplayItem> newDisplayItems = new ArrayList<>();
        String currentGroup = null;
        for (UsageLog usageLog : filteredLogs) {
            String groupTitle = getGroupTitle(usageLog);
            if (groupTitle != null && !groupTitle.equals(currentGroup)) {
                currentGroup = groupTitle;
                newDisplayItems.add(UsageLogAdapter.DisplayItem.group(groupTitle));
            }
            newDisplayItems.add(UsageLogAdapter.DisplayItem.log(usageLog));
        }

        adapter.setViewMode(viewMode);
        adapter.setWhitelistedPackages(whitelistedPackages);
        adapter.setItems(newDisplayItems);
        updateFilterSummary(filteredLogs.size());

        if (filteredLogs.isEmpty()) {
            showNoClassroom("No usage logs for selected filters.");
        } else {
            showMainView();
        }
    }

    private Comparator<UsageLog> getComparator() {
        if (sortMode == SortMode.TIME_ASC) {
            return Comparator.comparingLong(UsageLog::getTimestamp);
        }
        if (sortMode == SortMode.APP) {
            return Comparator.comparing(log -> safeText(log.getAppName()));
        }
        if (sortMode == SortMode.STUDENT) {
            return Comparator.comparing(log -> safeText(log.getStudentName()));
        }
        return (left, right) -> Long.compare(right.getTimestamp(), left.getTimestamp());
    }

    private String getGroupTitle(UsageLog usageLog) {
        if (groupMode == GroupMode.APP) {
            return safeText(usageLog.getAppName());
        }
        if (groupMode == GroupMode.STUDENT) {
            return safeText(usageLog.getStudentName());
        }
        return null;
    }

    private boolean isToday(long timestamp) {
        Calendar item = Calendar.getInstance();
        item.setTimeInMillis(timestamp);
        Calendar today = Calendar.getInstance();
        return item.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && item.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private void updateFilterSummary(int count) {
        String dateText = dateFilter == DateFilter.TODAY ? "Today" : "All dates";
        String appText = appNameFilter == null ? "All apps" : appNameFilter;
        String studentText = studentNameFilter == null ? "All students" : studentNameFilter;
        String groupText = groupMode == GroupMode.NONE ? "No grouping" : "Grouped by " + groupMode.name().toLowerCase(Locale.US);
        String viewText = viewMode == UsageLogAdapter.ViewMode.CARD ? "Cards" : "Table";
        binding.tvUsageFilterSummary.setText(dateText + " | " + appText + " | " + studentText + " | " + groupText + " | " + viewText + " | " + count + " logs");
    }

    private void showClassDetailsDialog(Classroom classroom) {
        if (classroom == null) {
            Toast.makeText(this, "Class details unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void showUsageLogDialog(UsageLog usageLog) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_usage_log, null);

        MaterialTextView tvStudentName = dialogView.findViewById(R.id.tv_student_name);
        MaterialTextView tvStudentLog = dialogView.findViewById(R.id.tv_student_log);
        MaterialTextView tvAppName = dialogView.findViewById(R.id.tv_app_name);
        MaterialTextView tvPackageName = dialogView.findViewById(R.id.tv_package_name);
        MaterialTextView tvTimestamp = dialogView.findViewById(R.id.tv_timestamp);

        tvStudentName.setText(safeText(usageLog.getStudentName()));
        tvStudentLog.setText("Log: " + safeText(usageLog.getStudentLog()));
        tvAppName.setText("App Name: " + safeText(usageLog.getAppName()));
        tvPackageName.setText("Package: " + safeText(usageLog.getPackageName()));
        tvTimestamp.setText("Timestamp: " + formattedDate(usageLog.getTimestamp()));

        boolean isWhitelisted = whitelistedPackages.contains(usageLog.getPackageName());
        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(isWhitelisted ? "Remove Whitelist" : "Whitelist App",
                        (dialog, which) -> confirmToggleWhitelist(usageLog))
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmToggleWhitelist(UsageLog usageLog) {
        String packageName = usageLog.getPackageName();
        if (packageName == null || packageName.trim().isEmpty()) {
            Toast.makeText(this, "No package name found for this app", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isWhitelisted = whitelistedPackages.contains(packageName);
        String appName = safeText(usageLog.getAppName()).equals("Unknown") ? packageName : usageLog.getAppName();
        String title = isWhitelisted ? "Remove from whitelist?" : "Add to whitelist?";
        String message = appName + "\n" + packageName;

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(isWhitelisted ? "Remove" : "Add", (dialog, which) -> {
                    if (isWhitelisted) {
                        removeAppFromWhitelist(packageName, appName);
                    } else {
                        addAppToWhitelist(packageName, appName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addAppToWhitelist(String packageName, String appName) {
        WhitelistedApp app = new WhitelistedApp(appName, packageName, System.currentTimeMillis());
        firestoreRepo.addWhitelistedApp(classCode, app,
                unused -> {
                    whitelistedPackages.add(packageName);
                    applyFiltersAndRender();
                    Toast.makeText(this, appName + " whitelisted", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(this, "Could not whitelist app", Toast.LENGTH_SHORT).show());
    }

    private void removeAppFromWhitelist(String packageName, String appName) {
        firestoreRepo.removeWhitelistedApp(classCode, packageName,
                unused -> {
                    whitelistedPackages.remove(packageName);
                    applyFiltersAndRender();
                    Toast.makeText(this, appName + " removed", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(this, "Could not remove app", Toast.LENGTH_SHORT).show());
    }

    private void showWhitelistedAppsDialog() {
        firestoreRepo.fetchWhitelistedApps(classCode, querySnapshot -> {
            List<WhitelistedApp> apps = querySnapshot.toObjects(WhitelistedApp.class);
            if (apps.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Whitelisted Apps")
                        .setMessage("No apps are whitelisted for this class.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            String[] appLabels = new String[apps.size()];
            for (int i = 0; i < apps.size(); i++) {
                WhitelistedApp app = apps.get(i);
                appLabels[i] = app.getAppName() + "\n" + app.getPackageName();
            }

            final int[] selectedIndex = {-1};
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Whitelisted Apps")
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
                        WhitelistedApp app = apps.get(selectedIndex[0]);
                        confirmRemoveWhitelistedApp(app, dialog);
                    });
        }, e -> Toast.makeText(this, "Could not load whitelisted apps", Toast.LENGTH_SHORT).show());
    }

    private void confirmRemoveWhitelistedApp(WhitelistedApp app, androidx.appcompat.app.AlertDialog listDialog) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove from whitelist?")
                .setMessage(app.getAppName() + "\n" + app.getPackageName())
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeAppFromWhitelist(app.getPackageName(), app.getAppName());
                    listDialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDateFilterDialog() {
        String[] options = {"Today", "All dates"};
        int checked = dateFilter == DateFilter.TODAY ? 0 : 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Date Filter")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dateFilter = which == 0 ? DateFilter.TODAY : DateFilter.ALL;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showFilterTypeDialog() {
        String[] options = {"Filter by app name", "Filter by student name", "Clear filters"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("App/Student Filter")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showValueFilterDialog(true);
                    } else if (which == 1) {
                        showValueFilterDialog(false);
                    } else {
                        appNameFilter = null;
                        studentNameFilter = null;
                        applyFiltersAndRender();
                    }
                })
                .show();
    }

    private void showValueFilterDialog(boolean filterByApp) {
        List<String> values = new ArrayList<>(getUniqueValues(filterByApp));
        values.add(0, filterByApp ? "All apps" : "All students");

        String[] options = values.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(filterByApp ? "Filter by App" : "Filter by Student")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (which == 0) {
                        if (filterByApp) {
                            appNameFilter = null;
                        } else {
                            studentNameFilter = null;
                        }
                    } else if (filterByApp) {
                        appNameFilter = selected;
                    } else {
                        studentNameFilter = selected;
                    }
                    applyFiltersAndRender();
                })
                .show();
    }

    private Set<String> getUniqueValues(boolean appNames) {
        Set<String> values = new TreeSet<>();
        for (UsageLog usageLog : allUsageLogList) {
            values.add(safeText(appNames ? usageLog.getAppName() : usageLog.getStudentName()));
        }
        return values;
    }

    private void showGroupDialog() {
        String[] options = {"None", "App name", "Student name"};
        int checked = groupMode == GroupMode.APP ? 1 : groupMode == GroupMode.STUDENT ? 2 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Group By")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    groupMode = which == 1 ? GroupMode.APP : which == 2 ? GroupMode.STUDENT : GroupMode.NONE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showViewModeDialog() {
        String[] options = {"Card view", "Sortable table view"};
        int checked = viewMode == UsageLogAdapter.ViewMode.CARD ? 0 : 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle("View Mode")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    viewMode = which == 0 ? UsageLogAdapter.ViewMode.CARD : UsageLogAdapter.ViewMode.TABLE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Newest first", "Oldest first", "App name", "Student name"};
        int checked = sortMode == SortMode.TIME_ASC ? 1 : sortMode == SortMode.APP ? 2 : sortMode == SortMode.STUDENT ? 3 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    sortMode = which == 1 ? SortMode.TIME_ASC : which == 2 ? SortMode.APP : which == 3 ? SortMode.STUDENT : SortMode.TIME_DESC;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private String formattedDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private void showLoading() {
        binding.pbLoadingUsageLog.setVisibility(View.VISIBLE);
        binding.swipeRefreshUsageLog.setVisibility(View.GONE);
        binding.tvNoUsageLog.setVisibility(View.GONE);
    }

    private void showMainView() {
        binding.pbLoadingUsageLog.setVisibility(View.GONE);
        binding.swipeRefreshUsageLog.setVisibility(View.VISIBLE);
        binding.rvUsageLog.setVisibility(View.VISIBLE);
        binding.tvNoUsageLog.setVisibility(View.GONE);
    }

    private void showNoClassroom(String message) {
        binding.pbLoadingUsageLog.setVisibility(View.GONE);
        binding.swipeRefreshUsageLog.setVisibility(View.VISIBLE);
        binding.rvUsageLog.setVisibility(View.GONE);
        binding.tvNoUsageLog.setText(message);
        binding.tvNoUsageLog.setVisibility(View.VISIBLE);
    }
}
