package org.tensorflow.lite.examples.detection.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListeningHandler implements RecognitionListener {

    private static String TAG = ListeningHandler.class.getCanonicalName();
    private SpeechRecognizer speechRecognizer = null;
    private Context context;
    private static ListeningHandler listeningHandlerObj = null;
    public boolean isSpeechRecogniserReady = false;
    private List<ListeningEventsListener> listeners = new ArrayList<ListeningEventsListener>();

    public void addListener(ListeningEventsListener toAdd) {
        listeners.add(toAdd);
    }

    private ListeningHandler(Context cxt) {
        this.context = cxt;

        if (SpeechRecognizer.isRecognitionAvailable(cxt)) {
            Log.i(TAG, "SpeechRecognizer.isRecognitionAvailable - true");
            this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(cxt);
            this.speechRecognizer.setRecognitionListener(this);
        } else {
            Log.i(TAG, "SpeechRecognizer.isRecognitionAvailable - false");
        }

    }

    /**
     * @return singleton SpeechHanlder object.
     */
    public static ListeningHandler getSingleObject(Context cxt) {
        if (listeningHandlerObj == null) {
            listeningHandlerObj = new ListeningHandler(cxt);
        }
        return listeningHandlerObj;
    }

    /**
     * @return singleton SpeechHanlder object. COULD BE NULL
     */
    public static ListeningHandler getSingleObject() {
        return listeningHandlerObj;
    }


    public void stopEngine() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
        isSpeechRecogniserReady = false;
    }

    public boolean startListening() {

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }

        // speech recognition must run on the main thread
        Handler mainHandler = new Handler(this.context.getMainLooper());
        Runnable listenningRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "startListening");
                speechRecognizer.startListening(speechRecognizerIntent);
            }
        };
        mainHandler.post(listenningRunnable);
        return true;

    }


    /**
     * Called when the endpointer is ready for the user to start speaking.
     *
     * @param params parameters set by the recognition service. Reserved for future use.
     */
    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(TAG, "onReadyForSpeech - Now it is ready to listen");
        isSpeechRecogniserReady = true;
    }

    /**
     * The user has started to speak.
     */
    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech - The user has started to speak");
    }

    /**
     * The sound level in the audio stream has changed. There is no guarantee that this method will
     * be called.
     *
     * @param rmsdB the new RMS dB value
     */
    @Override
    public void onRmsChanged(float rmsdB) {

    }

    /**
     * More sound has been received. The purpose of this function is to allow giving feedback to the
     * user regarding the captured audio. There is no guarantee that this method will be called.
     *
     * @param buffer a buffer containing a sequence of big-endian 16-bit integers representing a
     *               single channel audio stream. The sample rate is implementation dependent.
     */
    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    /**
     * Called after the user stops speaking.
     */
    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech");
    }

    /**
     * A network or recognition error occurred.
     *
     * @param error code is defined in {@link SpeechRecognizer}
     */
    @Override
    public void onError(int error) {
        Log.i(TAG, "onError - " + error);
        isSpeechRecogniserReady = false;

    }

    /**
     * Called when recognition results are ready.
     *
     * @param results the recognition results. To retrieve the results in {@code
     *                ArrayList<String>} format use {@link Bundle#getStringArrayList(String)} with
     *                {@link SpeechRecognizer#RESULTS_RECOGNITION} as a parameter. A float array of
     *                confidence values might also be given in {@link SpeechRecognizer#CONFIDENCE_SCORES}.
     */
    @Override
    public void onResults(Bundle results) {

        if (results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) != null) {
            // Notify everybody that may be interested.
            for (ListeningEventsListener ll : listeners) {
                ll.listeningResults(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            }
        }

    }

    /**
     * Called when partial recognition results are available. The callback might be called at any
     * time between {@link #onBeginningOfSpeech()} and {@link #onResults(Bundle)} when partial
     * results are ready. This method may be called zero, one or multiple times for each call to
     * {@link SpeechRecognizer#startListening(Intent)}, depending on the speech recognition
     * service implementation.  To request partial results, use
     * {@link RecognizerIntent#EXTRA_PARTIAL_RESULTS}
     *
     * @param partialResults the returned results. To retrieve the results in
     *                       ArrayList&lt;String&gt; format use {@link Bundle#getStringArrayList(String)} with
     *                       {@link SpeechRecognizer#RESULTS_RECOGNITION} as a parameter
     */
    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    /**
     * Reserved for adding future events.
     *
     * @param eventType the type of the occurred event
     * @param params    a Bundle containing the passed parameters
     */
    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
