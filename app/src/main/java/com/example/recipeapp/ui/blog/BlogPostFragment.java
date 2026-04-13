package com.example.recipeapp.ui.blog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.recipeapp.R;
import com.example.recipeapp.databinding.FragmentBlogPostBinding;
import com.example.recipeapp.models.BlogPost;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlogPostFragment extends Fragment {

    private FragmentBlogPostBinding binding;
    private FirebaseFirestore db;
    private String postId;
    private String currentUserId;
    private String currentUserName = "";
    private BlogPost currentPost;
    private ListenerRegistration postListener;
    private ListenerRegistration commentsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBlogPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadCurrentUserName();
        }

        Bundle args = getArguments();
        if (args == null) return;
        postId = args.getString("postId");
        if (postId == null) return;

        listenToPost();
        listenToComments();
        setupCommentSubmit();
    }

    private void loadCurrentUserName() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    currentUserName = doc.getString("displayName");
                    if (currentUserName == null) currentUserName = "User";
                });
    }

    private void listenToPost() {
        postListener = db.collection("blog").document(postId)
                .addSnapshotListener((doc, e) -> {
                    if (doc == null || !doc.exists() || binding == null) return;
                    currentPost = doc.toObject(BlogPost.class);
                    if (currentPost == null) return;
                    currentPost.setId(doc.getId());
                    populatePost(currentPost);
                });
    }

    private void populatePost(BlogPost post) {
        binding.tvTitle.setText(post.getTitle());

        // Meta line
        String date = post.getCreatedAt() > 0
                ? new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(new Date(post.getCreatedAt()))
                : "";
        int readMin = readingTime(post.getContent());
        String meta = "By " + post.getAuthorName();
        if (!date.isEmpty()) meta += "  ·  " + date;
        meta += "  ·  " + readMin + " min read";
        binding.tvMeta.setText(meta);

        // Content
        binding.tvContent.setText(post.getContent());

        // Cover image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            binding.ivCover.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl()).centerCrop().into(binding.ivCover);
        } else {
            binding.ivCover.setVisibility(View.GONE);
        }

        // Like state
        updateLikeUI(post);
    }

    private void updateLikeUI(BlogPost post) {
        List<String> likedBy = post.getLikedBy();
        int count = likedBy.size();
        boolean liked = currentUserId != null && likedBy.contains(currentUserId);

        binding.btnLike.setImageResource(
                liked ? R.drawable.ic_favourite_filled : R.drawable.ic_favourite_outline);
        binding.tvLikeCount.setText(count > 0 ? String.valueOf(count) : "");

        binding.btnLike.setOnClickListener(v -> toggleLike(post, liked));
    }

    private void toggleLike(BlogPost post, boolean currentlyLiked) {
        if (currentUserId == null) return;
        FieldValue update = currentlyLiked
                ? FieldValue.arrayRemove(currentUserId)
                : FieldValue.arrayUnion(currentUserId);
        db.collection("blog").document(post.getId()).update("likedBy", update);
        // UI updates automatically via the snapshot listener
    }

    private void listenToComments() {
        commentsListener = db.collection("blog").document(postId)
                .collection("comments")
                .orderBy("createdAt")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || binding == null) return;
                    binding.llComments.removeAllViews();
                    int count = snap.size();
                    binding.tvCommentCount.setText(count > 0 ? "(" + count + ")" : "");

                    for (QueryDocumentSnapshot doc : snap) {
                        String author = doc.getString("authorName");
                        String text = doc.getString("text");
                        addCommentView(author != null ? author : "User", text != null ? text : "");
                    }
                });
    }

    private void addCommentView(String author, String text) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 16);

        TextView tvAuthor = new TextView(requireContext());
        tvAuthor.setText(author);
        tvAuthor.setTextSize(13f);
        tvAuthor.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAuthor.setPadding(0, 0, 0, 4);

        TextView tvText = new TextView(requireContext());
        tvText.setText(text);
        tvText.setTextSize(14f);
        tvText.setLineSpacing(0, 1.4f);

        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0x1A000000);
        divider.setPadding(0, 16, 0, 0);

        row.addView(tvAuthor);
        row.addView(tvText);
        row.addView(divider);
        binding.llComments.addView(row);
    }

    private void setupCommentSubmit() {
        binding.btnPostComment.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(requireContext(),
                        "Sign in to leave a comment", Toast.LENGTH_SHORT).show();
                return;
            }
            String text = binding.etComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            Map<String, Object> comment = new HashMap<>();
            comment.put("text", text);
            comment.put("authorId", currentUserId);
            comment.put("authorName", currentUserName);
            comment.put("createdAt", System.currentTimeMillis());

            db.collection("blog").document(postId)
                    .collection("comments").add(comment)
                    .addOnSuccessListener(ref -> {
                        if (binding != null) binding.etComment.setText("");
                    });
        });
    }

    private int readingTime(String content) {
        if (content == null || content.isEmpty()) return 1;
        int words = content.trim().split("\\s+").length;
        return Math.max(1, words / 200);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
        if (commentsListener != null) commentsListener.remove();
        binding = null;
    }
}
