package com.example.recipeapp.adapters;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.example.recipeapp.databinding.ItemChatUserBinding;
import com.example.recipeapp.databinding.ItemChatAiBinding;
import com.example.recipeapp.models.ChatMessage;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI   = 1;

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isFromUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            return new UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false));
        } else {
            return new AiViewHolder(ItemChatAiBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else {
            ((AiViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemChatUserBinding binding;
        UserViewHolder(ItemChatUserBinding b) { super(b.getRoot()); binding = b; }
        void bind(ChatMessage msg) { binding.tvMessage.setText(msg.getText()); }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        final ItemChatAiBinding binding;
        AiViewHolder(ItemChatAiBinding b) { super(b.getRoot()); binding = b; }
        void bind(ChatMessage msg) { binding.tvMessage.setText(msg.getText()); }
    }
}