package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.QuizSubmission;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class QuizSubmissionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum ViewMode {
        CARD,
        TABLE
    }

    public static class DisplayItem {
        private final String groupTitle;
        private final QuizSubmission submission;

        private DisplayItem(String groupTitle, QuizSubmission submission) {
            this.groupTitle = groupTitle;
            this.submission = submission;
        }

        public static DisplayItem group(String title) {
            return new DisplayItem(title, null);
        }

        public static DisplayItem submission(QuizSubmission submission) {
            return new DisplayItem(null, submission);
        }

        public boolean isGroup() {
            return groupTitle != null;
        }
    }

    public interface OnSubmissionClickListener {
        void onSubmissionClick(QuizSubmission submission);
    }

    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_CARD = 1;
    private static final int VIEW_TYPE_TABLE = 2;

    private final List<DisplayItem> displayItems;
    private final OnSubmissionClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private ViewMode viewMode;

    public QuizSubmissionAdapter(List<DisplayItem> displayItems,
                                 ViewMode viewMode,
                                 OnSubmissionClickListener listener) {
        this.displayItems = displayItems;
        this.viewMode = viewMode;
        this.listener = listener;
    }

    public void setItems(List<DisplayItem> items) {
        displayItems.clear();
        displayItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        DisplayItem item = displayItems.get(position);
        if (item.isGroup()) {
            return VIEW_TYPE_GROUP;
        }
        return viewMode == ViewMode.TABLE ? VIEW_TYPE_TABLE : VIEW_TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_GROUP) {
            View view = inflater.inflate(R.layout.item_usage_log_group, parent, false);
            return new GroupViewHolder(view);
        }
        if (viewType == VIEW_TYPE_TABLE) {
            View view = inflater.inflate(R.layout.item_quiz_submission_table, parent, false);
            return new TableViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_quiz_submission, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = displayItems.get(position);
        if (holder instanceof GroupViewHolder) {
            ((GroupViewHolder) holder).tvGroupTitle.setText(item.groupTitle);
            return;
        }

        QuizSubmission submission = item.submission;
        if (holder instanceof TableViewHolder) {
            bindTable((TableViewHolder) holder, submission);
        } else if (holder instanceof CardViewHolder) {
            bindCard((CardViewHolder) holder, submission);
        }
    }

    private void bindCard(CardViewHolder holder, QuizSubmission submission) {
        holder.tvStudent.setText(safeText(submission.getStudentName()));
        holder.tvSubject.setText(safeText(submission.getSubject()) + " | " + safeText(submission.getDifficulty()));
        holder.tvChapters.setText(chaptersText(submission));
        holder.tvScore.setText("Score " + submission.getCorrectCount() + "/" + submission.getGradableCount());
        holder.tvAnswered.setText("Answered " + submission.getAnsweredCount() + "/" + submission.getQuestionCount());
        holder.tvDate.setText(dateText(submission.getSubmittedAtMillis()));
        holder.tvTime.setText(timeText(submission.getSubmittedAtMillis()));
        bindCommon(holder.itemView, submission);
    }

    private void bindTable(TableViewHolder holder, QuizSubmission submission) {
        holder.tvStudent.setText(safeText(submission.getStudentName()));
        holder.tvSubject.setText(safeText(submission.getSubject()));
        holder.tvScore.setText(submission.getCorrectCount() + "/" + submission.getGradableCount());
        holder.tvDate.setText(dateText(submission.getSubmittedAtMillis()));
        holder.tvTime.setText(timeText(submission.getSubmittedAtMillis()));
        bindCommon(holder.itemView, submission);
    }

    private void bindCommon(View itemView, QuizSubmission submission) {
        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubmissionClick(submission);
            }
        });
    }

    private String chaptersText(QuizSubmission submission) {
        List<String> chapters = submission.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            return "No chapters";
        }
        return joinStrings(chapters);
    }

    private String dateText(long millis) {
        return millis <= 0 ? "Unknown" : dateFormat.format(millis);
    }

    private String timeText(long millis) {
        return millis <= 0 ? "" : timeFormat.format(millis);
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

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvGroupTitle;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tv_usage_group_title);
        }
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvStudent, tvSubject, tvChapters, tvScore, tvAnswered, tvDate, tvTime;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudent = itemView.findViewById(R.id.tv_submission_student);
            tvSubject = itemView.findViewById(R.id.tv_submission_subject);
            tvChapters = itemView.findViewById(R.id.tv_submission_chapters);
            tvScore = itemView.findViewById(R.id.tv_submission_score);
            tvAnswered = itemView.findViewById(R.id.tv_submission_answered);
            tvDate = itemView.findViewById(R.id.tv_submission_date);
            tvTime = itemView.findViewById(R.id.tv_submission_time);
        }
    }

    public static class TableViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvStudent, tvSubject, tvScore, tvDate, tvTime;

        public TableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudent = itemView.findViewById(R.id.tv_submission_table_student);
            tvSubject = itemView.findViewById(R.id.tv_submission_table_subject);
            tvScore = itemView.findViewById(R.id.tv_submission_table_score);
            tvDate = itemView.findViewById(R.id.tv_submission_table_date);
            tvTime = itemView.findViewById(R.id.tv_submission_table_time);
        }
    }
}
