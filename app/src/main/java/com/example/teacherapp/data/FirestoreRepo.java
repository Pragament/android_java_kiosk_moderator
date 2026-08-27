package com.example.teacherapp.data;

import androidx.annotation.NonNull;

import com.example.teacherapp.model.Classroom;
import com.example.teacherapp.model.UsageLog;
import com.example.teacherapp.model.WhitelistedApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FirestoreRepo {

    private FirebaseFirestore firestore;
    private final String CLASSROOM_COLLECTION = "classrooms";
    private final String WHITELIST_COLLECTION = "whitelistedApps";

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
}
