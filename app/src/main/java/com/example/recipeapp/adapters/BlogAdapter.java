package com.example.recipeapp.adapters;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.example.recipeapp.databinding.ItemBlogPostBinding;
import com.example.recipeapp.models.BlogPost;

public class BlogAdapter extends ListAdapter<BlogPost, BlogAdapter.ViewHolder> {

    public interface OnPostClick { void onClick(BlogPost post); }
    private final OnPostClick listener;

    public BlogAdapter(OnPostClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<BlogPost> DIFF =
            new DiffUtil.ItemCallback<BlogPost>() {
                @Override public boolean areItemsTheSame(@NonNull BlogPost a, @NonNull BlogPost b) {
                    return a.getId().equals(b.getId());
                }
                @Override public boolean areContentsTheSame(@NonNull BlogPost a, @NonNull BlogPost b) {
                    return a.getTitle().equals(b.getTitle());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBlogPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBlogPostBinding binding;
        ViewHolder(ItemBlogPostBinding b) { super(b.getRoot()); this.binding = b; }

        void bind(BlogPost post) {
            binding.tvTitle.setText(post.getTitle());
            binding.tvExcerpt.setText(post.getExcerpt());
            binding.tvAuthor.setText("By " + post.getAuthorName());
            binding.getRoot().setOnClickListener(v -> listener.onClick(post));
        }
    }
}
