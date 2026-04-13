package com.example.recipeapp.ui.recipes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.recipeapp.models.Recipe;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.recipeapp.R;
import com.example.recipeapp.adapters.RecipeAdapter;
import com.example.recipeapp.databinding.FragmentRecipesBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RecipesFragment extends Fragment {

    private FragmentRecipesBinding binding;
    private RecipesViewModel viewModel;
    private RecipeAdapter adapter;

    private String selectedMealType = null;
    private final Set<String> userEquipment = new HashSet<>();
    private final Set<String> selectedIngredients = new LinkedHashSet<>();
    private List<String> availableIngredients = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecipesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RecipesViewModel.class);

        setupRecyclerView();
        setupMealTypeChips();
        setupIngredientSearch();
        restoreIngredientChips();
        loadUserEquipmentThenRecipes();
    }

    private void setupRecyclerView() {
        adapter = new RecipeAdapter(recipe -> {
            Bundle args = new Bundle();
            args.putString("recipeId", recipe.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_recipes_to_detail, args);
        });
        binding.rvRecipes.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvRecipes.setAdapter(adapter);
    }

    private void setupMealTypeChips() {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Dessert", "Snack"};
        for (String type : mealTypes) {
            Chip chip = new Chip(requireContext());
            chip.setText(type);
            chip.setCheckable(true);
            // Restore checked state before attaching listener to avoid spurious callbacks
            chip.setChecked(type.toLowerCase().equals(selectedMealType));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck all siblings
                    for (int i = 0; i < binding.chipGroupMealType.getChildCount(); i++) {
                        Chip c = (Chip) binding.chipGroupMealType.getChildAt(i);
                        if (c != chip) c.setChecked(false);
                    }
                    selectedMealType = type.toLowerCase();
                } else {
                    if (type.toLowerCase().equals(selectedMealType)) {
                        selectedMealType = null;
                    }
                }
                applyFilters();
            });
            binding.chipGroupMealType.addView(chip);
        }
    }

    private void setupIngredientSearch() {
        viewModel.getFilteredRecipes().observe(getViewLifecycleOwner(), recipes -> {
            // collect unique ingredients for autocomplete
        });

        loadIngredientSuggestions();

        binding.btnAddIngredient.setOnClickListener(v -> {
            String text = binding.etIngredients.getText().toString().trim();
            if (!text.isEmpty()) {
                if (isValidIngredient(text)) {
                    addIngredientTag(text);
                    binding.etIngredients.setText("");
                } else {
                    binding.etIngredients.setError("Please select an ingredient from the list");
                }
            }
        });

        binding.etIngredients.setOnEditorActionListener((v, actionId, event) -> {
            String text = binding.etIngredients.getText().toString().trim();
            if (!text.isEmpty()) {
                if (isValidIngredient(text)) {
                    addIngredientTag(text);
                    binding.etIngredients.setText("");
                } else {
                    binding.etIngredients.setError("Please select an ingredient from the list");
                }
            }
            return true;
        });

        binding.etIngredients.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            addIngredientTag(selected);
            binding.etIngredients.setText("");
        });
    }

    private void loadIngredientSuggestions() {
        FirebaseFirestore.getInstance().collection("recipes").get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> unique = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null && r.getIngredients() != null) unique.addAll(r.getIngredients());
                    }
                    availableIngredients = new ArrayList<>(unique);
                    ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            availableIngredients);
                    binding.etIngredients.setAdapter(autoAdapter);
                    binding.etIngredients.setThreshold(1);
                });
    }

    private boolean isValidIngredient(String text) {
        for (String ing : availableIngredients) {
            if (ing.equalsIgnoreCase(text)) return true;
        }
        return false;
    }

    private void addIngredientTag(String ingredient) {
        if (selectedIngredients.contains(ingredient.toLowerCase())) return;
        selectedIngredients.add(ingredient.toLowerCase());
        addIngredientChipView(ingredient);
        applyFilters();
    }

    /** Adds only the chip view — used when restoring state after view recreation. */
    private void addIngredientChipView(String ingredient) {
        Chip chip = new Chip(requireContext());
        chip.setText(ingredient);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> {
            selectedIngredients.remove(ingredient.toLowerCase());
            binding.chipGroupIngredients.removeView(chip);
            applyFilters();
        });
        binding.chipGroupIngredients.addView(chip);
    }

    /** Re-creates ingredient chip views from the surviving selectedIngredients set. */
    private void restoreIngredientChips() {
        for (String ing : new ArrayList<>(selectedIngredients)) {
            addIngredientChipView(ing);
        }
    }

    // ── The key method — loads in the correct order ──────────────────────────

    private void loadUserEquipmentThenRecipes() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // STEP 1: load user's saved equipment
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    userEquipment.clear();
                    List<String> saved = (List<String>) doc.get("equipment");
                    if (saved != null) {
                        for (String e : saved) {
                            userEquipment.add(e.toLowerCase().trim());
                        }
                    }
                    android.util.Log.d("RecipesFragment",
                            "User equipment loaded: " + userEquipment);

                    // STEP 2: now load recipes
                    // STEP 3: when recipes finish loading, apply filters
                    setupObserverAndLoadRecipes();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("RecipesFragment",
                            "Failed to load equipment: " + e.getMessage());
                    // Load recipes anyway even if equipment fails
                    setupObserverAndLoadRecipes();
                });
    }

    private void setupObserverAndLoadRecipes() {
        // Observe filtered results and update UI
        viewModel.getFilteredRecipes().observe(getViewLifecycleOwner(), recipes -> {
            adapter.submitList(recipes);
            binding.tvResultCount.setText(recipes.size() + " recipes found");
            binding.emptyState.setVisibility(
                    recipes.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Skip Firestore fetch if recipes are already in the ViewModel (returning from navigation)
        List<Recipe> existing = viewModel.getAllRecipes().getValue();
        if (existing != null && !existing.isEmpty()) {
            applyFilters();
        } else {
            viewModel.loadRecipes(() -> applyFilters());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void applyFilters() {
        android.util.Log.d("RecipesFragment",
                "applyFilters — mealType: " + selectedMealType +
                        " | equipment: " + userEquipment +
                        " | ingredients: " + selectedIngredients);

        viewModel.filterRecipes(selectedMealType, userEquipment, selectedIngredients);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}