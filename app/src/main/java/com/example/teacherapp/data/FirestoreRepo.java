package com.example.teacherapp.data;

import androidx.annotation.NonNull;

import com.example.teacherapp.model.ClassSection;
import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.model.Student;
import com.example.teacherapp.model.UsageLog;
import com.example.teacherapp.model.WhitelistedApp;
import com.example.teacherapp.model.WhitelistedWebsite;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class FirestoreRepo {

    private FirebaseFirestore firestore;
    private final String CLASSROOM_COLLECTION = "classrooms";
    private final String CLASS_SECTION_COLLECTION = "classSections";
    private final String STUDENT_COLLECTION = "students";
    private final String WHITELIST_COLLECTION = "whitelistedApps";
    private final String WEBSITE_WHITELIST_COLLECTION = "whitelistedWebsites";
    private final String QUIZ_SUBMISSIONS_COLLECTION = "qb_quiz_submissions_v1";

    public FirestoreRepo() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void createClassroom(Classroom classroom, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference reference = firestore.collection(CLASSROOM_COLLECTION).document(classroom.getClassCode());
        reference.set(classroom)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchClassrooms(String teacherUid, OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .whereEqualTo("creatorId", teacherUid)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public DocumentReference newClassSectionReference() {
        return firestore.collection(CLASS_SECTION_COLLECTION).document();
    }

    public void createClassSection(ClassSection classSection,
                                   OnSuccessListener<Void> onSuccess,
                                   OnFailureListener onFailure) {
        firestore.collection(CLASS_SECTION_COLLECTION)
                .document(classSection.getSectionId())
                .set(classSection)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchClassSections(String teacherUid,
                                   OnSuccessListener<QuerySnapshot> onSuccess,
                                   OnFailureListener onFailure) {
        firestore.collection(CLASS_SECTION_COLLECTION)
                .whereEqualTo("teacherId", teacherUid)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void importStudents(String sectionId,
                               List<Student> students,
                               OnSuccessListener<Void> onSuccess,
                               OnFailureListener onFailure) {
        WriteBatch batch = firestore.batch();
        DocumentReference sectionReference = firestore.collection(CLASS_SECTION_COLLECTION).document(sectionId);

        for (Student student : students) {
            DocumentReference studentReference = sectionReference
                    .collection(STUDENT_COLLECTION)
                    .document(student.getAdmissionNo());
            batch.set(studentReference, student);
        }
        batch.update(sectionReference, "studentCount", (long) students.size());
        batch.commit()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchSpecificClass(String classCode, OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .whereEqualTo("classCode", classCode)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchUsageLogs(String classCode,
                               OnSuccessListener<QuerySnapshot> onSuccess,
                               OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection("usageLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchWebUsageLogs(String classCode,
                                  OnSuccessListener<QuerySnapshot> onSuccess,
                                  OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection("webUsageLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchQuizSubmissions(String classCode,
                                     OnSuccessListener<QuerySnapshot> onSuccess,
                                     OnFailureListener onFailure) {
        firestore.collection(QUIZ_SUBMISSIONS_COLLECTION)
                .whereEqualTo("classroomId", classCode)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addWhitelistedApp(String classCode,
                                  WhitelistedApp app,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WHITELIST_COLLECTION)
                .document(app.getPackageName())
                .set(app)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchWhitelistedApps(String classCode,
                                     OnSuccessListener<QuerySnapshot> onSuccess,
                                     OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WHITELIST_COLLECTION)
                .orderBy("appName", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void removeWhitelistedApp(String classCode,
                                     String packageName,
                                     OnSuccessListener<Void> onSuccess,
                                     OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WHITELIST_COLLECTION)
                .document(packageName)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addWhitelistedWebsite(String classCode,
                                      WhitelistedWebsite website,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WEBSITE_WHITELIST_COLLECTION)
                .document(website.getHost())
                .set(website)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchWhitelistedWebsites(String classCode,
                                         OnSuccessListener<QuerySnapshot> onSuccess,
                                         OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WEBSITE_WHITELIST_COLLECTION)
                .orderBy("host", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void removeWhitelistedWebsite(String classCode,
                                         String host,
                                         OnSuccessListener<Void> onSuccess,
                                         OnFailureListener onFailure) {
        firestore.collection(CLASSROOM_COLLECTION)
                .document(classCode)
                .collection(WEBSITE_WHITELIST_COLLECTION)
                .document(host)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
