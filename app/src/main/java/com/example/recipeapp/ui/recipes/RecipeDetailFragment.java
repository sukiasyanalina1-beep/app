package com.example.recipeapp.ui.recipes;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.recipeapp.R;
import com.example.recipeapp.data.MealDbService;
import com.example.recipeapp.databinding.FragmentRecipeDetailBinding;
import com.example.recipeapp.models.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailFragment extends Fragment {

    private FragmentRecipeDetailBinding binding;
    private List<String> availableIngredients = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private Recipe currentRecipe;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Bundle args = getArguments();
        if (args == null) return;
        String recipeId = args.getString("recipeId");
        if (recipeId == null) return;
        loadRecipe(recipeId);
    }

    private void loadRecipe(String recipeId) {
        if (recipeId.startsWith("mealdb_")) {
            // TheMealDB recipe — fetch full details from the API
            String mealId = recipeId.substring(7); // strip "mealdb_" prefix
            new MealDbService().loadMealDetail(mealId, recipes -> {
                if (binding == null) return;
                if (!recipes.isEmpty()) {
                    currentRecipe = recipes.get(0);
                    populateUI();
                }
            });
        } else {
            // User-created recipe — load from Firestore
            db.collection("recipes").document(recipeId).get()
                    .addOnSuccessListener(doc -> {
                        currentRecipe = doc.toObject(Recipe.class);
                        if (currentRecipe == null) return;
                        currentRecipe.setId(doc.getId());
                        populateUI();
                    });
        }
    }

    private void populateUI() {
        boolean isMealDb = "mealdb".equals(currentRecipe.getAuthorId());

        binding.tvTitle.setText(currentRecipe.getTitle());

        String desc = currentRecipe.getDescription();
        if (desc != null && !desc.isEmpty()) {
            binding.tvDescription.setText(desc);
            binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            binding.tvDescription.setVisibility(View.GONE);
        }

        String author = currentRecipe.getAuthorName();
        binding.tvAuthor.setText("By " + (author != null ? author : "Unknown"));

        int prep = currentRecipe.getPrepTimeMinutes();
        int cook = currentRecipe.getCookTimeMinutes();
        binding.tvPrepTime.setText(prep > 0 ? prep + " min prep" : "– min prep");
        binding.tvCookTime.setText(cook > 0 ? cook + " min cook" : "– min cook");
        int servings = currentRecipe.getServings();
        binding.tvServings.setText((servings > 0 ? servings : 4) + " servings");

        String imageUrl = currentRecipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(binding.ivRecipeImage);
        }

        // Ingredients
        StringBuilder ingredients = new StringBuilder();
        if (currentRecipe.getIngredients() != null) {
            for (String ing : currentRecipe.getIngredients()) {
                ingredients.append("• ").append(ing).append("\n");
            }
        }
        binding.tvIngredients.setText(ingredients.toString().trim());

        // "Start Cooking" uses Firestore — hide it for TheMealDB recipes
        binding.btnStartCooking.setVisibility(isMealDb ? View.GONE : View.VISIBLE);
        if (!isMealDb) {
            binding.btnStartCooking.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("recipeId", currentRecipe.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_detail_to_cooking, args);
            });
        }

        checkFavouriteStatus();
        binding.fabFavourite.setOnClickListener(v -> toggleFavourite());
        binding.btnAddToShopping.setOnClickListener(v -> addIngredientsToShoppingList());
    }

    private void checkFavouriteStatus() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    java.util.List<String> favs = (java.util.List<String>) doc.get("favouriteRecipeIds");
                    boolean isFav = favs != null && favs.contains(currentRecipe.getId());
                    binding.fabFavourite.setImageResource(
                            isFav ? R.drawable.ic_favourite_filled : R.drawable.ic_favourite_outline);
                });
    }

    private void toggleFavourite() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    java.util.List<String> favs = (java.util.List<String>) doc.get("favouriteRecipeIds");
                    boolean isFav = favs != null && favs.contains(currentRecipe.getId());

                    if (isFav) {
                        db.collection("users").document(currentUserId)
                                .update("favouriteRecipeIds", FieldValue.arrayRemove(currentRecipe.getId()));
                        binding.fabFavourite.setImageResource(R.drawable.ic_favourite_outline);
                    } else {
                        db.collection("users").document(currentUserId)
                                .update("favouriteRecipeIds", FieldValue.arrayUnion(currentRecipe.getId()));
                        binding.fabFavourite.setImageResource(R.drawable.ic_favourite_filled);
                    }
                });
    }

    private void addIngredientsToShoppingList() {
        if (currentRecipe.getIngredients() == null) return;

        // Get user's display name first then add items
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.getString("displayName");
                    if (userName == null) userName = "";

                    // Use first group in groupIds (or fall back to old familyId)
                    List<String> groupIds = (List<String>) userDoc.get("groupIds");
                    String groupId = null;
                    if (groupIds != null && !groupIds.isEmpty()) {
                        groupId = groupIds.get(0);
                    } else {
                        groupId = userDoc.getString("familyId");
                    }

                    if (groupId == null) {
                        com.google.android.material.snackbar.Snackbar.make(
                                requireView(),
                                "No shopping group found. Go to Shopping to create one.",
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    final String finalGroupId = groupId;
                    final String finalUserName = userName;
                    for (String ingredient : currentRecipe.getIngredients()) {
                        com.example.recipeapp.models.ShoppingItem item =
                                new com.example.recipeapp.models.ShoppingItem(
                                        ingredient, currentUserId, finalUserName);
                        db.collection("groups")
                                .document(finalGroupId)
                                .collection("shoppingItems")
                                .add(item);
                    }

                    com.google.android.material.snackbar.Snackbar.make(
                            requireView(),
                            "Ingredients added to shopping list!",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}