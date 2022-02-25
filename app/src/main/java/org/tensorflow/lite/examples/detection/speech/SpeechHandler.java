package org.tensorflow.lite.examples.detection.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeechHandler extends UtteranceProgressListener implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    private static String TAG = SpeechHandler.class.getCanonicalName();
    private TextToSpeech tts = null;
    private Context context;
    private static SpeechHandler speechHandlerObj = null;
    public  boolean isSpeechEngineReady = false;
    private List<SpeechEventsListener> listeners = new ArrayList<SpeechEventsListener>();

    public void addListener(SpeechEventsListener toAdd) {
        listeners.add(toAdd);
    }

    private SpeechHandler(Context cxt) {
        this.context = cxt;

        this.tts =new TextToSpeech(this.context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    int result = tts.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Language not supported");
                    }else {
                        isSpeechEngineReady = true;
                        // Notify everybody that may be interested.
                        for (SpeechEventsListener sl : listeners)
                            sl.speechEngineOK();
                    }
                }else {
                    Toast.makeText(context, "Speech Engine Error", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Speech not supported");
                    isSpeechEngineReady = false;
                }
            }
        });

    }

    /**
     *
     * @return singleton SpeechHanlder object.
     */
    public static SpeechHandler getSingleObject(Context cxt) {
        if (speechHandlerObj == null) {
            speechHandlerObj = new SpeechHandler(cxt);
        }
        return speechHandlerObj;
    }

    /**
     *
     * @return singleton SpeechHanlder object. COULD BE NULL
     */
    public static SpeechHandler getSingleObject() {
        return speechHandlerObj;
    }

    public void speak(String text){

        if (!tts.isSpeaking()) {
            //Bundle b = Bundle.EMPTY;
            tts.setPitch(0.5f);
            tts.setSpeechRate(0.5f);
            tts.setOnUtteranceProgressListener(this);
            tts.setOnUtteranceCompletedListener(this);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "uniqueID");
        }
    }

    public void stopEngine(){
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        isSpeechEngineReady = false;
    }

    /**
     * Called to signal the completion of the TextToSpeech engine initialization.
     *
     * @param status {@link TextToSpeech#SUCCESS} or {@link TextToSpeech#ERROR}.
     */
    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceCompletedListener(this);

            // Notify everybody that may be interested.
            for (SpeechEventsListener sl : listeners)
                sl.speechInitiated();
        }else {
            // Notify everybody that may be interested.
            for (SpeechEventsListener sl : listeners)
                sl.speechEnded();

        }
    }

    /**
     * Called when an utterance has been synthesized.
     *
     * @param utteranceId the identifier of the utterance.
     */
    @Override
    public void onUtteranceCompleted(String utteranceId) {
        Log.i(TAG, "onUtteranceCompleted - Completed speech for utteranceId = " + utteranceId);  //utteranceId == "SOME MESSAGE"

        // Notify everybody that may be interested.
        for (SpeechEventsListener sl : listeners)
            sl.speechEnded();
    }

    /**
     * Called when an utterance "starts" as perceived by the caller. This will
     * be soon before audio is played back in the case of a {@link TextToSpeech#speak}
     * or before the first bytes of a file are written to the file system in the case
     * of {@link TextToSpeech#synthesizeToFile}.
     *
     * @param utteranceId The utterance ID of the utterance.
     */
    @Override
    public void onStart(String utteranceId) {

        // Notify everybody that may be interested.
        for (SpeechEventsListener sl : listeners)
            sl.speechInitiated();
    }

    /**
     * Called when an utterance has successfully completed processing.
     * All audio will have been played back by this point for audible output, and all
     * output will have been written to disk for file synthesis requests.
     * <p>
     * This request is guaranteed to be called after {@link #onStart(String)}.
     *
     * @param utteranceId The utterance ID of the utterance.
     */
    @Override
    public void onDone(String utteranceId) {
        Log.i(TAG, "onDone - Completed speech for utteranceId = " + utteranceId); //utteranceId == "SOME MESSAGE"

        // Notify everybody that may be interested.
        for (SpeechEventsListener sl : listeners)
            sl.speechEnded();
    }

    /**
     * Called when an error has occurred during processing. This can be called
     * at any point in the synthesis process. Note that there might be calls
     * to {@link #onStart(String)} for specified utteranceId but there will never
     * be a call to both {@link #onDone(String)} and {@link #onError(String)} for
     * the same utterance.
     *
     * @param utteranceId The utterance ID of the utterance.
     * @deprecated Use {@link #onError(String, int)} instead
     */
    @Override
    public void onError(String utteranceId) {
        // Notify everybody that may be interested.
        for (SpeechEventsListener sl : listeners)
            sl.speechEnded();

    }
}
