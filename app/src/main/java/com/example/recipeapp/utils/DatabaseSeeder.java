package com.example.recipeapp.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.recipeapp.models.Recipe;

import java.util.List;

public class DatabaseSeeder {

    private static final String PREF_SEEDED = "db_seeded_v1";

    public static void seedIfEmpty(android.content.Context context) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences("recipeapp_prefs", android.content.Context.MODE_PRIVATE);

        // Temporarily remove this check so it always tries to seed
        // if (prefs.getBoolean(PREF_SEEDED, false)) return;

        android.util.Log.d("DatabaseSeeder", "Checking if recipes exist...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recipes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    android.util.Log.d("DatabaseSeeder", "Recipe count: " + snapshot.size());
                    if (snapshot.isEmpty()) {
                        android.util.Log.d("DatabaseSeeder", "Seeding database...");
                        List<Recipe> recipes = MealDatabase.getSeedRecipes();
                        final int[] count = {0};
                        for (Recipe recipe : recipes) {
                            db.collection("recipes")
                                    .add(recipe)
                                    .addOnSuccessListener(ref -> {
                                        count[0]++;
                                        android.util.Log.d("DatabaseSeeder", "Added recipe " + count[0]);
                                        // After all recipes are added fetch images
                                        if (count[0] == recipes.size()) {
                                            android.util.Log.d("DatabaseSeeder", "All recipes added, fetching images...");
                                            ImageFetcher.fetchAndUpdateImages();
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            android.util.Log.e("DatabaseSeeder", "Failed: " + e.getMessage()));
                        }
                        prefs.edit().putBoolean(PREF_SEEDED, true).apply();
                    }else {
                        android.util.Log.d("DatabaseSeeder", "Already has data, skipping seed.");
                        prefs.edit().putBoolean(PREF_SEEDED, true).apply();
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("DatabaseSeeder", "Error reading recipes: " + e.getMessage()));
    }
    public static void seedEquipmentIfEmpty(android.content.Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("equipment").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        String[] items = {
                                "Oven", "Stovetop", "Blender", "Air fryer",
                                "Microwave", "Grill", "Slow cooker", "Instant pot",
                                "Food processor", "Stand mixer", "Rice cooker",
                                "Toaster", "Steamer", "Wok", "No equipment"
                        };
                        for (String item : items) {
                            java.util.Map<String, Object> doc = new java.util.HashMap<>();
                            doc.put("name", item);
                            db.collection("equipment").add(doc);
                        }
                    }
                });
    }
}