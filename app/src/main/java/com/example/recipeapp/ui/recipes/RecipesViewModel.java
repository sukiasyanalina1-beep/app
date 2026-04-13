package com.example.recipeapp.ui.recipes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.recipeapp.models.Recipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecipesViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Recipe>> allRecipes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Recipe>> filteredRecipes = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Recipe>> getFilteredRecipes() { return filteredRecipes; }
    public LiveData<List<Recipe>> getAllRecipes() { return allRecipes; }

    // Callback interface
    public interface OnRecipesLoaded {
        void onLoaded();
    }

    public void loadRecipes(OnRecipesLoaded callback) {
        db.collection("recipes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r == null) continue;
                        r.setId(doc.getId());
                        list.add(r);
                    }
                    android.util.Log.d("RecipesViewModel", "Loaded " + list.size() + " recipes");
                    allRecipes.setValue(list);
                    filteredRecipes.setValue(list);
                    // Notify fragment that recipes are ready
                    if (callback != null) callback.onLoaded();
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("RecipesViewModel", "Failed to load: " + e.getMessage()));
    }

    public void filterRecipes(String mealType, Set<String> equipment, Set<String> ingredients) {
        List<Recipe> source = allRecipes.getValue();
        if (source == null) return;

        List<Recipe> result = new ArrayList<>();
        for (Recipe r : source) {

            // Filter by meal type
            if (mealType != null && !mealType.isEmpty()) {
                if (r.getMealType() == null) continue;
                if (!r.getMealType().toLowerCase().trim()
                        .equals(mealType.toLowerCase().trim())) continue;
            }

            // Filter by equipment
            if (equipment != null && !equipment.isEmpty()) {
                if (r.getEquipment() == null || r.getEquipment().isEmpty()) {
                    // Recipe needs no equipment — always show it
                } else {
                    boolean userHasAny = false;
                    for (String recipeEquip : r.getEquipment()) {
                        for (String userEquip : equipment) {
                            if (recipeEquip.toLowerCase().trim()
                                    .equals(userEquip.toLowerCase().trim())) {
                                userHasAny = true;
                                break;
                            }
                        }
                        if (userHasAny) break;
                    }
                    if (!userHasAny) continue;
                }
            }

            // Filter by ingredients
            if (ingredients != null && !ingredients.isEmpty()) {
                if (r.getIngredients() == null) continue;
                boolean hasAny = false;
                for (String ing : ingredients) {
                    for (String ri : r.getIngredients()) {
                        if (ri.toLowerCase().contains(ing.toLowerCase())) {
                            hasAny = true;
                            break;
                        }
                    }
                    if (hasAny) break;
                }
                if (!hasAny) continue;
            }

            result.add(r);
        }

        android.util.Log.d("RecipesViewModel",
                "Filter — mealType: " + mealType +
                        " | equipment: " + equipment +
                        " | ingredients: " + ingredients +
                        " | results: " + result.size());

        filteredRecipes.setValue(result);
    }
}