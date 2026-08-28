package com.example.teacherapp.webusage;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.teacherapp.R;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityWebUsageBinding;
import com.example.teacherapp.model.WebUsageLog;
import com.example.teacherapp.model.WhitelistedWebsite;
import com.example.teacherapp.ui.WebUsageLogAdapter;
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

public class WebUsageActivity extends AppCompatActivity {

    private enum DateFilter {
        TODAY,
        ALL
    }

    private enum GroupMode {
        NONE,
        HOST,
        STUDENT
    }

    private enum SortMode {
        TIME_DESC,
        TIME_ASC,
        HOST,
        STUDENT
    }

    private ActivityWebUsageBinding binding;
    private FirestoreRepo firestoreRepo;
    private final List<WebUsageLog> allWebUsageLogList = new ArrayList<>();
    private final List<WebUsageLogAdapter.DisplayItem> displayItems = new ArrayList<>();
    private final Set<String> whitelistedHosts = new HashSet<>();
    private WebUsageLogAdapter adapter;
    private String classCode;
    private DateFilter dateFilter = DateFilter.TODAY;
    private GroupMode groupMode = GroupMode.NONE;
    private SortMode sortMode = SortMode.TIME_DESC;
    private WebUsageLogAdapter.ViewMode viewMode = WebUsageLogAdapter.ViewMode.CARD;
    private String hostFilter;
    private String studentNameFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebUsageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyStatusBarInsets();

        firestoreRepo = new FirestoreRepo();
        classCode = getIntent().getStringExtra("class_code");

