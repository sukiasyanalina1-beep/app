package com.example.recipeapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.recipeapp.models.Recipe;
import com.example.recipeapp.databinding.ItemRecipeCardBinding;

public class RecipeAdapter extends ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Recipe>() {
                @Override
                public boolean areItemsTheSame(@NonNull Recipe a, @NonNull Recipe b) {
                    return a.getId().equals(b.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Recipe a, @NonNull Recipe b) {
                    return a.getTitle().equals(b.getTitle());
                }
            };

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipeCardBinding binding = ItemRecipeCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RecipeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecipeCardBinding binding;

        RecipeViewHolder(ItemRecipeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Recipe recipe) {
            binding.tvRecipeTitle.setText(recipe.getTitle());
            binding.tvMealType.setText(capitalize(recipe.getMealType()));
            binding.tvTotalTime.setText(recipe.getTotalTime() + " min");

            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(binding.ivRecipeThumb.getContext())
                        .load(recipe.getImageUrl())
                        .centerCrop()
                        .placeholder(com.example.recipeapp.R.drawable.placeholder_recipe)
                        .into(binding.ivRecipeThumb);
            }

            binding.getRoot().setOnClickListener(v -> listener.onRecipeClick(recipe));
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return "";
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}