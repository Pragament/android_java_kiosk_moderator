package com.example.teacherapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.teacherapp.MainActivity;
import com.example.teacherapp.PrefManager;
import com.example.teacherapp.R;
import com.example.teacherapp.databinding.ActivityLoginBinding;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CredentialManagerCallback;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private CredentialManager credentialManager;
    private Executor executor;
    private static final String TAG = "LoginActivity";
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefManager = new PrefManager(this);

        if (prefManager.isLogin()){
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        }

        firebaseAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        executor = Executors.newSingleThreadExecutor();

        binding.btnLogin.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        String webClientId = getString(R.string.default_web_client_id);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancellationSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(this, request, cancellationSignal, executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleSignInResponse(response);
                    }

                    @Override
                    public void onError(@NonNull androidx.credentials.exceptions.GetCredentialException e) {
                        Log.e(TAG, "Google Sign-In error", e);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this,
                                        "Sign-in failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void handleSignInResponse(GetCredentialResponse response) {
        try {
            Credential credential = response.getCredential();
            if (credential instanceof GoogleIdTokenCredential) {
                GoogleIdTokenCredential googleIdTokenCred = (GoogleIdTokenCredential) credential;
                String idToken = googleIdTokenCred.getIdToken();
                firebaseAuthWithGoogle(idToken);
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "No valid Google credential retrieved",
                                Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling credential", e);
            runOnUiThread(() ->
                    Toast.makeText(this,
                            "Error processing credential",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        prefManager.saveLogin(firebaseAuth.getUid());
                        Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(this,
                                "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
