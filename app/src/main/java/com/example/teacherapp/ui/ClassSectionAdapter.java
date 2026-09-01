package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.ClassSection;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ClassSectionAdapter extends RecyclerView.Adapter<ClassSectionAdapter.ViewHolder> {

    private final List<ClassSection> classSections;
    private final OnClassSectionClickListener listener;

    public interface OnClassSectionClickListener {
        void onClassSectionClick(ClassSection classSection);
    }

    public ClassSectionAdapter(List<ClassSection> classSections, OnClassSectionClickListener listener) {
        this.classSections = classSections;
        this.listener = listener;
    }

    public void setItems(List<ClassSection> sections) {
        classSections.clear();
        classSections.addAll(sections);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_section, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassSection classSection = classSections.get(position);
        holder.tvSectionName.setText(safeText(classSection.getSectionName()));
        long studentCount = classSection.getStudentCount() == null ? 0 : classSection.getStudentCount();
        holder.tvSectionCount.setText(studentCount + " students");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClassSectionClick(classSection);
            }
        });
    }

    @Override
    public int getItemCount() {
        return classSections.size();
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "Unnamed section" : value;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvSectionName;
        MaterialTextView tvSectionCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectionName = itemView.findViewById(R.id.tv_item_section_name);
            tvSectionCount = itemView.findViewById(R.id.tv_item_section_count);
        }
    }
}
