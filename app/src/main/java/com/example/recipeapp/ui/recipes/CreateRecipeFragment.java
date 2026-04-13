package com.example.recipeapp.ui.recipes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.example.recipeapp.BuildConfig;
import com.example.recipeapp.databinding.FragmentCreateRecipeBinding;
import com.example.recipeapp.models.Recipe;
import java.util.*;
import java.util.HashMap;

public class CreateRecipeFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 102;

    private FragmentCreateRecipeBinding binding;
    private Uri selectedImageUri = null;
    private String uploadedImageUrl = null;

    private final List<String> ingredientList = new ArrayList<>();
    private final List<String> stepList = new ArrayList<>();
    private final Set<String> selectedEquipment = new LinkedHashSet<>();
    private final Set<String> firestoreEquipmentKeys = new HashSet<>(); // for new-item detection

    private static final String[] MEAL_TYPES = {
            "Breakfast", "Lunch", "Dinner", "Dessert", "Snack"
    };

    private String selectedMealType = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateRecipeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Resize layout above keyboard so inline suggestions stay visible
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setupMealTypeChips();
        setupEquipmentInput();
        setupIngredientInput();
        setupStepInput();
        setupImagePicker();

        binding.btnPublish.setOnClickListener(v -> publishRecipe());
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    private void setupImagePicker() {
        binding.frameImagePicker.setOnClickListener(v -> pickImage());
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
            selectedImageUri = data.getData();
            binding.ivRecipeImage.setVisibility(View.VISIBLE);
            binding.llImagePlaceholder.setVisibility(View.GONE);
            binding.tvChangePhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImageUri)
                    .centerCrop()
                    .into(binding.ivRecipeImage);
        }
    }

    // ── Meal type chips ───────────────────────────────────────────────────────

    private void setupMealTypeChips() {
        for (String type : MEAL_TYPES) {
            Chip chip = new Chip(requireContext());
            chip.setText(type);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    // Uncheck others
                    for (int i = 0; i < binding.chipGroupMealType.getChildCount(); i++) {
                        Chip c = (Chip) binding.chipGroupMealType.getChildAt(i);
                        if (c != chip) c.setChecked(false);
                    }
                    selectedMealType = type.toLowerCase();
                } else {
                    if (type.toLowerCase().equals(selectedMealType)) {
                        selectedMealType = null;
                    }
                }
            });
            binding.chipGroupMealType.addView(chip);
        }
    }

    // ── Equipment input ───────────────────────────────────────────────────────

    private final List<String> equipmentSuggestions = new ArrayList<>();

    private void setupEquipmentInput() {
        // Load Firestore equipment for suggestions and new-item tracking
        FirebaseFirestore.getInstance().collection("equipment")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            equipmentSuggestions.add(name);
                            firestoreEquipmentKeys.add(name.toLowerCase());
                        }
                    }
                });

        // Live suggestions as user types
        binding.etEquipmentInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showEquipmentSuggestions(s.toString().trim());
            }
        });

        // Add button — free text allowed
        binding.btnAddEquipment.setOnClickListener(v -> tryAddEquipmentFromInput());

        // Keyboard Done — free text allowed
        binding.etEquipmentInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                tryAddEquipmentFromInput();
                return true;
            }
            return false;
        });
    }

    private void showEquipmentSuggestions(String query) {
        binding.chipGroupEquipmentSuggestions.removeAllViews();

        if (query.isEmpty()) {
            hideEquipmentSuggestions();
            return;
        }

        List<String> matches = new ArrayList<>();
        for (String name : equipmentSuggestions) {
            if (name.toLowerCase().contains(query.toLowerCase())
                    && !selectedEquipment.contains(name.toLowerCase())) {
                matches.add(name);
                if (matches.size() == 10) break;
            }
        }

        if (matches.isEmpty()) {
            hideEquipmentSuggestions();
            return;
        }

        for (String name : matches) {
            Chip chip = new Chip(requireContext());
            chip.setText(name);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                addEquipmentTag(name);
                binding.etEquipmentInput.setText("");
                hideEquipmentSuggestions();
            });
            binding.chipGroupEquipmentSuggestions.addView(chip);
        }

        binding.chipGroupEquipmentSuggestions.setVisibility(View.VISIBLE);
        binding.tvEquipmentSuggestionsLabel.setVisibility(View.VISIBLE);
    }

    private void hideEquipmentSuggestions() {
        binding.chipGroupEquipmentSuggestions.setVisibility(View.GONE);
        binding.tvEquipmentSuggestionsLabel.setVisibility(View.GONE);
    }

    private void tryAddEquipmentFromInput() {
        String text = binding.etEquipmentInput.getText().toString().trim();
        if (text.isEmpty()) return;
        addEquipmentTag(text);
        binding.etEquipmentInput.setText("");
        hideEquipmentSuggestions();
    }

    private void addEquipmentTag(String name) {
        String key = name.toLowerCase();
        if (selectedEquipment.contains(key)) return;
        selectedEquipment.add(key);

        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> {
            selectedEquipment.remove(key);
            binding.chipGroupEquipment.removeView(chip);
        });
        binding.chipGroupEquipment.addView(chip);
    }

    /** Save any equipment item that doesn't yet exist in Firestore. */
    private void saveNewEquipmentToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (String key : selectedEquipment) {
            if (!firestoreEquipmentKeys.contains(key)) {
                // Capitalize first letter for display
                String display = key.substring(0, 1).toUpperCase() + key.substring(1);
                Map<String, Object> doc = new HashMap<>();
                doc.put("name", display);
                db.collection("equipment").add(doc);
            }
        }
    }

    // ── Ingredient input ──────────────────────────────────────────────────────

    private void setupIngredientInput() {
        binding.btnAddIngredient.setOnClickListener(v -> addIngredient());
        binding.etIngredientInput.setOnEditorActionListener((v, actionId, event) -> {
            addIngredient();
            return true;
        });
    }

    private void addIngredient() {
        String text = binding.etIngredientInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        ingredientList.add(text);
        binding.etIngredientInput.setText("");

        // Add chip tag
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        final String ingredient = text;
        chip.setOnCloseIconClickListener(v -> {
            ingredientList.remove(ingredient);
            binding.chipGroupIngredients.removeView(chip);
        });
        binding.chipGroupIngredients.addView(chip);
    }

    // ── Step input ────────────────────────────────────────────────────────────

    private void setupStepInput() {
        binding.btnAddStep.setOnClickListener(v -> addStep());
        binding.etStepInput.setOnEditorActionListener((v, actionId, event) -> {
            addStep();
            return true;
        });
    }

    private void addStep() {
        String text = binding.etStepInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        stepList.add(text);
        binding.etStepInput.setText("");

        // Add numbered step card
        int stepNumber = stepList.size();
        addStepView(stepNumber, text);
    }

    private void addStepView(int number, String text) {
        View stepView = getLayoutInflater().inflate(
                android.R.layout.simple_list_item_2, binding.llStepsList, false);

        TextView tv1 = stepView.findViewById(android.R.id.text1);
        TextView tv2 = stepView.findViewById(android.R.id.text2);
        tv1.setText("Step " + number);
        tv1.setTextSize(13f);
        tv1.setTypeface(null, android.graphics.Typeface.BOLD);
        tv2.setText(text);
        tv2.setTextSize(14f);

        binding.llStepsList.addView(stepView);
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    private void publishRecipe() {
        // Validate
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String prepStr = binding.etPrepTime.getText().toString().trim();
        String cookStr = binding.etCookTime.getText().toString().trim();
        String servStr = binding.etServings.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            binding.etTitle.setError("Title is required");
            binding.etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            binding.etDescription.setError("Description is required");
            binding.etDescription.requestFocus();
            return;
        }
        if (selectedMealType == null) {
            Toast.makeText(requireContext(),
                    "Please select a meal type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ingredientList.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stepList.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please add at least one step", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnPublish.setEnabled(false);
        binding.btnPublish.setText("Publishing...");

        int prep = parseIntSafe(prepStr);
        int cook = parseIntSafe(cookStr);
        int servings = parseIntSafe(servStr, 4);

        if (selectedImageUri != null) {
            // Upload image first then save recipe
            uploadImageThenSave(title, description, prep, cook, servings);
        } else {
            // Save recipe without image
            saveRecipe(title, description, prep, cook, servings, null);
        }
    }

    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/image/upload";

    private void uploadImageThenSave(String title, String description,
                                     int prep, int cook, int servings) {
        new Thread(() -> {
            try {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Read image bytes
                byte[] imageBytes;
                try (java.io.InputStream inputStream =
                             requireContext().getContentResolver()
                                     .openInputStream(selectedImageUri)) {
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

                // Upload preset
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"")
                        .append("\r\n\r\n");
                writer.append(BuildConfig.CLOUDINARY_UPLOAD_PRESET).append("\r\n");
                writer.flush();

                // Public ID
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"public_id\"")
                        .append("\r\n\r\n");
                writer.append("recipes/" + uid + "_" + System.currentTimeMillis())
                        .append("\r\n");
                writer.flush();

                // Image file
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; " +
                        "name=\"file\"; filename=\"recipe.jpg\"").append("\r\n");
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

                android.util.Log.d("Cloudinary", "Recipe upload response: " + response);

                // Parse response
                org.json.JSONObject json = new org.json.JSONObject(response);

                // Handle Cloudinary error
                if (json.has("error")) {
                    String error = json.getJSONObject("error")
                            .optString("message", "Unknown error");
                    android.util.Log.e("Cloudinary", "Error: " + error);
                    if (!isAdded() || getActivity() == null) return;
                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded() || getActivity() == null) return;
                        // Save recipe without image if upload fails
                        Toast.makeText(requireContext(),
                                "Image upload failed, saving without image",
                                Toast.LENGTH_SHORT).show();
                        saveRecipe(title, description, prep, cook, servings, null);
                    });
                    return;
                }

                // No secure_url in response
                if (!json.has("secure_url")) {
                    android.util.Log.e("Cloudinary",
                            "No secure_url in response: " + response);
                    if (!isAdded() || getActivity() == null) return;
                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded() || getActivity() == null) return;
                        saveRecipe(title, description, prep, cook, servings, null);
                    });
                    return;
                }

                // Success — get image URL
                String imageUrl = json.getString("secure_url");
                android.util.Log.d("Cloudinary", "Recipe image URL: " + imageUrl);

                // Check fragment still attached before going to main thread
                if (!isAdded() || getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    // Double check again on main thread
                    if (!isAdded() || getActivity() == null) return;
                    saveRecipe(title, description, prep, cook, servings, imageUrl);
                });

            } catch (Exception e) {
                android.util.Log.e("Cloudinary",
                        "Recipe image upload failed: " + e.getMessage());
                // Fragment might be detached — check before UI operations
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getActivity() == null) return;
                    Toast.makeText(requireContext(),
                            "Image upload failed, saving without image",
                            Toast.LENGTH_SHORT).show();
                    saveRecipe(title, description, prep, cook, servings, null);
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

    private void saveRecipe(String title, String description,
                            int prep, int cook, int servings,
                            String imageUrl) {
        // Persist any brand-new equipment to the shared Firestore list
        saveNewEquipmentToFirestore();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String authorName = doc.getString("displayName");
                    if (authorName == null) authorName = "Unknown";

                    Recipe recipe = new Recipe(
                            title, description, selectedMealType,
                            new ArrayList<>(selectedEquipment),
                            new ArrayList<>(ingredientList),
                            new ArrayList<>(stepList),
                            prep, cook, servings);

                    recipe.setAuthorId(uid);
                    recipe.setAuthorName(authorName);
                    if (imageUrl != null) recipe.setImageUrl(imageUrl);

                    FirebaseFirestore.getInstance()
                            .collection("recipes")
                            .add(recipe)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(requireContext(),
                                        "Recipe published! 🎉",
                                        Toast.LENGTH_SHORT).show();
                                requireActivity().onBackPressed();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnPublish.setEnabled(true);
                                binding.btnPublish.setText("Publish Recipe");
                                Toast.makeText(requireContext(),
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }

    private int parseIntSafe(String s) { return parseIntSafe(s, 0); }
    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore default soft input mode for other fragments
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        binding = null;
    }
}


