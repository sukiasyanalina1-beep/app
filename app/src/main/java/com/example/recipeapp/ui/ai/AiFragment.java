package com.example.recipeapp.ui.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.recipeapp.adapters.ChatAdapter;
import com.example.recipeapp.databinding.FragmentAiBinding;
import com.example.recipeapp.models.ChatMessage;
import com.example.recipeapp.BuildConfig;
import org.json.*;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AiFragment extends Fragment {

    private static final String TAG = "AiFragment";

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1/models/" +
                    "gemini-2.0-flash-lite:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
    private static final String SYSTEM_PROMPT =
            "You are a helpful cooking assistant called AI Chef. " +
                    "Help users with recipes, ingredient substitutions, cooking techniques, " +
                    "meal planning, nutrition, and food questions. " +
                    "Keep answers concise, friendly and practical. " +
                    "Do not use markdown symbols like ** or ## in your responses. " +
                    "Write in plain text only.";

    private FragmentAiBinding binding;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "Using API URL: " + API_URL);

        setupChat();
        addMessage(
                "Hi! I'm your AI Chef 👨‍🍳\n\n" +
                        "Ask me anything about:\n" +
                        "• Recipes and cooking techniques\n" +
                        "• Ingredient substitutions\n" +
                        "• Meal planning and nutrition\n" +
                        "• Food storage and preparation tips",
                false
        );
    }

    private void setupChat() {
        adapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text);
                binding.etMessage.setText("");
            }
        });

        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text);
                binding.etMessage.setText("");
                return true;
            }
            return false;
        });
    }

    private void sendMessage(String userText) {
        addMessage(userText, true);
        binding.btnSend.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        try {
            // Build conversation history
            JSONArray contents = new JSONArray();

            // Add system prompt as first user message + model acknowledgement
            // This works for all Gemini models regardless of version
            JSONObject systemUserMsg = new JSONObject();
            systemUserMsg.put("role", "user");
            JSONArray systemUserParts = new JSONArray();
            JSONObject systemUserPart = new JSONObject();
            systemUserPart.put("text", SYSTEM_PROMPT);
            systemUserParts.put(systemUserPart);
            systemUserMsg.put("parts", systemUserParts);
            contents.put(systemUserMsg);

            JSONObject systemModelMsg = new JSONObject();
            systemModelMsg.put("role", "model");
            JSONArray systemModelParts = new JSONArray();
            JSONObject systemModelPart = new JSONObject();
            systemModelPart.put("text", "Understood! I am AI Chef, ready to help with cooking.");
            systemModelParts.put(systemModelPart);
            systemModelMsg.put("parts", systemModelParts);
            contents.put(systemModelMsg);

            // Add actual conversation history
            for (ChatMessage msg : messages) {
                JSONObject content = new JSONObject();
                content.put("role", msg.isFromUser() ? "user" : "model");
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", msg.getText());
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
            }

            // Generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);

            // Request body — NO system_instruction field
            JSONObject body = new JSONObject();
            body.put("contents", contents);
            body.put("generationConfig", generationConfig);

            Log.d(TAG, "Request body: " + body.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.get("application/json; charset=utf-8")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Request failed: " + e.getMessage());
                    if (getActivity() == null) return;
                    requireActivity().runOnUiThread(() -> {
                        addMessage("Sorry, I couldn't connect. Please check your internet.", false);
                        resetSendButton();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    String responseBody = response.body() != null
                            ? response.body().string() : "";

                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);

                    if (getActivity() == null) return;

                    requireActivity().runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);

                            if (json.has("error")) {
                                JSONObject error = json.getJSONObject("error");
                                String msg = error.optString("message", "Unknown error");
                                int code = error.optInt("code", 0);
                                Log.e(TAG, "API error " + code + ": " + msg);
                                addMessage("API error: " + msg, false);
                                resetSendButton();
                                return;
                            }

                            String reply = json
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            addMessage(reply.trim(), false);

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parse error: " + e.getMessage());
                            addMessage("Sorry, unexpected response. Please try again.", false);
                        }
                        resetSendButton();
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error building request: " + e.getMessage());
            addMessage("Error building request. Please try again.", false);
            resetSendButton();
        }
    }

    private void addMessage(String text, boolean fromUser) {
        messages.add(new ChatMessage(text, fromUser));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void resetSendButton() {
        if (binding != null) {
            binding.btnSend.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}