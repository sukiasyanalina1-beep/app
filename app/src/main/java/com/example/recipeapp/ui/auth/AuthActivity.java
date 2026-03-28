package com.example.recipeapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.recipeapp.MainActivity;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()) {
            // Already logged in — go straight to app
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Not logged in — go to login page
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}