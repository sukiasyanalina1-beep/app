package com.example.recipeapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.recipeapp.databinding.ActivityMainBinding;
import com.example.recipeapp.utils.DatabaseSeeder;
import com.example.recipeapp.utils.ImageFetcher;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        DatabaseSeeder.seedIfEmpty(this);
        ImageFetcher.fetchAndUpdateImages();
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        DatabaseSeeder.seedEquipmentIfEmpty(this);
    }
}