        MaterialToolbar toolbar = findViewById(R.id.toolbar_web_usage);
        toolbar.setTitle("Web Usage");
        toolbar.setSubtitle(getIntent().getStringExtra("class_name"));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.btn_menu_web_refresh) {
                loadWebUsageData();
            } else if (id == R.id.btn_menu_web_whitelist) {
                showWhitelistedWebsitesDialog();
            } else if (id == R.id.btn_menu_web_date_filter) {
                showDateFilterDialog();
            } else if (id == R.id.btn_menu_web_filter) {
                showFilterTypeDialog();
            } else if (id == R.id.btn_menu_web_group) {
                showGroupDialog();
            } else if (id == R.id.btn_menu_web_view_mode) {
                showViewModeDialog();
            } else if (id == R.id.btn_menu_web_sort) {
                showSortDialog();
            }
            return true;
        });

        binding.swipeRefreshWebUsage.setOnRefreshListener(this::loadWebUsageData);
        setupRecyclerView();
        loadWebUsageData();
    }

    private void applyStatusBarInsets() {
        View appBar = findViewById(R.id.app_bar_web_usage);
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
        adapter = new WebUsageLogAdapter(displayItems, whitelistedHosts, viewMode,
                new WebUsageLogAdapter.OnWebUsageLogClickListener() {
                    @Override
                    public void onLogClick(WebUsageLog webUsageLog) {
                        showWebUsageLogDialog(webUsageLog);
                    }

                    @Override
                    public void onWhitelistClick(WebUsageLog webUsageLog) {
                        confirmToggleWebsiteWhitelist(webUsageLog);
                    }
                });
        binding.rvWebUsage.setLayoutManager(new LinearLayoutManager(this));
        binding.rvWebUsage.setAdapter(adapter);
    }

    private void loadWebUsageData() {
        if (!binding.swipeRefreshWebUsage.isRefreshing()) {
            showLoading();
        }

        firestoreRepo.fetchWhitelistedWebsites(classCode, whitelistSnapshot -> {
            whitelistedHosts.clear();
            List<WhitelistedWebsite> websites = whitelistSnapshot.toObjects(WhitelistedWebsite.class);
            for (WhitelistedWebsite website : websites) {
                whitelistedHosts.add(website.getHost());
            }
            loadWebUsageLogs();
        }, e -> {
            whitelistedHosts.clear();
            loadWebUsageLogs();
        });
    }

    private void loadWebUsageLogs() {

        firestoreRepo.fetchWebUsageLogs(classCode, querySnapshot -> {
            binding.swipeRefreshWebUsage.setRefreshing(false);
            allWebUsageLogList.clear();
            allWebUsageLogList.addAll(querySnapshot.toObjects(WebUsageLog.class));
            applyFiltersAndRender();
        }, e -> {
            binding.swipeRefreshWebUsage.setRefreshing(false);
            showNoWebUsage("Error loading web usage logs!");
        });
    }

    private void applyFiltersAndRender() {
        List<WebUsageLog> filteredLogs = new ArrayList<>();
        for (WebUsageLog webUsageLog : allWebUsageLogList) {
            if (dateFilter == DateFilter.TODAY && !isToday(webUsageLog.getTimestamp())) {
                continue;
            }
            if (hostFilter != null && !hostFilter.equals(safeText(webUsageLog.getHost()))) {
                continue;
            }
            if (studentNameFilter != null && !studentNameFilter.equals(safeText(webUsageLog.getStudentName()))) {
                continue;
            }
            filteredLogs.add(webUsageLog);
        }

        filteredLogs.sort(getComparator());

        List<WebUsageLogAdapter.DisplayItem> newDisplayItems = new ArrayList<>();
        String currentGroup = null;
        for (WebUsageLog webUsageLog : filteredLogs) {
            String groupTitle = getGroupTitle(webUsageLog);
            if (groupTitle != null && !groupTitle.equals(currentGroup)) {
                currentGroup = groupTitle;
                newDisplayItems.add(WebUsageLogAdapter.DisplayItem.group(groupTitle));
            }
            newDisplayItems.add(WebUsageLogAdapter.DisplayItem.log(webUsageLog));
        }

        adapter.setViewMode(viewMode);
        adapter.setWhitelistedHosts(whitelistedHosts);
        adapter.setItems(newDisplayItems);
        updateFilterSummary(filteredLogs.size());

        if (filteredLogs.isEmpty()) {
            showNoWebUsage("No web usage logs for selected filters.");
        } else {
            showMainView();
        }
    }

    private Comparator<WebUsageLog> getComparator() {
        if (sortMode == SortMode.TIME_ASC) {
            return Comparator.comparingLong(WebUsageLog::getTimestamp);
        }
        if (sortMode == SortMode.HOST) {
            return Comparator.comparing(log -> safeText(log.getHost()));
        }
        if (sortMode == SortMode.STUDENT) {
            return Comparator.comparing(log -> safeText(log.getStudentName()));
        }
        return (left, right) -> Long.compare(right.getTimestamp(), left.getTimestamp());
    }

    private String getGroupTitle(WebUsageLog webUsageLog) {
        if (groupMode == GroupMode.HOST) {
            return safeText(webUsageLog.getHost());
        }
        if (groupMode == GroupMode.STUDENT) {
            return safeText(webUsageLog.getStudentName());
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
        String hostText = hostFilter == null ? "All hosts" : hostFilter;
        String studentText = studentNameFilter == null ? "All students" : studentNameFilter;
        String groupText = groupMode == GroupMode.NONE ? "No grouping" : "Grouped by " + groupMode.name().toLowerCase(Locale.US);
        String viewText = viewMode == WebUsageLogAdapter.ViewMode.CARD ? "Cards" : "Table";
        binding.tvWebFilterSummary.setText(dateText + " | " + hostText + " | " + studentText + " | " + groupText + " | " + viewText + " | " + count + " logs");
    }

    private void showWebUsageLogDialog(WebUsageLog webUsageLog) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_web_usage_log, null);

        MaterialTextView tvStudentName = dialogView.findViewById(R.id.tv_web_student_name);
        MaterialTextView tvStudentLog = dialogView.findViewById(R.id.tv_web_student_log);
        MaterialTextView tvTitle = dialogView.findViewById(R.id.tv_web_title);
        MaterialTextView tvHost = dialogView.findViewById(R.id.tv_web_host);
        MaterialTextView tvUrl = dialogView.findViewById(R.id.tv_web_url);
        MaterialTextView tvTimestamp = dialogView.findViewById(R.id.tv_web_timestamp);

        tvStudentName.setText(safeText(webUsageLog.getStudentName()));
        tvStudentLog.setText("Log: " + safeText(webUsageLog.getStudentLog()));
        tvTitle.setText("Title: " + safeText(webUsageLog.getTitle()));
        tvHost.setText("Host: " + safeText(webUsageLog.getHost()));
        tvUrl.setText("URL: " + safeText(webUsageLog.getUrl()));
        tvTimestamp.setText("Timestamp: " + formattedDate(webUsageLog.getTimestamp()));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(whitelistedHosts.contains(webUsageLog.getHost()) ? "Remove Whitelist" : "Whitelist Website",
                        (dialog, which) -> confirmToggleWebsiteWhitelist(webUsageLog))
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmToggleWebsiteWhitelist(WebUsageLog webUsageLog) {
        String host = webUsageLog.getHost();
        if (host == null || host.trim().isEmpty()) {
            Toast.makeText(this, "No host found for this website", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isWhitelisted = whitelistedHosts.contains(host);
        String title = safeText(webUsageLog.getTitle()).equals("Unknown") ? host : webUsageLog.getTitle();
        String dialogTitle = isWhitelisted ? "Remove from whitelist?" : "Add to whitelist?";
        String message = title + "\n" + host;

        new MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setMessage(message)
                .setPositiveButton(isWhitelisted ? "Remove" : "Add", (dialog, which) -> {
                    if (isWhitelisted) {
                        removeWebsiteFromWhitelist(host, title);
                    } else {
                        addWebsiteToWhitelist(host, title, webUsageLog.getUrl());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addWebsiteToWhitelist(String host, String title, String url) {
        WhitelistedWebsite website = new WhitelistedWebsite(host, title, url, System.currentTimeMillis());
        firestoreRepo.addWhitelistedWebsite(classCode, website,
                unused -> {
                    whitelistedHosts.add(host);
                    applyFiltersAndRender();
                    Toast.makeText(this, host + " whitelisted", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(this, "Could not whitelist website", Toast.LENGTH_SHORT).show());
    }

    private void removeWebsiteFromWhitelist(String host, String title) {
        firestoreRepo.removeWhitelistedWebsite(classCode, host,
                unused -> {
                    whitelistedHosts.remove(host);
                    applyFiltersAndRender();
                    Toast.makeText(this, title + " removed", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(this, "Could not remove website", Toast.LENGTH_SHORT).show());
    }

    private void showWhitelistedWebsitesDialog() {
        firestoreRepo.fetchWhitelistedWebsites(classCode, querySnapshot -> {
            List<WhitelistedWebsite> websites = querySnapshot.toObjects(WhitelistedWebsite.class);
            if (websites.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Whitelisted Websites")
                        .setMessage("No websites are whitelisted for this class.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            String[] websiteLabels = new String[websites.size()];
            for (int i = 0; i < websites.size(); i++) {
                WhitelistedWebsite website = websites.get(i);
                websiteLabels[i] = website.getHost() + "\n" + safeText(website.getTitle());
            }

            final int[] selectedIndex = {-1};
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Whitelisted Websites")
                    .setSingleChoiceItems(websiteLabels, -1, (choiceDialog, which) -> selectedIndex[0] = which)
                    .setPositiveButton("Remove", null)
                    .setNegativeButton("Close", null)
                    .show();

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        if (selectedIndex[0] == -1) {
                            Toast.makeText(this, "Select a website to remove", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        confirmRemoveWhitelistedWebsite(websites.get(selectedIndex[0]), dialog);
                    });
        }, e -> Toast.makeText(this, "Could not load whitelisted websites", Toast.LENGTH_SHORT).show());
    }

    private void confirmRemoveWhitelistedWebsite(WhitelistedWebsite website,
                                                 androidx.appcompat.app.AlertDialog listDialog) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove from whitelist?")
                .setMessage(website.getHost() + "\n" + safeText(website.getTitle()))
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeWebsiteFromWhitelist(website.getHost(), safeText(website.getTitle()));
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
        String[] options = {"Filter by host", "Filter by student name", "Clear filters"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Host/Student Filter")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showValueFilterDialog(true);
                    } else if (which == 1) {
                        showValueFilterDialog(false);
                    } else {
                        hostFilter = null;
                        studentNameFilter = null;
                        applyFiltersAndRender();
                    }
                })
                .show();
    }

    private void showValueFilterDialog(boolean filterByHost) {
        List<String> values = new ArrayList<>(getUniqueValues(filterByHost));
        values.add(0, filterByHost ? "All hosts" : "All students");

        String[] options = values.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(filterByHost ? "Filter by Host" : "Filter by Student")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (which == 0) {
                        if (filterByHost) {
                            hostFilter = null;
                        } else {
                            studentNameFilter = null;
                        }
                    } else if (filterByHost) {
                        hostFilter = selected;
                    } else {
                        studentNameFilter = selected;
                    }
                    applyFiltersAndRender();
                })
                .show();
    }

    private Set<String> getUniqueValues(boolean hosts) {
        Set<String> values = new TreeSet<>();
        for (WebUsageLog webUsageLog : allWebUsageLogList) {
            values.add(safeText(hosts ? webUsageLog.getHost() : webUsageLog.getStudentName()));
        }
        return values;
    }

    private void showGroupDialog() {
        String[] options = {"None", "Host", "Student name"};
        int checked = groupMode == GroupMode.HOST ? 1 : groupMode == GroupMode.STUDENT ? 2 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Group By")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    groupMode = which == 1 ? GroupMode.HOST : which == 2 ? GroupMode.STUDENT : GroupMode.NONE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showViewModeDialog() {
        String[] options = {"Card view", "Sortable table view"};
        int checked = viewMode == WebUsageLogAdapter.ViewMode.CARD ? 0 : 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle("View Mode")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    viewMode = which == 0 ? WebUsageLogAdapter.ViewMode.CARD : WebUsageLogAdapter.ViewMode.TABLE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Newest first", "Oldest first", "Host", "Student name"};
        int checked = sortMode == SortMode.TIME_ASC ? 1 : sortMode == SortMode.HOST ? 2 : sortMode == SortMode.STUDENT ? 3 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    sortMode = which == 1 ? SortMode.TIME_ASC : which == 2 ? SortMode.HOST : which == 3 ? SortMode.STUDENT : SortMode.TIME_DESC;
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
        binding.pbLoadingWebUsage.setVisibility(View.VISIBLE);
        binding.swipeRefreshWebUsage.setVisibility(View.GONE);
        binding.tvNoWebUsage.setVisibility(View.GONE);
    }

    private void showMainView() {
        binding.pbLoadingWebUsage.setVisibility(View.GONE);
        binding.swipeRefreshWebUsage.setVisibility(View.VISIBLE);
        binding.rvWebUsage.setVisibility(View.VISIBLE);
        binding.tvNoWebUsage.setVisibility(View.GONE);
    }

    private void showNoWebUsage(String message) {
        binding.pbLoadingWebUsage.setVisibility(View.GONE);
        binding.swipeRefreshWebUsage.setVisibility(View.VISIBLE);
        binding.rvWebUsage.setVisibility(View.GONE);
        binding.tvNoWebUsage.setText(message);
        binding.tvNoWebUsage.setVisibility(View.VISIBLE);
    }
}
