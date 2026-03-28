package com.example.recipeapp.adapters;

import android.graphics.Paint;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.example.recipeapp.databinding.ItemShoppingBinding;
import com.example.recipeapp.models.ShoppingItem;

public class ShoppingItemAdapter extends ListAdapter<ShoppingItem, ShoppingItemAdapter.ViewHolder> {

    public interface OnItemToggle { void onToggle(ShoppingItem item); }
    public interface OnItemDelete { void onDelete(ShoppingItem item); }

    private final OnItemToggle onToggle;
    private final OnItemDelete onDelete;

    public ShoppingItemAdapter(OnItemToggle onToggle, OnItemDelete onDelete) {
        super(DIFF);
        this.onToggle = onToggle;
        this.onDelete = onDelete;
    }

    private static final DiffUtil.ItemCallback<ShoppingItem> DIFF =
            new DiffUtil.ItemCallback<ShoppingItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ShoppingItem a, @NonNull ShoppingItem b) {
                    return a.getId() != null && a.getId().equals(b.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull ShoppingItem a, @NonNull ShoppingItem b) {
                    return a.getName().equals(b.getName()) &&
                            a.isChecked() == b.isChecked();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemShoppingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemShoppingBinding binding;

        ViewHolder(ItemShoppingBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(ShoppingItem item) {
            binding.tvItemName.setText(item.getName());

            // Show who added it
            String addedBy = item.getAddedByName();
            if (addedBy != null && !addedBy.isEmpty()) {
                binding.tvAddedBy.setText("Added by " + addedBy);
                binding.tvAddedBy.setVisibility(View.VISIBLE);
            } else {
                binding.tvAddedBy.setVisibility(View.GONE);
            }

            binding.cbChecked.setChecked(item.isChecked());

            // Strike through when checked
            int flags = item.isChecked()
                    ? binding.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : binding.tvItemName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG;
            binding.tvItemName.setPaintFlags(flags);
            binding.tvItemName.setAlpha(item.isChecked() ? 0.4f : 1f);
            binding.tvAddedBy.setAlpha(item.isChecked() ? 0.4f : 1f);

            binding.cbChecked.setOnClickListener(v -> onToggle.onToggle(item));
            binding.btnDelete.setOnClickListener(v -> onDelete.onDelete(item));
        }
    }
}