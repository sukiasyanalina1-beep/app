package com.example.recipeapp.adapters;

import android.graphics.Paint;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.example.recipeapp.databinding.ItemShoppingBinding;
import com.example.recipeapp.models.ShoppingItem;
import java.util.Objects;

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
                    boolean sameName    = Objects.equals(a.getName(), b.getName());
                    boolean sameChecked = a.isChecked() == b.isChecked();
                    String ac = a.getCheckedByName(); String bc = b.getCheckedByName();
                    boolean sameChecker = (ac == null ? bc == null : ac.equals(bc));
                    return sameName && sameChecked && sameChecker;
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

            if (item.isChecked()) {
                String checker = item.getCheckedByName();
                binding.tvAddedBy.setText((checker != null && !checker.isEmpty())
                        ? "Done by " + checker : "Done");
                binding.tvAddedBy.setVisibility(View.VISIBLE);
            } else {
                String addedBy = item.getAddedByName();
                if (addedBy != null && !addedBy.isEmpty()) {
                    binding.tvAddedBy.setText("Added by " + addedBy);
                    binding.tvAddedBy.setVisibility(View.VISIBLE);
                } else {
                    binding.tvAddedBy.setVisibility(View.GONE);
                }
            }

            if (item.isChecked()) {
                // Done tab: hide checkbox, item is view-only
                binding.cbChecked.setVisibility(View.GONE);
                binding.tvItemName.setPaintFlags(
                        binding.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvItemName.setAlpha(0.4f);
                binding.tvAddedBy.setAlpha(0.4f);
            } else {
                binding.cbChecked.setVisibility(View.VISIBLE);
                binding.cbChecked.setChecked(false);
                binding.tvItemName.setPaintFlags(
                        binding.tvItemName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvItemName.setAlpha(1f);
                binding.tvAddedBy.setAlpha(1f);
                binding.cbChecked.setOnClickListener(v -> onToggle.onToggle(item));
            }

            binding.btnDelete.setOnClickListener(v -> onDelete.onDelete(item));
        }
    }
}
