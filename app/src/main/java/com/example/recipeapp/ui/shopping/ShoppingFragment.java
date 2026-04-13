package com.example.recipeapp.ui.shopping;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.recipeapp.MainActivity;
import com.example.recipeapp.adapters.ShoppingItemAdapter;
import com.example.recipeapp.databinding.FragmentShoppingBinding;
import com.example.recipeapp.models.Invitation;
import com.example.recipeapp.models.ShoppingItem;
import java.util.*;

public class ShoppingFragment extends Fragment {

    private FragmentShoppingBinding binding;
    private ShoppingItemAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Current user info
    private String currentUserId;
    private String currentUserName = "";
    private String currentUserEmail = "";

    // Active group
    private String currentGroupId = null;
    private String currentGroupName = "";

    // All groups: groupId -> groupName
    private final Map<String, String> userGroups = new LinkedHashMap<>();

    // Pending invitations
    private final List<Invitation> pendingInvitations = new ArrayList<>();

    // All items (unfiltered) from the live listener
    private final List<ShoppingItem> allShoppingItems = new ArrayList<>();
    private String currentFilter = "all";

    // Dropdown state
    private boolean groupDetailsExpanded = false;
    private boolean isGroupOwner = false;

    // Firestore listeners
    private ListenerRegistration shoppingListener;
    private ListenerRegistration invitationListener;

    // Undo-on-done
    private final Handler deletionHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingDeletions = new HashMap<>();

    // Member bubble colors
    private static final int[] MEMBER_COLORS = {
        0xFF5C6BC0, 0xFF26A69A, 0xFFEF5350, 0xFFAB47BC,
        0xFF42A5F5, 0xFFFF7043, 0xFF66BB6A, 0xFFEC407A
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable @Override
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
        if (auth.getCurrentUser() == null) return;
        currentUserId = auth.getCurrentUser().getUid();
        currentUserEmail = auth.getCurrentUser().getEmail() != null
                ? auth.getCurrentUser().getEmail().toLowerCase() : "";

        setupRecyclerView();
        setupButtons();
        showLoading(true);
        loadUserData();
        checkPendingDeepLink();
    }

