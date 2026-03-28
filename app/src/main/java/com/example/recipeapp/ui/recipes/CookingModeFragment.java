package com.example.recipeapp.ui.recipes;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.recipeapp.databinding.FragmentCookingModeBinding;
import com.example.recipeapp.models.Recipe;
import java.util.List;
import java.util.Locale;

public class CookingModeFragment extends Fragment implements TextToSpeech.OnInitListener {

    private FragmentCookingModeBinding binding;
    private List<String> steps;
    private int currentStep = 0;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean isSpeaking = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCookingModeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Text-to-Speech
        tts = new TextToSpeech(requireContext(), this);

        String recipeId = getArguments().getString("recipeId");
        loadRecipe(recipeId);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.ENGLISH);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.e("TTS", "Language not supported");
            } else {
                ttsReady = true;
                // Set speech rate — slightly slower for cooking instructions
                tts.setSpeechRate(0.85f);
                tts.setPitch(1.0f);
            }
        } else {
            android.util.Log.e("TTS", "TTS initialization failed");
        }
    }

    private void loadRecipe(String recipeId) {
        FirebaseFirestore.getInstance()
                .collection("recipes")
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    Recipe recipe = doc.toObject(Recipe.class);
                    if (recipe == null) return;
                    steps = recipe.getSteps();

                    if (steps == null || steps.isEmpty()) {
                        binding.tvStepText.setText("No steps available for this recipe.");
                        binding.btnNextStep.setEnabled(false);
                        return;
                    }

                    binding.tvRecipeTitle.setText(recipe.getTitle());
                    binding.tvTotalSteps.setText("/ " + steps.size());
                    showStep(0);
                });
    }

    private void showStep(int index) {
        currentStep = index;
        String stepText = steps.get(index);

        binding.tvStepNumber.setText(String.valueOf(index + 1));
        binding.tvStepText.setText(stepText);

        // Progress bar
        int progress = (int) (((index + 1f) / steps.size()) * 100);
        binding.progressCooking.setProgress(progress);

        // Button states
        binding.btnPrevStep.setEnabled(index > 0);
        binding.btnNextStep.setText(
                index == steps.size() - 1 ? "Finish!" : "Next →");

        // Update speak button icon
        updateSpeakButton(false);

        // Auto-speak the step
        speakStep(stepText);

        binding.btnPrevStep.setOnClickListener(v -> {
            stopSpeaking();
            showStep(currentStep - 1);
        });

        binding.btnNextStep.setOnClickListener(v -> {
            stopSpeaking();
            if (currentStep < steps.size() - 1) {
                showStep(currentStep + 1);
            } else {
                showCookingComplete();
            }
        });

        // Speak / stop button
        binding.btnSpeak.setOnClickListener(v -> {
            if (isSpeaking) {
                stopSpeaking();
            } else {
                speakStep(stepText);
            }
        });
    }

    private void speakStep(String text) {
        if (!ttsReady || tts == null) return;

        // Announce step number then read the step
        String announcement = "Step " + (currentStep + 1) + ". " + text;

        tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "step_" + currentStep);
        isSpeaking = true;
        updateSpeakButton(true);

        // Listen for when speech finishes
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    isSpeaking = false;
                    updateSpeakButton(false);
                });
            }

            @Override
            public void onError(String utteranceId) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    isSpeaking = false;
                    updateSpeakButton(false);
                });
            }
        });
    }

    private void stopSpeaking() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
        isSpeaking = false;
        updateSpeakButton(false);
    }

    private void updateSpeakButton(boolean speaking) {
        if (binding == null) return;
        if (speaking) {
            binding.btnSpeak.setText("⏹ Stop");
            binding.btnSpeak.setIconResource(android.R.drawable.ic_media_pause);
        } else {
            binding.btnSpeak.setText("🔊 Speak");
            binding.btnSpeak.setIconResource(android.R.drawable.ic_lock_silent_mode_off);
        }
    }

    private void showCookingComplete() {
        stopSpeaking();

        // Speak congratulations
        if (ttsReady && tts != null) {
            tts.speak(
                    "Congratulations! You have completed the recipe. Enjoy your meal!",
                    TextToSpeech.QUEUE_FLUSH, null, "complete");
        }

        binding.cookingContent.setVisibility(View.GONE);
        binding.completionState.setVisibility(View.VISIBLE);
        binding.btnBackToRecipe.setOnClickListener(v -> {
            stopSpeaking();
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        stopSpeaking();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        binding = null;
    }
}