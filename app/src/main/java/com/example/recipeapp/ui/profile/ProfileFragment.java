package com.example.recipeapp.ui.profile;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.example.recipeapp.BuildConfig;
import com.example.recipeapp.R;
import com.example.recipeapp.databinding.FragmentProfileBinding;
import com.example.recipeapp.ui.auth.EquipmentSetupActivity;
import com.example.recipeapp.ui.auth.LoginActivity;
import java.util.*;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 101;

    private FragmentProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefs = requireContext().getSharedPreferences(
                "recipeapp_prefs", Context.MODE_PRIVATE);

        loadUserProfile();
        setupContentRows();
        setupSettings();
        setupAccountRows();
        setupAvatar();
    }

    // ── Profile data ──────────────────────────────────────────────────────────

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    // Name and email
                    String name = doc.getString("displayName");
                    String email = doc.getString("email");
                    binding.tvDisplayName.setText(name != null ? name : "");
                    binding.tvEmail.setText(email != null ? email : "");

                    // Avatar
                    String photoUrl = doc.getString("photoUrl");
                    if (photoUrl != null && !photoUrl.isEmpty() && getContext() != null) {
                        Glide.with(this)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.bg_avatar_circle)
                                .into(binding.ivAvatar);
                    }

                    // Equipment count
                    List<String> equip = (List<String>) doc.get("equipment");
                    int equipCount = equip != null ? equip.size() : 0;
                    binding.tvEquipmentCount.setText(String.valueOf(equipCount));

                    // Favourite count
                    List<String> favs = (List<String>) doc.get("favouriteRecipeIds");
                    int favCount = favs != null ? favs.size() : 0;
                    binding.tvFavouriteCount.setText(String.valueOf(favCount));
                    binding.tvFavouriteCountRow.setText(String.valueOf(favCount));
                });

        // My recipe count
        db.collection("recipes")
                .whereEqualTo("authorId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    binding.tvRecipeCount.setText(String.valueOf(count));
                    binding.tvRecipeCountRow.setText(String.valueOf(count));
                });
    }

    // ── Content rows ──────────────────────────────────────────────────────────

    private void setupContentRows() {
        binding.rowFavourites.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profile_to_favourites));

        binding.rowMyRecipes.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profile_to_my_recipes));

        binding.rowCreateRecipe.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profile_to_create_recipe));
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private void setupAvatar() {
        binding.ivAvatar.setOnClickListener(v -> pickImage());
        binding.ivEditAvatar.setOnClickListener(v -> pickImage());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null) {
            uploadAvatar(data.getData());
        }
    }

    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/image/upload";

    private void uploadAvatar(Uri imageUri) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Read image bytes
                byte[] imageBytes;
                try (java.io.InputStream inputStream =
                             requireContext().getContentResolver().openInputStream(imageUri)) {
                    imageBytes = readBytes(inputStream);
                }

                // Build multipart request
                String boundary = "----FormBoundary" + System.currentTimeMillis();
                java.net.URL url = new java.net.URL(CLOUDINARY_UPLOAD_URL);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                java.io.OutputStream os = conn.getOutputStream();
                java.io.PrintWriter writer = new java.io.PrintWriter(
                        new java.io.OutputStreamWriter(os, "UTF-8"), true);

                // Add upload_preset field (unsigned upload)
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"")
                        .append("\r\n\r\n");
                writer.append(BuildConfig.CLOUDINARY_UPLOAD_PRESET).append("\r\n");
                writer.flush();

                // Use unique public_id per upload so Cloudinary returns a fresh URL
                // (avoids Glide serving the stale cached image on re-upload)
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"public_id\"")
                        .append("\r\n\r\n");
                writer.append("avatars/" + uid + "_" + System.currentTimeMillis()).append("\r\n");
                writer.flush();

                // Add image file
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; " +
                        "name=\"file\"; filename=\"avatar.jpg\"").append("\r\n");
                writer.append("Content-Type: image/jpeg").append("\r\n\r\n");
                writer.flush();
                os.write(imageBytes);
                os.flush();
                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
                os.close();

                // Read response
                int responseCode = conn.getResponseCode();
                java.io.InputStream responseStream = responseCode == 200
                        ? conn.getInputStream() : conn.getErrorStream();
                String response = new String(readBytes(responseStream));
                conn.disconnect();

                android.util.Log.d("Upload", "Response: " + response);

                // Parse URL from response
                org.json.JSONObject json = new org.json.JSONObject(response);

                if (json.has("error")) {
                    String error = json.getJSONObject("error").optString("message", "Unknown");
                    android.util.Log.e("Cloudinary", "Error: " + error);
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Cloudinary error: " + error, Toast.LENGTH_LONG).show());
                    }
                    return;
                }

                String imageUrl = json.getString("secure_url");

