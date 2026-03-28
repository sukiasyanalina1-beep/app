package com.example.recipeapp.ui.auth;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    public static void createProfile(String uid, String displayName, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a unique family group ID for this user
        String familyId = db.collection("families").document().getId();

        // Create the family document
        Map<String, Object> family = new HashMap<>();
        family.put("ownerId", uid);
        family.put("members", new ArrayList<String>() {{ add(uid); }});
        family.put("createdAt", System.currentTimeMillis());
        db.collection("families").document(familyId).set(family);

        // Create user profile with familyId
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", displayName);
        user.put("email", email);
        user.put("familyId", familyId);
        user.put("favouriteRecipeIds", new ArrayList<>());
        user.put("createdAt", System.currentTimeMillis());
        db.collection("users").document(uid).set(user);
    }
}