package com.example.recipeapp.ui.blog;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.google.firebase.firestore.*;
import com.example.recipeapp.R;
import com.example.recipeapp.databinding.FragmentBlogBinding;
import com.example.recipeapp.models.BlogPost;
import java.util.*;

public class BlogFragment extends Fragment {

    private FragmentBlogBinding binding;
    private BlogAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBlogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        adapter = new BlogAdapter(post -> {
            Bundle args = new Bundle();
            args.putString("postId", post.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_blog_to_post, args);
        });

        binding.rvBlog.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBlog.setAdapter(adapter);

        loadPosts();
    }

    private void loadPosts() {
        db.collection("blog")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<BlogPost> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        BlogPost p = d.toObject(BlogPost.class);
                        p.setId(d.getId());
                        posts.add(p);
                    }
                    adapter.submitList(posts);
                    binding.tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}