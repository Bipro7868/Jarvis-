package com.biprojit.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private TextView status;

    private boolean speaking = false;
    private boolean listening = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = new TextView(this);
        status.setText("JARVIS\n\nStarting...");
        status.setTextSize(24);
        status.setGravity(Gravity.CENTER);
        setContentView(status);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    100
            );
        }

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);

                handler.postDelayed(() -> {
                    speak("Hello, I am Jarvis.");
                }, 1000);
            }
        });

        setupSpeechRecognizer();
    }

    private void setupSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is not available.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        listening = true;
                        status.setText("JARVIS\n\nListening...");
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        status.setText("JARVIS\n\nVoice detected...");
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        listening = false;
                        status.setText("JARVIS\n\nProcessing...");
                    }

                    @Override
                    public void onError(int error) {
                        listening = false;

                        handler.postDelayed(() -> {
                            startListening();
                        }, 500);
                    }

                    @Override
                    public void onResults(Bundle results) {

                        listening = false;

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (matches == null || matches.size() == 0) {
                            startListening();
                            return;
                        }

                        String text = matches.get(0);

                        status.setText(
                                "You said:\n\n" + text
                        );

                        processSpeech(text);
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {
                    }
                }
        );
    }

    private void startListening() {

        if (speaking) {
            return;
        }

        if (speechRecognizer == null) {
            return;
        }

        handler.postDelayed(() -> {

            try {
                speechRecognizer.startListening(speechIntent);
            } catch (Exception e) {
                handler.postDelayed(
                        this::startListening,
                        1000
                );
            }

        }, 300);
    }

    private void processSpeech(String text) {

        String command = text.toLowerCase(Locale.US).trim();

        /*
         * JARVIS WAKE WORD
         *
         * If "jarvis" is NOT present:
         * ignore the sentence and return to idle/listening.
         */

        if (!command.contains("jarvis")) {
            startListening();
            return;
        }

        /*
         * Remove the wake word.
         */

        command = command.replace("jarvis", "").trim();

        /*
         * Empty command:
         * User only said "Jarvis".
         */

        if (command.isEmpty()) {
            speak("Yes?");
            return;
        }

        /*
         * YOUTUBE
         */

        if (command.contains("open youtube")
                || command.equals("youtube")
                || command.contains("open you tube")) {

            speak("Opening YouTube.");

            handler.postDelayed(() -> {

                try {
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                    "https://www.youtube.com"
                            )
                    );

                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(
                            this,
                            "Unable to open YouTube",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                handler.postDelayed(
                        this::startListening,
                        1500
                );

            }, 1200);

            return;
        }

        /*
         * HELLO
         */

        if (command.contains("hello")
                || command.contains("hi")) {

            speak("Hello. How can I help you?");
            return;
        }

        /*
         * HOW ARE YOU
         */

        if (command.contains("how are you")) {

            speak("I am working perfectly.");
            return;
        }

        /*
         * STOP
         */

        if (command.contains("stop listening")
                || command.equals("stop")
                || command.contains("go to sleep")) {

            speak("Going idle.");

            handler.postDelayed(() -> {
                status.setText(
                        "JARVIS\n\nIdle"
                );
            }, 1500);

            return;
        }

        /*
         * EXIT
         */

        if (command.contains("exit jarvis")
                || command.contains("close jarvis")) {

            speak("Goodbye.");

            handler.postDelayed(() -> {
                finish();
            }, 1500);

            return;
        }

        /*
         * UNKNOWN COMMAND
         */

        speak("I heard you, but I don't know that command yet.");
    }

    private void speak(String text) {

        if (tts == null) {
            return;
        }

        speaking = true;

        status.setText(
                "JARVIS\n\n" + text
        );

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS"
        );

        /*
         * Give Android TTS enough time to finish.
         * Then return to the listening cycle.
         */

        long delay = Math.max(
                1500,
                text.length() * 70L
        );

        handler.postDelayed(() -> {

            speaking = false;
            startListening();

        }, delay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed(() -> {
            if (!speaking) {
                startListening();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
