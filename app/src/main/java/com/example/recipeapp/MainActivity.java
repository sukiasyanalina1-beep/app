package com.example.recipeapp;

import android.content.Intent;
import android.net.Uri;
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
    private Uri pendingInviteUri = null;

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

        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null) return;
        if ("recipeapp".equals(data.getScheme()) && "invite".equals(data.getHost())) {
            pendingInviteUri = data;
            // Switch bottom nav to Shopping tab so ShoppingFragment picks it up
            binding.bottomNav.setSelectedItemId(R.id.shoppingFragment);
        }
    }

    /** Called by ShoppingFragment once on view creation to consume a pending invite URI. */
    public Uri consumePendingInviteUri() {
        Uri uri = pendingInviteUri;
        pendingInviteUri = null;
        return uri;
    }
}