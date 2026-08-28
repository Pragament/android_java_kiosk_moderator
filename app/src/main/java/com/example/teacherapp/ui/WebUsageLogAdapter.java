package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.WebUsageLog;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WebUsageLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum ViewMode {
        CARD,
        TABLE
    }

    public static class DisplayItem {
        private final String groupTitle;
        private final WebUsageLog webUsageLog;

        private DisplayItem(String groupTitle, WebUsageLog webUsageLog) {
            this.groupTitle = groupTitle;
            this.webUsageLog = webUsageLog;
        }

        public static DisplayItem group(String title) {
            return new DisplayItem(title, null);
        }

        public static DisplayItem log(WebUsageLog webUsageLog) {
            return new DisplayItem(null, webUsageLog);
        }

        public boolean isGroup() {
            return groupTitle != null;
        }
    }

    public interface OnWebUsageLogClickListener {
        void onLogClick(WebUsageLog webUsageLog);
    }

    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_CARD = 1;
    private static final int VIEW_TYPE_TABLE = 2;

    private final List<DisplayItem> displayItems;
    private final OnWebUsageLogClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private ViewMode viewMode;

    public WebUsageLogAdapter(List<DisplayItem> displayItems,
                              ViewMode viewMode,
                              OnWebUsageLogClickListener listener) {
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
            View view = inflater.inflate(R.layout.item_web_usage_table, parent, false);
            return new TableViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_web_usage_log, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = displayItems.get(position);
        if (holder instanceof GroupViewHolder) {
            ((GroupViewHolder) holder).tvGroupTitle.setText(item.groupTitle);
            return;
        }

        WebUsageLog webUsageLog = item.webUsageLog;
        if (holder instanceof TableViewHolder) {
            bindTable((TableViewHolder) holder, webUsageLog);
        } else if (holder instanceof CardViewHolder) {
            bindCard((CardViewHolder) holder, webUsageLog);
        }
    }

    private void bindCard(CardViewHolder holder, WebUsageLog webUsageLog) {
        holder.tvWebLog.setText(safeText(webUsageLog.getStudentLog()));
        holder.tvWebHost.setText(safeText(webUsageLog.getHost()));
        holder.tvWebStudentName.setText("By - " + safeText(webUsageLog.getStudentName()));
        holder.tvWebDate.setText(dateFormat.format(webUsageLog.getTimestamp()));
        holder.tvWebTime.setText(timeFormat.format(webUsageLog.getTimestamp()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogClick(webUsageLog);
            }
        });
    }

    private void bindTable(TableViewHolder holder, WebUsageLog webUsageLog) {
        holder.tvWebHost.setText(safeText(webUsageLog.getHost()));
        holder.tvWebStudent.setText(safeText(webUsageLog.getStudentName()));
        holder.tvWebDate.setText(dateFormat.format(webUsageLog.getTimestamp()));
        holder.tvWebTime.setText(timeFormat.format(webUsageLog.getTimestamp()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogClick(webUsageLog);
            }
        });
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
        MaterialTextView tvWebLog, tvWebHost, tvWebStudentName, tvWebDate, tvWebTime;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWebLog = itemView.findViewById(R.id.tv_web_log);
            tvWebHost = itemView.findViewById(R.id.tv_web_host);
            tvWebStudentName = itemView.findViewById(R.id.tv_web_student_name);
            tvWebDate = itemView.findViewById(R.id.tv_web_date);
            tvWebTime = itemView.findViewById(R.id.tv_web_time);
        }
    }

    public static class TableViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvWebHost, tvWebStudent, tvWebDate, tvWebTime;

        public TableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWebHost = itemView.findViewById(R.id.tv_web_table_host);
            tvWebStudent = itemView.findViewById(R.id.tv_web_table_student);
            tvWebDate = itemView.findViewById(R.id.tv_web_table_date);
            tvWebTime = itemView.findViewById(R.id.tv_web_table_time);
        }
    }
}
