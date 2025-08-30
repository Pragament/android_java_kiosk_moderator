package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.UsageLog;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class UsageLogAdapter extends RecyclerView.Adapter<UsageLogAdapter.ViewHolder> {

    private List<UsageLog> usageLogList;
    private OnUsageLogClickListener listener;

    public interface OnUsageLogClickListener{
        void onLogClick(UsageLog usageLog);
    }

    public UsageLogAdapter(List<UsageLog> usageLogList, OnUsageLogClickListener listener) {
        this.usageLogList = usageLogList;
        this.listener = listener;
    }
    @NonNull
    @Override
    public UsageLogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_log, parent, false);
        return new UsageLogAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsageLogAdapter.ViewHolder holder, int position) {
        UsageLog usageLog = usageLogList.get(position);
        holder.tvUsageLog.setText(usageLog.getStudentLog());
        holder.tvUsageStudentName.setText("By - " + usageLog.getStudentName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(usageLog);
        });
    }

    @Override
    public int getItemCount() {
        return usageLogList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvUsageLog, tvUsageStudentName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsageLog = itemView.findViewById(R.id.tv_usage_log);
            tvUsageStudentName = itemView.findViewById(R.id.tv_usage_log_student_name);
        }
    }
}
