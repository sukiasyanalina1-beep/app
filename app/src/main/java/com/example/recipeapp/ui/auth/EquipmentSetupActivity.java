package com.example.recipeapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.recipeapp.MainActivity;
import com.example.recipeapp.databinding.ActivityEquipmentSetupBinding;
import java.util.*;

public class EquipmentSetupActivity extends AppCompatActivity {

    private ActivityEquipmentSetupBinding binding;
    private final Set<String> selectedEquipment = new LinkedHashSet<>();
    private final List<String> availableEquipment = new ArrayList<>();
    /** Saved keys from Firestore — held separately so chips can be created after list loads. */
    private final List<String> savedEquipmentKeys = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEquipmentSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        isEditing = getIntent().getBooleanExtra("isEditing", false);
        if (isEditing) {
            binding.tvTitle.setText("Edit your equipment");
            binding.tvSubtitle.setText("Update the equipment available in your kitchen.");
            binding.btnSkip.setVisibility(View.GONE);
            binding.btnSaveEquipment.setText("Save changes");
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveEquipment.setEnabled(false);

        loadUserEquipmentThenLoadList();
        setupSearchInput();

        binding.btnSaveEquipment.setOnClickListener(v -> saveEquipment());
        binding.btnSkip.setOnClickListener(v -> goToMain());
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadUserEquipmentThenLoadList() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    List<String> saved = (List<String>) doc.get("equipment");
                    if (saved != null) {
                        for (String e : saved) savedEquipmentKeys.add(e.toLowerCase());
                    }
                    loadEquipmentList();
                })
                .addOnFailureListener(e -> loadEquipmentList());
    }

    private void loadEquipmentList() {
        db.collection("equipment")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveEquipment.setEnabled(true);

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) availableEquipment.add(name);
                    }

                    // Pre-populate chips for already-saved equipment.
                    // selectedEquipment is still empty here — addSelectedChip fills it.
                    for (String savedKey : savedEquipmentKeys) {
                        String display = findDisplayName(savedKey);
                        addSelectedChip(display != null ? display : savedKey);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveEquipment.setEnabled(true);
                    Toast.makeText(this,
                            "Failed to load equipment list", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Search input with inline suggestions ─────────────────────────────────

    private void setupSearchInput() {
        binding.etEquipment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showSuggestions(s.toString().trim());
            }
        });

        binding.etEquipment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String text = binding.etEquipment.getText().toString().trim();
                String match = findDisplayName(text.toLowerCase());
                if (match != null) {
                    addSelectedChip(match);
                    binding.etEquipment.setText("");
                } else if (!text.isEmpty()) {
                    binding.tilEquipment.setError("Not found — pick from suggestions");
                }
                return true;
            }
            return false;
        });

        binding.etEquipment.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideSuggestions();
        });
    }

    private void showSuggestions(String query) {
        binding.chipGroupSuggestions.removeAllViews();

        if (query.isEmpty()) {
            hideSuggestions();
            binding.tilEquipment.setError(null);
            return;
        }

        binding.tilEquipment.setError(null);
        List<String> matches = new ArrayList<>();
        for (String name : availableEquipment) {
            if (name.toLowerCase().contains(query.toLowerCase())
                    && !selectedEquipment.contains(name.toLowerCase())) {
                matches.add(name);
                if (matches.size() == 10) break; // cap at 10 suggestions
            }
        }

        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }

        for (String name : matches) {
            Chip chip = new Chip(this);
            chip.setText(name);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                addSelectedChip(name);
                binding.etEquipment.setText("");
                hideSuggestions();
            });
            binding.chipGroupSuggestions.addView(chip);
        }

        binding.chipGroupSuggestions.setVisibility(View.VISIBLE);
        binding.tvSuggestionsLabel.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        binding.chipGroupSuggestions.setVisibility(View.GONE);
        binding.tvSuggestionsLabel.setVisibility(View.GONE);
    }

    // ── Selected chips ────────────────────────────────────────────────────────

    private void addSelectedChip(String displayName) {
        String key = displayName.toLowerCase();
        if (selectedEquipment.contains(key)) return;
        selectedEquipment.add(key);

        Chip chip = new Chip(this);
        chip.setText(displayName);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> {
            selectedEquipment.remove(key);
            binding.chipGroupSelected.removeView(chip);
        });
        binding.chipGroupSelected.addView(chip);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String findDisplayName(String lowerKey) {
        for (String name : availableEquipment) {
            if (name.toLowerCase().equals(lowerKey)) return name;
        }
        return null;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveEquipment() {
        if (selectedEquipment.isEmpty()) {
            Toast.makeText(this,
                    "Please add at least one piece of equipment", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveEquipment.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(uid)
                .update("equipment", new ArrayList<>(selectedEquipment))
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            isEditing ? "Equipment updated!" : "Equipment saved!",
                            Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    binding.btnSaveEquipment.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMain() {
        if (isEditing) {
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}