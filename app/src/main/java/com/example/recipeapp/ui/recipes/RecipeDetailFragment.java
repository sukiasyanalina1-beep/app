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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String recipeId = getArguments().getString("recipeId");
        loadRecipe(recipeId);
    }

    private void loadRecipe(String recipeId) {
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(doc -> {
                    currentRecipe = doc.toObject(Recipe.class);
                    if (currentRecipe == null) return;
                    currentRecipe.setId(doc.getId());
                    populateUI();
                });
    }

    private void populateUI() {
        binding.tvTitle.setText(currentRecipe.getTitle());
        binding.tvDescription.setText(currentRecipe.getDescription());
        binding.tvAuthor.setText("By " + currentRecipe.getAuthorName());
        binding.tvPrepTime.setText(currentRecipe.getPrepTimeMinutes() + " min prep");
        binding.tvCookTime.setText(currentRecipe.getCookTimeMinutes() + " min cook");
        binding.tvServings.setText(currentRecipe.getServings() + " servings");

        if (currentRecipe.getImageUrl() != null) {
            Glide.with(this).load(currentRecipe.getImageUrl())
                    .centerCrop().into(binding.ivRecipeImage);
        }

        // Ingredients
        StringBuilder ingredients = new StringBuilder();
        if (currentRecipe.getIngredients() != null) {
            for (String ing : currentRecipe.getIngredients()) {
                ingredients.append("• ").append(ing).append("\n");
            }
        }
        binding.tvIngredients.setText(ingredients.toString().trim());

        // Check if already favourited
        checkFavouriteStatus();

        binding.fabFavourite.setOnClickListener(v -> toggleFavourite());

        binding.btnStartCooking.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("recipeId", currentRecipe.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_detail_to_cooking, args);
        });

        // Add to shopping list button
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
                    String familyId = userDoc.getString("familyId");

                    if (familyId == null) {
                        com.google.android.material.snackbar.Snackbar.make(
                                requireView(),
                                "No family list found. Please set up your shopping list first.",
                                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    final String finalUserName = userName;
                    for (String ingredient : currentRecipe.getIngredients()) {
                        com.example.recipeapp.models.ShoppingItem item =
                                new com.example.recipeapp.models.ShoppingItem(
                                        ingredient, currentUserId, finalUserName);
                        db.collection("families")
                                .document(familyId)
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