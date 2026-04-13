package com.example.recipeapp.ui.recipes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.recipeapp.databinding.FragmentCookingModeBinding;
import com.example.recipeapp.models.Recipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CookingModeFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;

    private FragmentCookingModeBinding binding;
    private List<String> steps;
    private int currentStep = 0;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean isSpeaking = false;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

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

        tts = new TextToSpeech(requireContext(), this);

        Bundle args = getArguments();
        if (args == null) return;
        String recipeId = args.getString("recipeId");
        if (recipeId == null) return;
        loadRecipe(recipeId);

        checkMicPermissionAndSetup();
    }

    // ── Permission ────────────────────────────────────────────────────────────

    private void checkMicPermissionAndSetup() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer();
        } else {
            updateVoiceStatus("Voice control unavailable — microphone permission denied");
        }
    }

    // ── Speech Recognizer ─────────────────────────────────────────────────────

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            updateVoiceStatus("Voice control not available on this device");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                updateVoiceStatus("🎤 Listening... say \"next\", \"back\" or \"repeat\"");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; }

            @Override
            public void onError(int error) {
                isListening = false;
                if (!isSpeaking && binding != null) {
                    updateVoiceStatus("🎤 Say \"next\", \"back\" or \"repeat\"");
                    restartListeningDelayed();
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    handleVoiceCommand(matches);
                }
                if (!isSpeaking && binding != null) {
                    restartListeningDelayed();
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        updateVoiceStatus("🎤 Say \"next\", \"back\" or \"repeat\"");
        // Start listening right away if TTS isn't speaking yet
        restartListeningDelayed();
    }

    private void startListening() {
        if (speechRecognizer == null || isListening || isSpeaking || binding == null) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private void restartListeningDelayed() {
        if (binding == null) return;
        binding.getRoot().postDelayed(() -> {
            if (!isSpeaking && binding != null) startListening();
        }, 600);
    }

    private void handleVoiceCommand(ArrayList<String> matches) {
        if (steps == null || binding == null) return;
        for (String match : matches) {
            String lower = match.toLowerCase(Locale.ENGLISH);

            if (lower.contains("next") || lower.contains("forward")) {
                requireActivity().runOnUiThread(() -> {
                    stopSpeaking();
                    if (currentStep < steps.size() - 1) {
                        showStep(currentStep + 1);
                    } else {
                        showCookingComplete();
                    }
                });
                return;
            }

            if (lower.contains("back") || lower.contains("previous") || lower.contains("prev")) {
                requireActivity().runOnUiThread(() -> {
                    if (currentStep > 0) {
                        stopSpeaking();
                        showStep(currentStep - 1);
                    }
                });
                return;
            }

            if (lower.contains("repeat") || lower.contains("again")) {
                requireActivity().runOnUiThread(() -> speakStep(steps.get(currentStep)));
                return;
            }
        }
    }

    private void updateVoiceStatus(String message) {
        if (binding == null) return;
        requireActivity().runOnUiThread(() -> {
            if (binding != null) binding.tvVoiceStatus.setText(message);
        });
    }

    // ── Recipe loading ────────────────────────────────────────────────────────

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

    // ── Step display ──────────────────────────────────────────────────────────

    private void showStep(int index) {
        currentStep = index;
        String stepText = steps.get(index);

        binding.tvStepNumber.setText(String.valueOf(index + 1));
        binding.tvStepText.setText(stepText);

        int progress = (int) (((index + 1f) / steps.size()) * 100);
        binding.progressCooking.setProgress(progress);

        binding.btnPrevStep.setEnabled(index > 0);
        binding.btnNextStep.setText(index == steps.size() - 1 ? "Finish!" : "Next →");

        updateSpeakButton(false);
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

        binding.btnSpeak.setOnClickListener(v -> {
            if (isSpeaking) {
                stopSpeaking();
            } else {
                speakStep(stepText);
            }
        });
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.ENGLISH);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.e("TTS", "Language not supported");
            } else {
                ttsReady = true;
                tts.setSpeechRate(0.85f);
                tts.setPitch(1.0f);
            }
        } else {
            android.util.Log.e("TTS", "TTS initialization failed");
        }
    }

    private void speakStep(String text) {
        if (!ttsReady || tts == null) return;

        // Stop recognizer while TTS is speaking (avoids picking up TTS audio)
        stopListening();
        updateVoiceStatus("🔊 Speaking...");

        String announcement = "Step " + (currentStep + 1) + ". " + text;
        tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "step_" + currentStep);
        isSpeaking = true;
        updateSpeakButton(true);

        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    isSpeaking = false;
                    updateSpeakButton(false);
                    // Resume listening once TTS is done
                    if (speechRecognizer != null && binding != null) {
                        updateVoiceStatus("🎤 Say \"next\", \"back\" or \"repeat\"");
                        startListening();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    isSpeaking = false;
                    updateSpeakButton(false);
                    if (speechRecognizer != null && binding != null) {
                        updateVoiceStatus("🎤 Say \"next\", \"back\" or \"repeat\"");
                        startListening();
                    }
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
        stopListening();

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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        stopSpeaking();
        stopListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume listening when coming back, if not speaking
        if (speechRecognizer != null && !isSpeaking && steps != null) {
            restartListeningDelayed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        binding = null;
    }
}