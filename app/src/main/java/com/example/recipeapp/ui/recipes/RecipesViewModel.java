package com.example.recipeapp.ui.recipes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.recipeapp.data.MealDbService;
import com.example.recipeapp.models.Recipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipesViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MealDbService mealDbService = new MealDbService();

    private final MutableLiveData<List<Recipe>> allRecipes      = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Recipe>> filteredRecipes = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Recipe>> getFilteredRecipes() { return filteredRecipes; }
    public LiveData<List<Recipe>> getAllRecipes()       { return allRecipes; }

    public interface OnRecipesLoaded {
        void onLoaded();
    }

    /**
     * Loads recipes from two sources in parallel:
     *  1. Firestore  — user-created recipes
     *  2. TheMealDB  — community recipes (lightweight list items)
     * Both callbacks run on the main thread; the combined list is published
     * once both have completed (success or failure).
     */
    public void loadRecipes(OnRecipesLoaded callback) {
        List<Recipe> combined = new ArrayList<>();      // touched only on main thread
        AtomicInteger pending = new AtomicInteger(2);   // 2 sources

        Runnable publish = () -> {
            android.util.Log.d("RecipesViewModel",
                    "Loaded " + combined.size() + " recipes total");
            allRecipes.setValue(new ArrayList<>(combined));
            filteredRecipes.setValue(new ArrayList<>(combined));
            if (callback != null) callback.onLoaded();
        };

        // ── Source 1: Firestore (user-created) ───────────────────────────────
        db.collection("recipes").get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r == null) continue;
                        r.setId(doc.getId());
                        combined.add(r);
                    }
                    if (pending.decrementAndGet() == 0) publish.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("RecipesViewModel", "Firestore failed: " + e.getMessage());
                    if (pending.decrementAndGet() == 0) publish.run();
                });

        // ── Source 2: TheMealDB ───────────────────────────────────────────────
        mealDbService.loadAllRecipes(mealDbRecipes -> {
            combined.addAll(mealDbRecipes);
            if (pending.decrementAndGet() == 0) publish.run();
        });
    }

    public void filterRecipes(String mealType, Set<String> equipment, Set<String> ingredients) {
        List<Recipe> source = allRecipes.getValue();
        if (source == null) return;

        List<Recipe> result = new ArrayList<>();
        for (Recipe r : source) {

            // ── Meal type ─────────────────────────────────────────────────────
            if (mealType != null && !mealType.isEmpty()) {
                if (r.getMealType() == null) continue;
                if (!r.getMealType().equalsIgnoreCase(mealType.trim())) continue;
            }

            // ── Equipment ─────────────────────────────────────────────────────
            // MealDB recipes have no equipment data → always pass this filter.
            if (equipment != null && !equipment.isEmpty()) {
                List<String> recipeEquip = r.getEquipment();
                if (recipeEquip != null && !recipeEquip.isEmpty()) {
                    boolean userHasAny = false;
                    outer:
                    for (String re : recipeEquip) {
                        for (String ue : equipment) {
                            if (re.equalsIgnoreCase(ue.trim())) {
                                userHasAny = true;
                                break outer;
                            }
                        }
                    }
                    if (!userHasAny) continue;
                }
            }

            // ── Ingredients ───────────────────────────────────────────────────
            // MealDB list-view items have no ingredient data; exclude them when
            // the user is filtering by ingredient so results are accurate.
            if (ingredients != null && !ingredients.isEmpty()) {
                List<String> recipeIngredients = r.getIngredients();
                if (recipeIngredients == null || recipeIngredients.isEmpty()) continue;
                boolean hasAny = false;
                outer:
                for (String ing : ingredients) {
                    for (String ri : recipeIngredients) {
                        if (ri.toLowerCase().contains(ing.toLowerCase())) {
                            hasAny = true;
                            break outer;
                        }
                    }
                }
                if (!hasAny) continue;
            }

            result.add(r);
        }

        android.util.Log.d("RecipesViewModel",
                "Filter mealType=" + mealType + " equipment=" + equipment +
                " ingredients=" + ingredients + " → " + result.size() + " results");

        filteredRecipes.setValue(result);
    }
}
