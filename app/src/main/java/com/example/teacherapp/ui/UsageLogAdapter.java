package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.UsageLog;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UsageLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum ViewMode {
        CARD,
        TABLE
    }

    public static class DisplayItem {
        private final String groupTitle;
        private final UsageLog usageLog;

        private DisplayItem(String groupTitle, UsageLog usageLog) {
            this.groupTitle = groupTitle;
            this.usageLog = usageLog;
        }

        public static DisplayItem group(String title) {
            return new DisplayItem(title, null);
        }

        public static DisplayItem log(UsageLog usageLog) {
            return new DisplayItem(null, usageLog);
        }

        public boolean isGroup() {
            return groupTitle != null;
        }
    }

    public interface OnUsageLogActionListener {
        void onLogClick(UsageLog usageLog);
        void onWhitelistClick(UsageLog usageLog);
    }

    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_CARD = 1;
    private static final int VIEW_TYPE_TABLE = 2;

    private final List<DisplayItem> displayItems;
    private final OnUsageLogActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private Set<String> whitelistedPackages;
    private ViewMode viewMode;

    public UsageLogAdapter(List<DisplayItem> displayItems,
                           Set<String> whitelistedPackages,
                           ViewMode viewMode,
                           OnUsageLogActionListener listener) {
        this.displayItems = displayItems;
        this.whitelistedPackages = new HashSet<>(whitelistedPackages);
        this.viewMode = viewMode;
        this.listener = listener;
    }

    public void setItems(List<DisplayItem> items) {
        displayItems.clear();
        displayItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setWhitelistedPackages(Set<String> packages) {
        whitelistedPackages = new HashSet<>(packages);
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
            View view = inflater.inflate(R.layout.item_usage_log_table, parent, false);
            return new TableViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_usage_log, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = displayItems.get(position);
        if (holder instanceof GroupViewHolder) {
            ((GroupViewHolder) holder).tvGroupTitle.setText(item.groupTitle);
            return;
        }

        UsageLog usageLog = item.usageLog;
        boolean isWhitelisted = whitelistedPackages.contains(usageLog.getPackageName());
        if (holder instanceof TableViewHolder) {
            bindTable((TableViewHolder) holder, usageLog, isWhitelisted);
        } else if (holder instanceof CardViewHolder) {
            bindCard((CardViewHolder) holder, usageLog, isWhitelisted);
        }
    }

    private void bindCard(CardViewHolder holder, UsageLog usageLog, boolean isWhitelisted) {
        holder.tvUsageLog.setText(usageLog.getStudentLog());
        holder.tvUsageStudentName.setText("By - " + safeText(usageLog.getStudentName()));
        holder.tvUsageAppName.setText(safeText(usageLog.getAppName()));
        holder.tvUsageDate.setText(dateFormat.format(usageLog.getTimestamp()));
        holder.tvUsageTime.setText(timeFormat.format(usageLog.getTimestamp()));
        bindCommon(holder.itemView, holder.btnWhitelist, usageLog, isWhitelisted);
    }

    private void bindTable(TableViewHolder holder, UsageLog usageLog, boolean isWhitelisted) {
        holder.tvTableApp.setText(safeText(usageLog.getAppName()));
        holder.tvTableStudent.setText(safeText(usageLog.getStudentName()));
        holder.tvTableDate.setText(dateFormat.format(usageLog.getTimestamp()));
        holder.tvTableTime.setText(timeFormat.format(usageLog.getTimestamp()));
        bindCommon(holder.itemView, holder.btnWhitelist, usageLog, isWhitelisted);
    }

    private void bindCommon(View itemView, ImageButton btnWhitelist, UsageLog usageLog, boolean isWhitelisted) {
        btnWhitelist.setSelected(isWhitelisted);
        btnWhitelist.setImageResource(isWhitelisted ? R.drawable.ic_whitelist_filled : R.drawable.ic_whitelist_outline);
        btnWhitelist.setContentDescription(isWhitelisted ? "Remove from whitelist" : "Add to whitelist");
        btnWhitelist.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWhitelistClick(usageLog);
            }
        });
        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogClick(usageLog);
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
        MaterialTextView tvUsageLog, tvUsageStudentName, tvUsageAppName, tvUsageDate, tvUsageTime;
        ImageButton btnWhitelist;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsageLog = itemView.findViewById(R.id.tv_usage_log);
            tvUsageStudentName = itemView.findViewById(R.id.tv_usage_log_student_name);
            tvUsageAppName = itemView.findViewById(R.id.tv_usage_log_app_name);
            tvUsageDate = itemView.findViewById(R.id.tv_usage_log_date);
            tvUsageTime = itemView.findViewById(R.id.tv_usage_log_time);
            btnWhitelist = itemView.findViewById(R.id.btn_usage_whitelist);
        }
    }

    public static class TableViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvTableApp, tvTableStudent, tvTableDate, tvTableTime;
        ImageButton btnWhitelist;

        public TableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableApp = itemView.findViewById(R.id.tv_usage_table_app);
            tvTableStudent = itemView.findViewById(R.id.tv_usage_table_student);
            tvTableDate = itemView.findViewById(R.id.tv_usage_table_date);
            tvTableTime = itemView.findViewById(R.id.tv_usage_table_time);
            btnWhitelist = itemView.findViewById(R.id.btn_usage_whitelist);
        }
    }
}