// Save to Firestore on main thread — check fragment is still attached
                if (!isAdded() || getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getActivity() == null) return;
                    db.collection("users").document(uid)
                            .update("photoUrl", imageUrl)
                            .addOnSuccessListener(v -> {
                                if (!isAdded() || getActivity() == null || binding == null) return;
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .circleCrop()
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                        .into(binding.ivAvatar);
                                Toast.makeText(requireContext(),
                                        "Avatar updated!", Toast.LENGTH_SHORT).show();
                            });
                });

            } catch (Exception e) {
                android.util.Log.e("Upload", "Failed: " + e.getMessage());
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getActivity() == null || binding == null) return;
                    Toast.makeText(requireContext(),
                            "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private byte[] readBytes(java.io.InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private void setupSettings() {
        // Edit display name
        binding.rowEditName.setOnClickListener(v -> showEditNameDialog());

        // Change password
        binding.rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Kitchen equipment
        binding.rowEditEquipment.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(),
                    EquipmentSetupActivity.class);
            intent.putExtra("isEditing", true);
            startActivity(intent);
        });

        // Dark mode
        boolean isDark = prefs.getBoolean("dark_mode", false);
        binding.switchDarkMode.setChecked(isDark);
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Notifications
        boolean notifs = prefs.getBoolean("notifications", true);
        binding.switchNotifications.setChecked(notifs);
        binding.switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(requireContext(),
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // ── Account rows ──────────────────────────────────────────────────────────

    private void setupAccountRows() {
        binding.rowRateApp.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Thank you for your support! ⭐",
                        Toast.LENGTH_SHORT).show());

        binding.rowPrivacy.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Privacy Policy")
                        .setMessage("RecipeApp collects only the data necessary to " +
                                "provide the service: your email, display name, recipes " +
                                "you create, and your shopping list.\n\n" +
                                "We do not sell your data to third parties.")
                        .setPositiveButton("OK", null)
                        .show());

        binding.rowDeleteAccount.setOnClickListener(v ->
                showDeleteAccountDialog());
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showEditNameDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(binding.tvDisplayName.getText());
        input.setSelection(input.getText().length());
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit display name")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || auth.getCurrentUser() == null) return;
                    String uid = auth.getCurrentUser().getUid();
                    db.collection("users").document(uid)
                            .update("displayName", newName)
                            .addOnSuccessListener(v -> {
                                binding.tvDisplayName.setText(newName);
                                Toast.makeText(requireContext(),
                                        "Name updated!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        if (auth.getCurrentUser() == null) return;
        String email = auth.getCurrentUser().getEmail();
        new AlertDialog.Builder(requireContext())
                .setTitle("Change password")
                .setMessage("We'll send a password reset link to:\n\n" + email)
                .setPositiveButton("Send link", (d, w) ->
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(requireContext(),
                                                "Reset link sent to " + email,
                                                Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(),
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (d, w) -> {
                    auth.signOut();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete account")
                .setMessage("This will permanently delete your account and all " +
                        "your recipes. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (auth.getCurrentUser() == null) return;
                    String uid = auth.getCurrentUser().getUid();
                    db.collection("users").document(uid).delete();
                    auth.getCurrentUser().delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(requireContext(),
                                        "Account deleted",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(requireContext(),
                                        LoginActivity.class));
                                requireActivity().finish();
                            })
                            .addOnFailureListener(e ->
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Re-authentication required")
                                            .setMessage("For security, please log out and " +
                                                    "log back in before deleting your account.")
                                            .setPositiveButton("OK", null)
                                            .show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}