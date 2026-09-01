package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.Classroom;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<Classroom> classroomList;
    private final OnClassroomClickListener listener;
    private final OnClassroomEditListener editListener;

    public interface OnClassroomClickListener {
        void onClassroomClick(Classroom classroom);
    }

    public interface OnClassroomEditListener {
        void onClassroomEdit(Classroom classroom);
    }

    public ClassAdapter(List<Classroom> classroomList, OnClassroomClickListener listener) {
        this(classroomList, listener, null);
    }

    public ClassAdapter(List<Classroom> classroomList,
                        OnClassroomClickListener listener,
                        OnClassroomEditListener editListener) {
        this.classroomList = classroomList;
        this.listener = listener;
        this.editListener = editListener;
    }

    public void setItems(List<Classroom> classrooms) {
        classroomList.clear();
        classroomList.addAll(classrooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Classroom classroom = classroomList.get(position);
        holder.tvClassName.setText(classroom.getClassName());
        holder.tvClassCode.setText("Code: " + classroom.getClassCode());
        String sectionName = classroom.getSectionName();
        if (sectionName == null || sectionName.trim().isEmpty()) {
            holder.tvClassSection.setVisibility(View.GONE);
        } else {
            holder.tvClassSection.setVisibility(View.VISIBLE);
            holder.tvClassSection.setText("Section: " + sectionName);
        }
        holder.tvClassStatus.setText(statusText(classroom));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClassroomClick(classroom);
        });
        holder.btnEditClassroom.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onClassroomEdit(classroom);
            }
        });
    }

    private String statusText(Classroom classroom) {
        String classStatus = classroom.isClassEnabledOrDefault() ? "Class enabled" : "Class disabled";
        String quizStatus = classroom.isQuizModeEnabledOrDefault() ? "Quiz enabled" : "Quiz disabled";
        return classStatus + " | " + quizStatus;
    }

    @Override
    public int getItemCount() {
        return classroomList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvClassName, tvClassCode, tvClassSection, tvClassStatus;
        ImageButton btnEditClassroom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tv_item_class_name);
            tvClassCode = itemView.findViewById(R.id.tv_item_class_code);
            tvClassSection = itemView.findViewById(R.id.tv_item_class_section);
            tvClassStatus = itemView.findViewById(R.id.tv_item_class_status);
            btnEditClassroom = itemView.findViewById(R.id.btn_edit_classroom);
        }
    }

}
