package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.Classroom;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<Classroom> classroomList;
    private OnClassroomClickListener listener;

    public interface OnClassroomClickListener {
        void onClassroomClick(Classroom classroom);
    }

    public ClassAdapter(List<Classroom> classroomList, OnClassroomClickListener listener) {
        this.classroomList = classroomList;
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClassroomClick(classroom);
        });
    }

    @Override
    public int getItemCount() {
        return classroomList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvClassName, tvClassCode;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tv_item_class_name);
            tvClassCode = itemView.findViewById(R.id.tv_item_class_code);
        }
    }

}
