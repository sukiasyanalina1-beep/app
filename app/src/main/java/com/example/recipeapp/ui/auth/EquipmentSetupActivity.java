package com.example.recipeapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    private final Set<String> selectedEquipment = new HashSet<>();
    private final List<String> availableEquipment = new ArrayList<>();
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

        // Check if coming from profile (editing) or from registration
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

        binding.btnSaveEquipment.setOnClickListener(v -> saveEquipment());
        binding.btnSkip.setOnClickListener(v -> goToMain());
    }

    private void loadUserEquipmentThenLoadList() {
        // First load user's saved equipment
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    List<String> saved = (List<String>) doc.get("equipment");
                    if (saved != null) {
                        for (String e : saved) {
                            selectedEquipment.add(e.toLowerCase());
                        }
                    }
                    // Then load the full equipment list from Firestore
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
                        if (name != null) {
                            availableEquipment.add(name);
                            addChip(name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load equipment list", Toast.LENGTH_SHORT).show();
                });
    }

    private void addChip(String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCheckable(true);

        // Pre-select if user already has this equipment
        boolean isSelected = selectedEquipment.contains(name.toLowerCase());
        chip.setChecked(isSelected);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedEquipment.add(name.toLowerCase());
            else selectedEquipment.remove(name.toLowerCase());
        });

        binding.chipGroupEquipment.addView(chip);
    }

    private void saveEquipment() {
        if (selectedEquipment.isEmpty()) {
            Toast.makeText(this,
                    "Please select at least one equipment", Toast.LENGTH_SHORT).show();
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