package com.example.teacherapp.submissions;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.teacherapp.R;
import com.example.teacherapp.classdetail.ClassDetailActivity;
import com.example.teacherapp.data.FirestoreRepo;
import com.example.teacherapp.databinding.ActivityQuizSubmissionsBinding;
import com.example.teacherapp.databinding.DialogQuizSubmissionBinding;
import com.example.teacherapp.model.QuizAnswer;
import com.example.teacherapp.model.QuizSubmission;
import com.example.teacherapp.ui.QuizSubmissionAdapter;
import com.example.teacherapp.webusage.WebUsageActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class QuizSubmissionsActivity extends AppCompatActivity {

    private enum GroupMode {
        NONE,
        STUDENT,
        SUBJECT
    }

    private enum SortMode {
        TIME_DESC,
        TIME_ASC,
        STUDENT,
        SUBJECT,
        SCORE
    }

    private ActivityQuizSubmissionsBinding binding;
    private FirestoreRepo firestoreRepo;
    private final List<QuizSubmission> allSubmissions = new ArrayList<>();
    private final List<QuizSubmissionAdapter.DisplayItem> displayItems = new ArrayList<>();
    private QuizSubmissionAdapter adapter;
    private String classCode;
    private GroupMode groupMode = GroupMode.NONE;
    private SortMode sortMode = SortMode.TIME_DESC;
    private QuizSubmissionAdapter.ViewMode viewMode = QuizSubmissionAdapter.ViewMode.CARD;
    private String studentNameFilter;
    private String subjectFilter;
    private String difficultyFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizSubmissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyStatusBarInsets();

        firestoreRepo = new FirestoreRepo();
        classCode = getIntent().getStringExtra("class_code");

        MaterialToolbar toolbar = findViewById(R.id.toolbar_quiz_submissions);
        toolbar.setTitle("Quiz Submissions");
        toolbar.setSubtitle(getIntent().getStringExtra("class_name"));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.btn_menu_submissions_refresh) {
                loadSubmissions();
            } else if (id == R.id.btn_menu_submissions_filter) {
                showFilterTypeDialog();
            } else if (id == R.id.btn_menu_submissions_group) {
                showGroupDialog();
            } else if (id == R.id.btn_menu_submissions_view_mode) {
                showViewModeDialog();
            } else if (id == R.id.btn_menu_submissions_sort) {
                showSortDialog();
            }
            return true;
        });
        setupBottomNavigation();

        binding.swipeRefreshQuizSubmissions.setOnRefreshListener(this::loadSubmissions);
        setupRecyclerView();
        loadSubmissions();
    }

    private void setupBottomNavigation() {
        binding.bottomNavClassroom.setSelectedItemId(R.id.bottom_nav_quiz);
        binding.bottomNavClassroom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.bottom_nav_quiz) {
                return true;
            }
            if (id == R.id.bottom_nav_apps) {
                Intent appsIntent = new Intent(this, ClassDetailActivity.class)
                        .putExtra("class_code", classCode)
                        .putExtra("class_name", getIntent().getStringExtra("class_name"))
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(appsIntent);
                return true;
            }
            if (id == R.id.bottom_nav_websites) {
                Intent webIntent = new Intent(this, WebUsageActivity.class)
                        .putExtra("class_code", classCode)
                        .putExtra("class_name", getIntent().getStringExtra("class_name"));
                startActivity(webIntent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void applyStatusBarInsets() {
        View appBar = findViewById(R.id.app_bar_quiz_submissions);
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
        adapter = new QuizSubmissionAdapter(displayItems, viewMode, this::showSubmissionDialog);
        binding.rvQuizSubmissions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvQuizSubmissions.setAdapter(adapter);
    }

    private void loadSubmissions() {
        if (!binding.swipeRefreshQuizSubmissions.isRefreshing()) {
            showLoading();
        }

        firestoreRepo.fetchQuizSubmissions(classCode, querySnapshot -> {
            binding.swipeRefreshQuizSubmissions.setRefreshing(false);
            allSubmissions.clear();
            allSubmissions.addAll(querySnapshot.toObjects(QuizSubmission.class));
            applyFiltersAndRender();
        }, e -> {
            binding.swipeRefreshQuizSubmissions.setRefreshing(false);
            showNoSubmissions("Error loading quiz submissions!");
        });
    }

    private void applyFiltersAndRender() {
        List<QuizSubmission> filteredSubmissions = new ArrayList<>();
        for (QuizSubmission submission : allSubmissions) {
            if (studentNameFilter != null && !studentNameFilter.equals(safeText(submission.getStudentName()))) {
                continue;
            }
            if (subjectFilter != null && !subjectFilter.equals(safeText(submission.getSubject()))) {
                continue;
            }
            if (difficultyFilter != null && !difficultyFilter.equals(safeText(submission.getDifficulty()))) {
                continue;
            }
            filteredSubmissions.add(submission);
        }

        filteredSubmissions.sort(getComparator());

        List<QuizSubmissionAdapter.DisplayItem> newDisplayItems = new ArrayList<>();
        String currentGroup = null;
        for (QuizSubmission submission : filteredSubmissions) {
            String groupTitle = getGroupTitle(submission);
            if (groupTitle != null && !groupTitle.equals(currentGroup)) {
                currentGroup = groupTitle;
                newDisplayItems.add(QuizSubmissionAdapter.DisplayItem.group(groupTitle));
            }
            newDisplayItems.add(QuizSubmissionAdapter.DisplayItem.submission(submission));
        }

        adapter.setViewMode(viewMode);
        adapter.setItems(newDisplayItems);
        updateFilterSummary(filteredSubmissions.size());

        if (filteredSubmissions.isEmpty()) {
            showNoSubmissions("No quiz submissions for selected filters.");
        } else {
            showMainView();
        }
    }

    private Comparator<QuizSubmission> getComparator() {
        if (sortMode == SortMode.TIME_ASC) {
            return Comparator.comparingLong(QuizSubmission::getSubmittedAtMillis);
        }
        if (sortMode == SortMode.STUDENT) {
            return Comparator.comparing(submission -> safeText(submission.getStudentName()));
        }
        if (sortMode == SortMode.SUBJECT) {
            return Comparator.comparing(submission -> safeText(submission.getSubject()));
        }
        if (sortMode == SortMode.SCORE) {
            return (left, right) -> Double.compare(scorePercent(right), scorePercent(left));
        }
        return (left, right) -> Long.compare(right.getSubmittedAtMillis(), left.getSubmittedAtMillis());
    }

    private double scorePercent(QuizSubmission submission) {
        int gradableCount = submission.getGradableCount();
        if (gradableCount <= 0) {
            return 0;
        }
        return (double) submission.getCorrectCount() / gradableCount;
    }

    private String getGroupTitle(QuizSubmission submission) {
        if (groupMode == GroupMode.STUDENT) {
            return safeText(submission.getStudentName());
        }
        if (groupMode == GroupMode.SUBJECT) {
            return safeText(submission.getSubject());
        }
        return null;
    }

    private void updateFilterSummary(int count) {
        String studentText = studentNameFilter == null ? "All students" : studentNameFilter;
        String subjectText = subjectFilter == null ? "All subjects" : subjectFilter;
        String difficultyText = difficultyFilter == null ? "All difficulties" : difficultyFilter;
        String groupText = groupMode == GroupMode.NONE ? "No grouping" : "Grouped by " + groupMode.name().toLowerCase(Locale.US);
        String viewText = viewMode == QuizSubmissionAdapter.ViewMode.CARD ? "Cards" : "Table";
        binding.tvSubmissionFilterSummary.setText(studentText + " | " + subjectText + " | " + difficultyText + " | " + groupText + " | " + viewText + " | " + count + " submissions");
    }

    private void showSubmissionDialog(QuizSubmission submission) {
        DialogQuizSubmissionBinding dialogBinding = DialogQuizSubmissionBinding.inflate(getLayoutInflater());
        dialogBinding.tvQuizStudentName.setText(safeText(submission.getStudentName()));
        dialogBinding.tvQuizSummary.setText("Score: " + submission.getCorrectCount() + "/" + submission.getGradableCount()
                + " | Answered: " + submission.getAnsweredCount() + "/" + submission.getQuestionCount());
        dialogBinding.tvQuizMeta.setText("Admission No: " + safeText(submission.getAdmissionNo())
                + "\nClass: " + safeText(submission.getClassName())
                + "\nSubject: " + safeText(submission.getSubject())
                + "\nDifficulty: " + safeText(submission.getDifficulty())
                + "\nChapters: " + chaptersText(submission)
                + "\nSubmitted: " + formattedDate(submission.getSubmittedAtMillis()));
        dialogBinding.tvQuizAnswers.setText(buildAnswersText(submission));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Close", null)
                .show();
    }

    private String buildAnswersText(QuizSubmission submission) {
        List<QuizAnswer> answers = submission.getAnswers();
        if (answers == null || answers.isEmpty()) {
            return "No stored answers found for this submission.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < answers.size(); i++) {
            QuizAnswer answer = answers.get(i);
            builder.append("Q").append(i + 1).append(" ");
            builder.append("(").append(safeText(answer.getType())).append(")").append("\n");
            builder.append(htmlText(answer.getPromptHtml())).append("\n");
            builder.append("Student Answer: ").append(answerText(answer)).append("\n");
            builder.append("Correct Answer: ").append(htmlText(answer.getCorrectAnswer())).append("\n");
            builder.append("Result: ").append(resultText(answer.getIsCorrect())).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String answerText(QuizAnswer answer) {
        if (!isBlank(answer.getDisplayAnswer())) {
            return htmlText(answer.getDisplayAnswer());
        }
        if (!isBlank(answer.getShortAnswer())) {
            return answer.getShortAnswer();
        }
        if (answer.getTrueFalseAnswer() != null) {
            return answer.getTrueFalseAnswer() ? "True" : "False";
        }
        if (answer.getFibAnswers() != null && !answer.getFibAnswers().isEmpty()) {
            return joinStrings(answer.getFibAnswers());
        }
        return "Not answered";
    }

    private String resultText(Boolean isCorrect) {
        if (isCorrect == null) {
            return "Manual review";
        }
        return isCorrect ? "Correct" : "Incorrect";
    }

    private void showFilterTypeDialog() {
        String[] options = {"Filter by student", "Filter by subject", "Filter by difficulty", "Clear filters"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Student/Subject Filter")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showValueFilterDialog(FilterType.STUDENT);
                    } else if (which == 1) {
                        showValueFilterDialog(FilterType.SUBJECT);
                    } else if (which == 2) {
                        showValueFilterDialog(FilterType.DIFFICULTY);
                    } else {
                        studentNameFilter = null;
                        subjectFilter = null;
                        difficultyFilter = null;
                        applyFiltersAndRender();
                    }
                })
                .show();
    }

    private enum FilterType {
        STUDENT,
        SUBJECT,
        DIFFICULTY
    }

    private void showValueFilterDialog(FilterType filterType) {
        List<String> values = new ArrayList<>(getUniqueValues(filterType));
        values.add(0, "All " + filterLabelPlural(filterType));

        String[] options = values.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter by " + filterLabel(filterType))
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (which == 0) {
                        setFilterValue(filterType, null);
                    } else {
                        setFilterValue(filterType, selected);
                    }
                    applyFiltersAndRender();
                })
                .show();
    }

    private Set<String> getUniqueValues(FilterType filterType) {
        Set<String> values = new TreeSet<>();
        for (QuizSubmission submission : allSubmissions) {
            if (filterType == FilterType.STUDENT) {
                values.add(safeText(submission.getStudentName()));
            } else if (filterType == FilterType.SUBJECT) {
                values.add(safeText(submission.getSubject()));
            } else {
                values.add(safeText(submission.getDifficulty()));
            }
        }
        return values;
    }

    private void setFilterValue(FilterType filterType, String value) {
        if (filterType == FilterType.STUDENT) {
            studentNameFilter = value;
        } else if (filterType == FilterType.SUBJECT) {
            subjectFilter = value;
        } else {
            difficultyFilter = value;
        }
    }

    private String filterLabel(FilterType filterType) {
        if (filterType == FilterType.STUDENT) {
            return "Student";
        }
        if (filterType == FilterType.SUBJECT) {
            return "Subject";
        }
        return "Difficulty";
    }

    private String filterLabelPlural(FilterType filterType) {
        if (filterType == FilterType.STUDENT) {
            return "students";
        }
        if (filterType == FilterType.SUBJECT) {
            return "subjects";
        }
        return "difficulties";
    }

    private void showGroupDialog() {
        String[] options = {"None", "Student name", "Subject"};
        int checked = groupMode == GroupMode.STUDENT ? 1 : groupMode == GroupMode.SUBJECT ? 2 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Group By")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    groupMode = which == 1 ? GroupMode.STUDENT : which == 2 ? GroupMode.SUBJECT : GroupMode.NONE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showViewModeDialog() {
        String[] options = {"Card view", "Sortable table view"};
        int checked = viewMode == QuizSubmissionAdapter.ViewMode.CARD ? 0 : 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle("View Mode")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    viewMode = which == 0 ? QuizSubmissionAdapter.ViewMode.CARD : QuizSubmissionAdapter.ViewMode.TABLE;
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Newest first", "Oldest first", "Student name", "Subject", "Score"};
        int checked = sortMode == SortMode.TIME_ASC ? 1
                : sortMode == SortMode.STUDENT ? 2
                : sortMode == SortMode.SUBJECT ? 3
                : sortMode == SortMode.SCORE ? 4 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    if (which == 1) {
                        sortMode = SortMode.TIME_ASC;
                    } else if (which == 2) {
                        sortMode = SortMode.STUDENT;
                    } else if (which == 3) {
                        sortMode = SortMode.SUBJECT;
                    } else if (which == 4) {
                        sortMode = SortMode.SCORE;
                    } else {
                        sortMode = SortMode.TIME_DESC;
                    }
                    applyFiltersAndRender();
                    dialog.dismiss();
                })
                .show();
    }

    private String chaptersText(QuizSubmission submission) {
        List<String> chapters = submission.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            return "No chapters";
        }
        return joinStrings(chapters);
    }

    private String joinStrings(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String formattedDate(long millis) {
        if (millis <= 0) {
            return "Unknown";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private String htmlText(String value) {
        if (isBlank(value)) {
            return "Unknown";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        }
        return Html.fromHtml(value).toString().trim();
    }

    private String safeText(String value) {
        return isBlank(value) ? "Unknown" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showLoading() {
        binding.pbLoadingQuizSubmissions.setVisibility(View.VISIBLE);
        binding.swipeRefreshQuizSubmissions.setVisibility(View.GONE);
        binding.tvNoQuizSubmissions.setVisibility(View.GONE);
    }

    private void showMainView() {
        binding.pbLoadingQuizSubmissions.setVisibility(View.GONE);
        binding.swipeRefreshQuizSubmissions.setVisibility(View.VISIBLE);
        binding.rvQuizSubmissions.setVisibility(View.VISIBLE);
        binding.tvNoQuizSubmissions.setVisibility(View.GONE);
    }

    private void showNoSubmissions(String message) {
        binding.pbLoadingQuizSubmissions.setVisibility(View.GONE);
        binding.swipeRefreshQuizSubmissions.setVisibility(View.VISIBLE);
        binding.rvQuizSubmissions.setVisibility(View.GONE);
        binding.tvNoQuizSubmissions.setText(message);
        binding.tvNoQuizSubmissions.setVisibility(View.VISIBLE);
    }
}
