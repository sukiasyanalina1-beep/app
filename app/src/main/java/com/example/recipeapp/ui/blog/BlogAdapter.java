package com.example.recipeapp.ui.blog;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.example.recipeapp.databinding.ItemBlogPostBinding;
import com.example.recipeapp.models.BlogPost;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                    return a.getTitle().equals(b.getTitle())
                            && a.getLikedBy().size() == b.getLikedBy().size();
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

            // Meta: "By Author · Apr 10 · 3 min read"
            String date = post.getCreatedAt() > 0
                    ? new SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(new Date(post.getCreatedAt()))
                    : "";
            int readMin = readingTime(post.getContent());
            String meta = "By " + post.getAuthorName();
            if (!date.isEmpty()) meta += " · " + date;
            meta += " · " + readMin + " min read";
            binding.tvMeta.setText(meta);

            // Cover image
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.ivCover.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot())
                        .load(post.getImageUrl())
                        .centerCrop()
                        .into(binding.ivCover);
            } else {
                binding.ivCover.setVisibility(View.GONE);
            }

            // Like count
            List<String> likedBy = post.getLikedBy();
            int likeCount = likedBy.size();
            if (likeCount > 0) {
                binding.tvLikeCount.setText(String.valueOf(likeCount));
                binding.tvLikeCount.setVisibility(View.VISIBLE);
                binding.ivHeart.setVisibility(View.VISIBLE);
            } else {
                binding.tvLikeCount.setVisibility(View.GONE);
                binding.ivHeart.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(post));
        }

        private int readingTime(String content) {
            if (content == null || content.isEmpty()) return 1;
            int words = content.trim().split("\\s+").length;
            return Math.max(1, words / 200);
        }
    }
}