    private void checkPendingDeepLink() {
        if (!(getActivity() instanceof MainActivity)) return;
        Uri uri = ((MainActivity) getActivity()).consumePendingInviteUri();
        if (uri == null) return;
        String groupId   = uri.getQueryParameter("groupId");
        String groupName = uri.getQueryParameter("groupName");
        String from      = uri.getQueryParameter("from");
        if (groupId == null) return;
        // Delay slightly so the loading UI settles before showing the dialog
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> showDeepLinkAcceptDialog(groupId, groupName, from), 600);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shoppingListener != null) shoppingListener.remove();
        if (invitationListener != null) invitationListener.remove();
        for (Runnable r : pendingDeletions.values()) deletionHandler.removeCallbacks(r);
        pendingDeletions.clear();
        binding = null;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new ShoppingItemAdapter(this::toggleItem, this::confirmDelete);
        binding.rvShoppingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShoppingList.setAdapter(adapter);
    }

    private void setupButtons() {
        // Add item
        binding.btnAddItem.setOnClickListener(v -> {
            String text = binding.etNewItem.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            addItem(text);
            binding.etNewItem.setText("");
        });
        binding.etNewItem.setOnEditorActionListener((v, actionId, event) -> {
            String text = binding.etNewItem.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) { addItem(text); binding.etNewItem.setText(""); }
            return true;
        });

        // Stats bar
        binding.btnClearChecked.setOnClickListener(v -> clearCheckedItems());

        // Header actions
        binding.btnManageGroups.setOnClickListener(v -> showGroupManager());
        binding.btnSwitchGroup.setOnClickListener(v -> toggleGroupDetails());
        binding.btnViewMembers.setOnClickListener(v -> showMembersDialog());
        binding.btnInvite.setOnClickListener(v -> showInviteDialog());

        // Invitation banner
        binding.btnViewInvitations.setOnClickListener(v -> showPendingInvitationsDialog());

        // Empty state - create first group
        binding.btnCreateFirstGroup.setOnClickListener(v -> showCreateGroupDialog());

        // Filter chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (checkedIds.isEmpty()) return;
                    int id = checkedIds.get(0);
                    if (id == binding.chipPending.getId()) currentFilter = "pending";
                    else if (id == binding.chipDone.getId()) currentFilter = "done";
                    else currentFilter = "all";
                    applyFilter();
                });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadUserData() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    currentUserName = doc.getString("displayName");
                    if (currentUserName == null) currentUserName = "Member";

                    List<String> groupIds = (List<String>) doc.get("groupIds");

                    // Migrate old familyId → groupIds
                    String oldFamilyId = doc.getString("familyId");
                    if ((groupIds == null || groupIds.isEmpty()) && oldFamilyId != null) {
                        groupIds = Collections.singletonList(oldFamilyId);
                        db.collection("users").document(currentUserId)
                                .update("groupIds", groupIds);
                    }

                    if (groupIds == null || groupIds.isEmpty()) {
                        showLoading(false);
                        showNoGroupsState();
                    } else {
                        loadGroups(new ArrayList<>(groupIds));
                    }
                    listenToPendingInvitations();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    showLoading(false);
                    showNoGroupsState();
                });
    }

    private void loadGroups(List<String> groupIds) {
        userGroups.clear();
        final int[] loaded = {0};
        for (String groupId : groupIds) {
            db.collection("groups").document(groupId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            userGroups.put(doc.getId(), name != null ? name : "Group");
                        }
                        if (++loaded[0] == groupIds.size()) {
                            if (binding == null) return;
                            showLoading(false);
                            if (userGroups.isEmpty()) showNoGroupsState();
                            else if (currentGroupId == null) {
                                // Don't override a group already switched to via deep link
                                String firstId = userGroups.keySet().iterator().next();
                                switchToGroup(firstId, userGroups.get(firstId));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (++loaded[0] == groupIds.size()) {
                            if (binding == null) return;
                            showLoading(false);
                            if (userGroups.isEmpty()) showNoGroupsState();
                            else if (currentGroupId == null) {
                                String firstId = userGroups.keySet().iterator().next();
                                switchToGroup(firstId, userGroups.get(firstId));
                            }
                        }
                    });
        }
    }

    private void listenToPendingInvitations() {
        if (currentUserEmail.isEmpty()) return;
        if (invitationListener != null) invitationListener.remove();
        invitationListener = db.collection("invitations")
                .whereEqualTo("invitedEmail", currentUserEmail)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || binding == null) return;
                    pendingInvitations.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Invitation inv = doc.toObject(Invitation.class);
                        if (inv == null) continue;
                        inv.setId(doc.getId());
                        pendingInvitations.add(inv);
                    }
                    updateInvitationBanner();
                });
    }

    // ── UI state ──────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showNoGroupsState() {
        if (binding == null) return;
        binding.activeGroupRow.setVisibility(View.GONE);
        binding.headerNoGroup.setVisibility(View.VISIBLE);
        binding.filterBar.setVisibility(View.GONE);
        binding.statsBar.setVisibility(View.GONE);
        binding.rvShoppingList.setVisibility(View.GONE);
        binding.tvEmptyList.setVisibility(View.GONE);
        binding.addItemBar.setVisibility(View.GONE);
        binding.emptyNoGroups.setVisibility(View.VISIBLE);
    }

    private void showGroupState() {
        if (binding == null) return;
        binding.headerNoGroup.setVisibility(View.GONE);
        binding.activeGroupRow.setVisibility(View.VISIBLE);
        binding.filterBar.setVisibility(View.VISIBLE);
        binding.statsBar.setVisibility(View.VISIBLE);
        binding.rvShoppingList.setVisibility(View.VISIBLE);
        binding.addItemBar.setVisibility(View.VISIBLE);
        binding.emptyNoGroups.setVisibility(View.GONE);
    }

    private void toggleGroupDetails() {
        if (binding == null) return;
        groupDetailsExpanded = !groupDetailsExpanded;
        binding.groupDetailsPanel.setVisibility(groupDetailsExpanded ? View.VISIBLE : View.GONE);
        binding.tvDropdownArrow.setText(groupDetailsExpanded ? "  ▴" : "  ▾");
    }

    private void switchToGroup(String groupId, String groupName) {
        currentGroupId = groupId;
        currentGroupName = groupName;
        groupDetailsExpanded = false;
        if (binding == null) return;
        binding.tvGroupName.setText(groupName);
        binding.groupDetailsPanel.setVisibility(View.GONE);
        binding.tvDropdownArrow.setText("  ▾");
        showGroupState();
        loadMemberBubbles(groupId);
        if (shoppingListener != null) shoppingListener.remove();
        listenToShoppingList();
    }

    private void updateInvitationBanner() {
        if (binding == null) return;
        if (pendingInvitations.isEmpty()) {
            binding.invitationBanner.setVisibility(View.GONE);
        } else {
            int n = pendingInvitations.size();
            binding.tvInvitationCount.setText(
                    "📨 " + n + " pending invitation" + (n > 1 ? "s" : ""));
            binding.invitationBanner.setVisibility(View.VISIBLE);
        }
    }

    // ── Member bubbles ────────────────────────────────────────────────────────

    private void loadMemberBubbles(String groupId) {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    isGroupOwner = currentUserId.equals(doc.getString("ownerId"));
                    List<String> memberIds = (List<String>) doc.get("members");
                    if (memberIds == null) return;
                    binding.llMemberBubbles.removeAllViews();
                    for (int i = 0; i < memberIds.size(); i++) {
                        final int color = MEMBER_COLORS[i % MEMBER_COLORS.length];
                        final String uid = memberIds.get(i);
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (binding == null) return;
                                    String name = uid.equals(currentUserId)
                                            ? currentUserName : "?";
                                    String n = userDoc.getString("displayName");
                                    if (n != null && !n.isEmpty()) name = n;
                                    addMemberBubble(name, color);
                                });
                    }
                });
    }

    private void addMemberBubble(String name, int color) {
        if (binding == null) return;
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (40 * density);
        int margin = (int) (8 * density);

        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(margin);
        bubble.setLayoutParams(lp);

        // Colored circle
        FrameLayout circle = new FrameLayout(requireContext());
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(color);
        circle.setBackground(bg);
        circle.setLayoutParams(new LinearLayout.LayoutParams(size, size));

        TextView tvInitials = new TextView(requireContext());
        tvInitials.setText(getInitials(name));
        tvInitials.setTextColor(0xFFFFFFFF);
        tvInitials.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvInitials.setTypeface(null, android.graphics.Typeface.BOLD);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        flp.gravity = Gravity.CENTER;
        tvInitials.setLayoutParams(flp);
        circle.addView(tvInitials);

        // First name below bubble
        String firstName = name.trim().split("\\s+")[0];
        TextView tvName = new TextView(requireContext());
        tvName.setText(firstName);
        tvName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        tvName.setTextColor(0xFF666666);
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setMaxWidth((int) (48 * density));
        tvName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = (int) (3 * density);
        tvName.setLayoutParams(nameLp);

        bubble.addView(circle);
        bubble.addView(tvName);
        binding.llMemberBubbles.addView(bubble);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    // ── Members dialog ────────────────────────────────────────────────────────

    private void showMembersDialog() {
        if (currentGroupId == null) return;
        db.collection("groups").document(currentGroupId).get()
                .addOnSuccessListener(groupDoc -> {
                    if (binding == null) return;
                    List<String> memberIds = (List<String>) groupDoc.get("members");
                    String ownerId = groupDoc.getString("ownerId");
                    if (memberIds == null || memberIds.isEmpty()) return;

                    final int[] loaded = {0};
                    // [uid, displayName, role]
                    final List<String[]> members = Collections.synchronizedList(new ArrayList<>());

                    for (String uid : memberIds) {
                        db.collection("users").document(uid).get()
                                .addOnCompleteListener(task -> {
                                    String name = uid.equals(currentUserId)
                                            ? currentUserName : "Unknown";
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        String n = task.getResult().getString("displayName");
                                        if (n != null && !n.isEmpty()) name = n;
                                    }
                                    String role = uid.equals(ownerId) ? "Owner" : "Member";
                                    members.add(new String[]{uid, name, role});
                                    if (++loaded[0] == memberIds.size()) {
                                        if (isAdded() && getActivity() != null)
                                            requireActivity().runOnUiThread(
                                                    () -> displayMembersDialog(members));
                                    }
                                });
                    }
                });
    }

    private void displayMembersDialog(List<String[]> members) {
        if (binding == null) return;
        float density = getResources().getDisplayMetrics().density;

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * density);
        container.setPadding(pad, (int) (8 * density), pad, (int) (8 * density));

        final AlertDialog[] dialogRef = {null};

        for (int i = 0; i < members.size(); i++) {
            String[] m = members.get(i);
            int color = MEMBER_COLORS[i % MEMBER_COLORS.length];

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rowPad = (int) (12 * density);
            row.setPadding(0, rowPad, 0, rowPad);

            // Mini bubble (30dp)
            int bubbleSize = (int) (30 * density);
            FrameLayout circle = new FrameLayout(requireContext());
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(color);
            circle.setBackground(bg);
            LinearLayout.LayoutParams clp =
                    new LinearLayout.LayoutParams(bubbleSize, bubbleSize);
            clp.setMarginEnd((int) (12 * density));
            circle.setLayoutParams(clp);

            TextView tvI = new TextView(requireContext());
            tvI.setText(getInitials(m[1]));
            tvI.setTextColor(0xFFFFFFFF);
            tvI.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
            tvI.setTypeface(null, android.graphics.Typeface.BOLD);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            ilp.gravity = Gravity.CENTER;
            tvI.setLayoutParams(ilp);
            circle.addView(tvI);
            row.addView(circle);

            // Name
            TextView tvName = new TextView(requireContext());
            tvName.setText(m[1]);
            tvName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nlp);
            row.addView(tvName);

            // Role badge
            TextView tvRole = new TextView(requireContext());
            tvRole.setText(m[2]);
            tvRole.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            tvRole.setTextColor("Owner".equals(m[2]) ? 0xFF6200EE : 0xFF888888);
            row.addView(tvRole);

            // Remove button (owner only, non-owner members)
            if (isGroupOwner && !"Owner".equals(m[2])) {
                final String uid = m[0];
                final String name = m[1];
                Button btnRemove = new Button(requireContext());
                btnRemove.setText("Remove");
                btnRemove.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                btnRemove.setTextColor(0xFFB00020);
                btnRemove.setBackground(null);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rlp.setMarginStart((int) (8 * density));
                btnRemove.setLayoutParams(rlp);
                btnRemove.setOnClickListener(v ->
                        confirmRemoveMember(dialogRef[0], uid, name));
                row.addView(btnRemove);
            }

            container.addView(row);

            // Divider (except last)
            if (i < members.size() - 1) {
                View div = new View(requireContext());
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(0x1A000000);
                container.addView(div);
            }
        }

        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.addView(container);

        dialogRef[0] = new AlertDialog.Builder(requireContext())
                .setTitle("Members · " + members.size())
                .setView(sv)
                .setNegativeButton("Close", null)
                .show();
    }

    // ── Remove member ─────────────────────────────────────────────────────────

    private void confirmRemoveMember(AlertDialog parent, String uid, String name) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove " + name + "?")
                .setMessage("They will lose access to this group's shopping list.")
                .setPositiveButton("Remove", (d, w) -> {
                    if (parent != null) parent.dismiss();
                    WriteBatch batch = db.batch();
                    batch.update(db.collection("groups").document(currentGroupId),
                            "members", FieldValue.arrayRemove(uid));
                    batch.update(db.collection("users").document(uid),
                            "groupIds", FieldValue.arrayRemove(currentGroupId));
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                if (binding == null) return;
                                Toast.makeText(requireContext(),
                                        name + " removed", Toast.LENGTH_SHORT).show();
                                loadMemberBubbles(currentGroupId);
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                    "Failed to remove member", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Invite members ────────────────────────────────────────────────────────

    // ── Deep link accept ──────────────────────────────────────────────────────

    private void showDeepLinkAcceptDialog(String groupId, String groupName, String from) {
        if (binding == null || !isAdded()) return;
        String title = (from != null && !from.isEmpty())
                ? from + " invited you!" : "You've been invited!";
        String displayName = (groupName != null && !groupName.isEmpty()) ? groupName : "a group";
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage("Join \"" + displayName + "\"?")
                .setPositiveButton("Accept", (d, w) -> acceptDeepLinkInvite(groupId, displayName))
                .setNegativeButton("Decline", null)
                .show();
    }

    private void acceptDeepLinkInvite(String groupId, String fallbackName) {
        if (currentUserId == null) return;
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(),
                                "This group no longer exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> members = (List<String>) doc.get("members");
                    if (members != null && members.contains(currentUserId)) {
                        Toast.makeText(requireContext(),
                                "You're already in this group!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String name = doc.getString("name");
                    final String groupName = (name != null) ? name : fallbackName;
                    WriteBatch batch = db.batch();
                    batch.update(db.collection("groups").document(groupId),
                            "members", FieldValue.arrayUnion(currentUserId));
                    batch.update(db.collection("users").document(currentUserId),
                            "groupIds", FieldValue.arrayUnion(groupId));
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                if (binding == null) return;
                                userGroups.put(groupId, groupName);
                                switchToGroup(groupId, groupName);
                                Toast.makeText(requireContext(),
                                        "Joined \"" + groupName + "\"!",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                    "Failed to join: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                });
    }

    // ── Invite members ────────────────────────────────────────────────────────

    private void showInviteDialog() {
        if (currentGroupId == null) return;
        String[] options = {"Invite by email", "Share invite link"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Invite to \"" + currentGroupName + "\"")
                .setItems(options, (d, which) -> {
                    if (which == 0) showInviteByEmailDialog();
                    else shareInviteLink();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInviteByEmailDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("their@email.com");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Invite by email")
                .setMessage("Enter the email address of the person you want to invite. "
                        + "They'll see the invitation when they open the app.")
                .setView(input)
                .setPositiveButton("Send invite", (d, w) -> {
                    String email = input.getText().toString().trim().toLowerCase();
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(requireContext(),
                                "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendEmailInvitation(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendEmailInvitation(String email) {
        // Check for existing pending invite first to avoid duplicates
        db.collection("invitations")
                .whereEqualTo("groupId", currentGroupId)
                .whereEqualTo("invitedEmail", email)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    if (!snap.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "An invitation is already pending for " + email,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> inv = new HashMap<>();
                    inv.put("groupId", currentGroupId);
                    inv.put("groupName", currentGroupName);
                    inv.put("invitedByUid", currentUserId);
                    inv.put("invitedByName", currentUserName);
                    inv.put("invitedEmail", email);
                    inv.put("status", "pending");
                    inv.put("createdAt", System.currentTimeMillis());

                    db.collection("invitations").add(inv)
                            .addOnSuccessListener(ref -> Toast.makeText(requireContext(),
                                    "Invitation sent to " + email, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                    "Failed to send invitation: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                });
    }

    private void shareInviteLink() {
        String link = "recipeapp://invite"
                + "?groupId=" + currentGroupId
                + "&groupName=" + Uri.encode(currentGroupName)
                + "&from=" + Uri.encode(currentUserName);
        String message = "Hey! Join my \"" + currentGroupName
                + "\" shopping list on RecipeApp.\n\n"
                + "Tap the link below to accept instantly:\n" + link + "\n\n"
                + "(You need RecipeApp installed on your phone)";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, "Share invite via…"));
    }

    // ── Shopping list ─────────────────────────────────────────────────────────

    private void listenToShoppingList() {
        if (currentGroupId == null) return;
        shoppingListener = db.collection("groups")
                .document(currentGroupId)
                .collection("shoppingItems")
                .orderBy("createdAt")
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null || binding == null) return;
                    allShoppingItems.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        if (item == null) continue;
                        item.setId(doc.getId());
                        allShoppingItems.add(item);
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        if (binding == null) return;
        List<ShoppingItem> filtered = new ArrayList<>();
        for (ShoppingItem item : allShoppingItems) {
            if ("pending".equals(currentFilter) && item.isChecked()) continue;
            if ("done".equals(currentFilter) && !item.isChecked()) continue;
            filtered.add(item);
        }
        adapter.submitList(filtered);

        long total = allShoppingItems.size();
        long done = 0;
        for (ShoppingItem i : allShoppingItems) if (i.isChecked()) done++;
        long pending = total - done;

        binding.chipAll.setText("All (" + total + ")");
        binding.chipPending.setText("To Buy (" + pending + ")");
        binding.chipDone.setText("Done (" + done + ")");
        binding.tvItemCount.setText(total + " items · " + done + " done");
        binding.tvEmptyList.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addItem(String name) {
        if (currentGroupId == null) return;
        ShoppingItem item = new ShoppingItem(name, currentUserId, currentUserName);
        db.collection("groups").document(currentGroupId)
                .collection("shoppingItems").add(item)
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to add item", Toast.LENGTH_SHORT).show());
    }

    private void toggleItem(ShoppingItem item) {
        if (currentGroupId == null) return;
        boolean nowChecked = !item.isChecked();
        db.collection("groups").document(currentGroupId)
                .collection("shoppingItems").document(item.getId())
                .update("checked", nowChecked);
        if (nowChecked) {
            showUndoSnackbar(item);
        } else {
            Runnable pending = pendingDeletions.remove(item.getId());
            if (pending != null) deletionHandler.removeCallbacks(pending);
        }
    }

    private void showUndoSnackbar(ShoppingItem item) {
        Runnable existing = pendingDeletions.remove(item.getId());
        if (existing != null) deletionHandler.removeCallbacks(existing);

        Runnable deletion = () -> {
            pendingDeletions.remove(item.getId());
            if (currentGroupId != null)
                db.collection("groups").document(currentGroupId)
                        .collection("shoppingItems").document(item.getId()).delete();
        };
        pendingDeletions.put(item.getId(), deletion);
        deletionHandler.postDelayed(deletion, 5000);

        Snackbar.make(binding.getRoot(),
                        "\"" + item.getName() + "\" done", 5000)
                .setAnchorView(binding.addItemBar)
                .setAction("Undo", v -> {
                    Runnable pending = pendingDeletions.remove(item.getId());
                    if (pending != null) deletionHandler.removeCallbacks(pending);
                    if (currentGroupId != null)
                        db.collection("groups").document(currentGroupId)
                                .collection("shoppingItems").document(item.getId())
                                .update("checked", false);
                })
                .show();
    }

    private void confirmDelete(ShoppingItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove item")
                .setMessage("Remove \"" + item.getName() + "\"?")
                .setPositiveButton("Remove", (d, w) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(ShoppingItem item) {
        if (currentGroupId == null) return;
        db.collection("groups").document(currentGroupId)
                .collection("shoppingItems").document(item.getId()).delete();
    }

    private void clearCheckedItems() {
        if (currentGroupId == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear checked items")
                .setMessage("Remove all done items from the list?")
                .setPositiveButton("Clear", (d, w) ->
                        db.collection("groups").document(currentGroupId)
                                .collection("shoppingItems")
                                .whereEqualTo("checked", true).get()
                                .addOnSuccessListener(snap -> {
                                    WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot doc : snap)
                                        batch.delete(doc.getReference());
                                    batch.commit();
                                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Group management ──────────────────────────────────────────────────────

    private void showGroupManager() {
        List<String> ids = new ArrayList<>(userGroups.keySet());
        List<String> names = new ArrayList<>(userGroups.values());

        String[] items = new String[names.size() + 1];
        for (int i = 0; i < names.size(); i++)
            items[i] = (ids.get(i).equals(currentGroupId) ? "✓  " : "      ") + names.get(i);
        items[names.size()] = "+ Create new group";

        new AlertDialog.Builder(requireContext())
                .setTitle("My shopping groups")
                .setItems(items, (d, which) -> {
                    if (which == names.size()) showCreateGroupDialog();
                    else showGroupOptions(ids.get(which), names.get(which));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showGroupOptions(String groupId, String groupName) {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    boolean isOwner = currentUserId.equals(doc.getString("ownerId"));
                    boolean isActive = groupId.equals(currentGroupId);

                    List<String> options = new ArrayList<>();
                    if (!isActive) options.add("Switch to this group");
                    if (isOwner) options.add("Rename group");
                    options.add("Leave group");

                    new AlertDialog.Builder(requireContext())
                            .setTitle(groupName)
                            .setItems(options.toArray(new String[0]), (d, which) -> {
                                String chosen = options.get(which);
                                if ("Switch to this group".equals(chosen))
                                    switchToGroup(groupId, groupName);
                                else if ("Rename group".equals(chosen))
                                    showRenameGroupDialog(groupId, groupName);
                                else if ("Leave group".equals(chosen))
                                    confirmLeaveGroup(groupId, groupName);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showRenameGroupDialog(String groupId, String currentName) {
        EditText input = new EditText(requireContext());
        input.setText(currentName);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setPadding(48, 24, 48, 24);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename group")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newName) || newName.equals(currentName)) return;
                    renameGroup(groupId, newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameGroup(String groupId, String newName) {
        db.collection("groups").document(groupId).update("name", newName)
                .addOnSuccessListener(v -> {
                    userGroups.put(groupId, newName);
                    if (groupId.equals(currentGroupId)) {
                        currentGroupName = newName;
                        if (binding != null) binding.tvGroupName.setText(newName);
                    }
                    Toast.makeText(requireContext(),
                            "Renamed to \"" + newName + "\"", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to rename", Toast.LENGTH_SHORT).show());
    }

    private void confirmLeaveGroup(String groupId, String groupName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Leave \"" + groupName + "\"?")
                .setMessage("You will lose access to this group's shopping list.\n\n"
                        + "If you are the only member, the group will be deleted.")
                .setPositiveButton("Leave", (d, w) -> leaveGroup(groupId, groupName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup(String groupId, String groupName) {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    List<String> members = (List<String>) doc.get("members");
                    boolean isOwner = currentUserId.equals(doc.getString("ownerId"));
                    boolean isLastMember = members == null || members.size() <= 1;

                    WriteBatch batch = db.batch();
                    if (isLastMember) {
                        batch.delete(db.collection("groups").document(groupId));
                    } else {
                        batch.update(db.collection("groups").document(groupId),
                                "members", FieldValue.arrayRemove(currentUserId));
                        if (isOwner && members != null) {
                            String newOwner = members.stream()
                                    .filter(uid -> !uid.equals(currentUserId))
                                    .findFirst().orElse(null);
                            if (newOwner != null)
                                batch.update(db.collection("groups").document(groupId),
                                        "ownerId", newOwner);
                        }
                    }
                    batch.update(db.collection("users").document(currentUserId),
                            "groupIds", FieldValue.arrayRemove(groupId));

                    batch.commit().addOnSuccessListener(v -> {
                        userGroups.remove(groupId);
                        Toast.makeText(requireContext(),
                                "You left \"" + groupName + "\"", Toast.LENGTH_SHORT).show();
                        if (groupId.equals(currentGroupId)) {
                            if (userGroups.isEmpty()) {
                                currentGroupId = null;
                                currentGroupName = "";
                                if (shoppingListener != null) shoppingListener.remove();
                                showNoGroupsState();
                            } else {
                                String nextId = userGroups.keySet().iterator().next();
                                switchToGroup(nextId, userGroups.get(nextId));
                            }
                        }
                    }).addOnFailureListener(e -> Toast.makeText(requireContext(),
                            "Failed to leave group", Toast.LENGTH_SHORT).show());
                });
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Group name (e.g. My Family)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create a new group")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(),
                                "Please enter a group name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createGroup(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String name) {
        Map<String, Object> group = new HashMap<>();
        group.put("name", name);
        group.put("ownerId", currentUserId);
        group.put("members", Collections.singletonList(currentUserId));
        group.put("createdAt", System.currentTimeMillis());

        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    String newId = ref.getId();
                    db.collection("users").document(currentUserId)
                            .update("groupIds", FieldValue.arrayUnion(newId))
                            .addOnSuccessListener(v -> {
                                userGroups.put(newId, name);
                                switchToGroup(newId, name);
                                Toast.makeText(requireContext(),
                                        "Group \"" + name + "\" created!",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to create group", Toast.LENGTH_SHORT).show());
    }

    // ── Email invitation (kept for completeness) ──────────────────────────────

    private void showPendingInvitationsDialog() {
        if (pendingInvitations.isEmpty()) return;
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);

        for (Invitation inv : new ArrayList<>(pendingInvitations)) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 16, 0, 16);

            TextView title = new TextView(requireContext());
            title.setText("\"" + inv.getGroupName() + "\"");
            title.setTextSize(15f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView sub = new TextView(requireContext());
            sub.setText("Invited by " + inv.getInvitedByName());
            sub.setTextSize(13f);
            sub.setTextColor(0xFF666666);

            LinearLayout buttons = new LinearLayout(requireContext());
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setPadding(0, 8, 0, 0);

            Button btnAccept = new Button(requireContext());
            btnAccept.setText("Accept");
            btnAccept.setOnClickListener(v -> acceptInvitation(inv));

            Button btnDecline = new Button(requireContext());
            btnDecline.setText("Decline");
            btnDecline.setOnClickListener(v -> declineInvitation(inv));

            buttons.addView(btnAccept);
            buttons.addView(btnDecline);
            row.addView(title);
            row.addView(sub);
            row.addView(buttons);

            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0x1A000000);
            row.addView(divider);
            container.addView(row);
        }

        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.addView(container);

        new AlertDialog.Builder(requireContext())
                .setTitle("Pending invitations")
                .setView(sv)
                .setNegativeButton("Close", null)
                .show();
    }

    private void acceptInvitation(Invitation inv) {
        db.collection("groups").document(inv.getGroupId())
                .update("members", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(v -> {
                    db.collection("users").document(currentUserId)
                            .update("groupIds", FieldValue.arrayUnion(inv.getGroupId()));
                    db.collection("invitations").document(inv.getId())
                            .update("status", "accepted");
                    userGroups.put(inv.getGroupId(), inv.getGroupName());
                    switchToGroup(inv.getGroupId(), inv.getGroupName());
                    Toast.makeText(requireContext(),
                            "Joined \"" + inv.getGroupName() + "\"!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to join group", Toast.LENGTH_SHORT).show());
    }

    private void declineInvitation(Invitation inv) {
        db.collection("invitations").document(inv.getId())
                .update("status", "declined")
                .addOnSuccessListener(v -> Toast.makeText(requireContext(),
                        "Invitation declined", Toast.LENGTH_SHORT).show());
    }
}
