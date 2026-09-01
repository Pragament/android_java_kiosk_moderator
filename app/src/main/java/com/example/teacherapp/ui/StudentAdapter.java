package com.example.teacherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teacherapp.R;
import com.example.teacherapp.model.Student;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private final List<Student> students;

    public StudentAdapter(List<Student> students) {
        this.students = students;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);
        holder.tvStudentName.setText(safeText(student.getName(), "Unnamed student"));
        holder.tvStudentDetails.setText("Admission: " + safeText(student.getAdmissionNo(), "Unknown")
                + " | Phone: " + safeText(student.getPhone(), "Unknown"));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvStudentName;
        MaterialTextView tvStudentDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_item_student_name);
            tvStudentDetails = itemView.findViewById(R.id.tv_item_student_details);
        }
    }
}
