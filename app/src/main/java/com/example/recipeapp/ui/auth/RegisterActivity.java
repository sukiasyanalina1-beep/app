package com.example.recipeapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.recipeapp.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String displayName = binding.etName.getText().toString().trim();
        String email      = binding.etEmail.getText().toString().trim();
        String password   = binding.etPassword.getText().toString().trim();
        String confirm    = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(displayName)) {
            binding.etName.setError("Name is required");
            binding.etName.requestFocus();
            return;
        }
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
        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }

        binding.btnRegister.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    result.getUser().sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                UserRepository.createProfile(
                                        result.getUser().getUid(), displayName, email);
                                mAuth.signOut();
                                binding.btnRegister.setEnabled(true);
                                binding.progressBar.setVisibility(View.GONE);
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Verify your email")
                                        .setMessage("A verification link has been sent to " + email +
                                                ".\n\nPlease check your inbox and click the link before logging in.")
                                        .setPositiveButton("Go to login", (dialog, which) -> {
                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        })
                                        .setCancelable(false)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnRegister.setEnabled(true);
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(this,
                                        "Failed to send verification email: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnRegister.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}