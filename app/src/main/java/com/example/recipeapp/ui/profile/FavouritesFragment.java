package com.example.recipeapp.ui.profile;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.recipeapp.R;
import com.example.recipeapp.adapters.RecipeAdapter;
import com.example.recipeapp.databinding.FragmentFavouritesBinding;
import com.example.recipeapp.models.Recipe;
import java.util.*;

public class FavouritesFragment extends Fragment {

    private FragmentFavouritesBinding binding;
    private RecipeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavouritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new RecipeAdapter(recipe -> {
            Bundle args = new Bundle();
            args.putString("recipeId", recipe.getId());
            androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.action_recipes_to_detail, args);
        });

        binding.rvFavourites.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvFavourites.setAdapter(adapter);
        binding.btnBack.setOnClickListener(v ->
                requireActivity().onBackPressed());

        loadFavourites();
    }

    private void loadFavourites() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    List<String> favIds = (List<String>) doc.get("favouriteRecipeIds");
                    if (favIds == null || favIds.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    db.collection("recipes")
                            .whereIn(FieldPath.documentId(), favIds)
                            .get()
                            .addOnSuccessListener(snap -> {
                                binding.progressBar.setVisibility(View.GONE);
                                List<Recipe> list = new ArrayList<>();
                                for (QueryDocumentSnapshot d : snap) {
                                    Recipe r = d.toObject(Recipe.class);
                                    r.setId(d.getId());
                                    list.add(r);
                                }
                                adapter.submitList(list);
                                binding.tvEmpty.setVisibility(
                                        list.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}