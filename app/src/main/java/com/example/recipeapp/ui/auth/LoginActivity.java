package com.example.recipeapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.recipeapp.MainActivity;
import com.example.recipeapp.databinding.ActivityLoginBinding;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvForgotPassword.setOnClickListener(v -> resetPassword());
        binding.tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser().isEmailVerified()) {
                        checkEquipmentAndProceed();
                    } else {
                        mAuth.signOut();
                        binding.btnLogin.setEnabled(true);
                        binding.progressBar.setVisibility(View.GONE);
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Email not verified")
                                .setMessage("Please verify your email before logging in.\n\nCheck your inbox for the verification link.")
                                .setPositiveButton("OK", null)
                                .setNeutralButton("Resend email", (dialog, which) -> {
                                    mAuth.signInWithEmailAndPassword(email, password)
                                            .addOnSuccessListener(r -> {
                                                r.getUser().sendEmailVerification();
                                                mAuth.signOut();
                                                Toast.makeText(this,
                                                        "Verification email resent!",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnLogin.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Login failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkEquipmentAndProceed() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> equipment = (List<String>) doc.get("equipment");
                    if (equipment == null || equipment.isEmpty()) {
                        startActivity(new Intent(this, EquipmentSetupActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Enter your email first");
            binding.etEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Email sent")
                                .setMessage("A password reset link has been sent to " + email +
                                        ".\n\nCheck your inbox and follow the link to reset your password.")
                                .setPositiveButton("OK", null)
                                .show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not send reset email. Make sure the email address is correct.",
                                Toast.LENGTH_LONG).show());
    }
}