package com.example.recipeapp.ui.blog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.recipeapp.databinding.FragmentBlogPostBinding;
import com.example.recipeapp.models.BlogPost;
import java.util.*;

public class BlogPostFragment extends Fragment {

    private FragmentBlogPostBinding binding;
    private FirebaseFirestore db;
    private String postId;

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
        postId = getArguments().getString("postId");
        loadPost();
        setupCommentSubmit();
    }

    private void loadPost() {
        db.collection("blog").document(postId).get()
                .addOnSuccessListener(doc -> {
                    BlogPost post = doc.toObject(BlogPost.class);
                    if (post == null) return;
                    binding.tvTitle.setText(post.getTitle());
                    binding.tvAuthor.setText("By " + post.getAuthorName());
                    binding.tvContent.setText(post.getContent());
                });
        loadComments();
    }

    private void loadComments() {
        db.collection("blog").document(postId)
                .collection("comments")
                .orderBy("createdAt")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    binding.llComments.removeAllViews();
                    for (QueryDocumentSnapshot doc : snap) {
                        String author = doc.getString("authorName");
                        String text   = doc.getString("text");
                        addCommentView(author, text);
                    }
                });
    }

    private void addCommentView(String author, String text) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 16);

        android.widget.TextView tvAuthor = new android.widget.TextView(requireContext());
        tvAuthor.setText(author);
        tvAuthor.setTextSize(13f);
        tvAuthor.setTypeface(null, android.graphics.Typeface.BOLD);

        android.widget.TextView tvText = new android.widget.TextView(requireContext());
        tvText.setText(text);
        tvText.setTextSize(14f);

        row.addView(tvAuthor);
        row.addView(tvText);

        android.view.View divider = new android.view.View(requireContext());
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0x1A000000);
        row.addView(divider);

        binding.llComments.addView(row);
    }

    private void setupCommentSubmit() {
        binding.btnPostComment.setOnClickListener(v -> {
            String text = binding.etComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        Map<String, Object> comment = new HashMap<>();
                        comment.put("text", text);
                        comment.put("authorId", uid);
                        comment.put("authorName", doc.getString("displayName"));
                        comment.put("createdAt", System.currentTimeMillis());

                        db.collection("blog").document(postId)
                                .collection("comments").add(comment)
                                .addOnSuccessListener(ref -> binding.etComment.setText(""));
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}