package com.example.recipeapp.ui.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.recipeapp.adapters.ShoppingItemAdapter;
import com.example.recipeapp.databinding.FragmentShoppingBinding;
import com.example.recipeapp.models.ShoppingItem;
import java.util.*;

public class ShoppingFragment extends Fragment {

    private FragmentShoppingBinding binding;
    private ShoppingItemAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration listenerRegistration;
    private String familyId = null;
    private String currentUserName = "";
    private String currentUserId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShoppingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        setupRecyclerView();
        loadUserDataThenSetup();
    }

    private void setupRecyclerView() {
        adapter = new ShoppingItemAdapter(
                item -> toggleItem(item),
                item -> confirmDelete(item)
        );
        binding.rvShoppingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShoppingList.setAdapter(adapter);
    }

    private void loadUserDataThenSetup() {
        binding.tvFamilyCode.setText("Loading...");

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    currentUserName = doc.getString("displayName");
                    if (currentUserName == null) currentUserName = "Member";
                    familyId = doc.getString("familyId");

                    if (familyId == null) {
                        // No family — create one
                        createFamilyForUser();
                    } else {
                        setupWithFamily();
                    }
                });
    }

    private void createFamilyForUser() {
        String newFamilyId = db.collection("families").document().getId();
        Map<String, Object> family = new HashMap<>();
        family.put("ownerId", currentUserId);
        family.put("members", Collections.singletonList(currentUserId));
        family.put("createdAt", System.currentTimeMillis());

        db.collection("families").document(newFamilyId)
                .set(family)
                .addOnSuccessListener(v -> {
                    db.collection("users").document(currentUserId)
                            .update("familyId", newFamilyId)
                            .addOnSuccessListener(v2 -> {
                                familyId = newFamilyId;
                                setupWithFamily();
                            });
                });
    }

    private void setupWithFamily() {
        // Show family code
        binding.tvFamilyCode.setText("Family code: " + familyId.substring(0, 8).toUpperCase());

        // Load member count
        db.collection("families").document(familyId).get()
                .addOnSuccessListener(doc -> {
                    List<String> members = (List<String>) doc.get("members");
                    int count = members != null ? members.size() : 1;
                    binding.tvMemberCount.setText(count + " member" + (count > 1 ? "s" : ""));
                });

        setupButtons();
        listenToShoppingList();
    }

    private void setupButtons() {
        // Add item button
        binding.btnAddItem.setOnClickListener(v -> {
            String text = binding.etNewItem.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            addItem(text);
            binding.etNewItem.setText("");
        });

        // Clear checked items
        binding.btnClearChecked.setOnClickListener(v -> clearCheckedItems());

        // Invite family member
        binding.btnInvite.setOnClickListener(v -> showInviteDialog());

        // Join family
        binding.btnJoinFamily.setOnClickListener(v -> showJoinFamilyDialog());
    }

    private void listenToShoppingList() {
        if (familyId == null) return;

        listenerRegistration = db.collection("families")
                .document(familyId)
                .collection("shoppingItems")
                .orderBy("createdAt")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<ShoppingItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        item.setId(doc.getId());
                        items.add(item);
                    }
                    adapter.submitList(items);

                    // Count stats
                    long total = items.size();
                    long checked = items.stream().filter(ShoppingItem::isChecked).count();
                    binding.tvItemCount.setText(total + " items · " + checked + " done");
                    binding.tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void addItem(String name) {
        if (familyId == null) return;
        ShoppingItem item = new ShoppingItem(name, currentUserId, currentUserName);
        db.collection("families").document(familyId)
                .collection("shoppingItems")
                .add(item)
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to add item", Toast.LENGTH_SHORT).show());
    }

    private void toggleItem(ShoppingItem item) {
        if (familyId == null) return;
        db.collection("families").document(familyId)
                .collection("shoppingItems")
                .document(item.getId())
                .update("checked", !item.isChecked());
    }

    private void confirmDelete(ShoppingItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove item")
                .setMessage("Remove \"" + item.getName() + "\" from the list?")
                .setPositiveButton("Remove", (d, w) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(ShoppingItem item) {
        if (familyId == null) return;
        db.collection("families").document(familyId)
                .collection("shoppingItems")
                .document(item.getId())
                .delete();
    }

    private void clearCheckedItems() {
        if (familyId == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear checked items")
                .setMessage("Remove all checked items from the list?")
                .setPositiveButton("Clear", (d, w) -> {
                    db.collection("families").document(familyId)
                            .collection("shoppingItems")
                            .whereEqualTo("checked", true)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot doc : snapshot) {
                                    batch.delete(doc.getReference());
                                }
                                batch.commit();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInviteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Invite family member")
                .setMessage("Share this code with your family member:\n\n" +
                        familyId.substring(0, 8).toUpperCase() +
                        "\n\nThey can enter this code in their app under Shopping → Join Family.")
                .setPositiveButton("Copy code", (d, w) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) requireContext()
                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            "Family code", familyId.substring(0, 8).toUpperCase());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(),
                            "Code copied!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showJoinFamilyDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter family code");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Join a family")
                .setMessage("Enter the 8-character family code shared by your family member:")
                .setView(input)
                .setPositiveButton("Join", (d, w) -> {
                    String code = input.getText().toString().trim().toUpperCase();
                    if (code.length() < 8) {
                        Toast.makeText(requireContext(),
                                "Invalid code", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    joinFamily(code);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinFamily(String shortCode) {
        // Find the full family ID that starts with this short code
        db.collection("families").get()
                .addOnSuccessListener(snapshot -> {
                    String foundId = null;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (doc.getId().toUpperCase().startsWith(shortCode)) {
                            foundId = doc.getId();
                            break;
                        }
                    }

                    if (foundId == null) {
                        Toast.makeText(requireContext(),
                                "Family not found. Check the code and try again.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    final String finalFamilyId = foundId;

                    // Add user to family members
                    db.collection("families").document(finalFamilyId)
                            .update("members",
                                    FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener(v -> {
                                // Update user's familyId
                                db.collection("users").document(currentUserId)
                                        .update("familyId", finalFamilyId)
                                        .addOnSuccessListener(v2 -> {
                                            // Stop old listener
                                            if (listenerRegistration != null) {
                                                listenerRegistration.remove();
                                            }
                                            familyId = finalFamilyId;
                                            setupWithFamily();
                                            Toast.makeText(requireContext(),
                                                    "Joined family successfully!",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed to join: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) listenerRegistration.remove();
        binding = null;
    }
}