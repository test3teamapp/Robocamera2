package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.tensorflow.lite.examples.detection.arduino.BluetoothHandler;
import org.tensorflow.lite.examples.detection.arduino.RoboBrain;
import org.tensorflow.lite.examples.detection.env.StorageHandler;
import org.tensorflow.lite.examples.detection.speech.ListeningEventsListener;
import org.tensorflow.lite.examples.detection.speech.ListeningHandler;
import org.tensorflow.lite.examples.detection.speech.SpeechEventsListener;
import org.tensorflow.lite.examples.detection.speech.SpeechHandler;
import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier;

import java.util.ArrayList;
import java.util.Collections;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RobotFaceActivity extends AppCompatActivity implements  SpeechEventsListener, ListeningEventsListener {

    private static String TAG = RobotFaceActivity.class.getCanonicalName();

    private static final int PERMISSIONS_REQUEST = 2;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_AUDIO = Manifest.permission.RECORD_AUDIO;


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private AnimationDrawable frameAnimation;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
//                mContentView.getWindowInsetsController().hide(
//                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    private boolean permissionsGranded = false;
    private boolean canSpeak = false;
    private boolean canListen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_robot_face);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToSpeak("Welcome ! - Do you want me to find anyone? ");
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // set the robotface animation on the imageview

        // Setting animation_list.xml as the background of the image view
        mContentView.setBackgroundResource(R.drawable.robo_talk_animation);

        // Typecasting the Animation Drawable
        frameAnimation = (AnimationDrawable) mContentView.getBackground();

        if (hasPermission()) {
            permissionsGranded = true;
        } else {
            requestPermission();
        }

        // READ DB WITH FACES
        // WILL INITIALISE THE SINGLETON OBJECT AND LOAD THE FILE IF IT EXISTS
        StorageHandler.getSingleObject(this);
        // SpeechHandler initiation
        SpeechHandler.getSingleObject(this);
        // listen to TTS events
        SpeechHandler.getSingleObject().addListener(this);
        // Listening handler initiation
        ListeningHandler.getSingleObject(this);
        // listen to SpeechToText events
        ListeningHandler.getSingleObject().addListener(this);
        // create robotBrain object
        RoboBrain.getSingleObject(this);
        // create bluetooth controller
        BluetoothHandler.getSingleObject(this);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpeechHandler.getSingleObject().stopEngine();
        ListeningHandler.getSingleObject().stopEngine();
        BluetoothHandler.getSingleObject().terminateConnection();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
//            mContentView.getWindowInsetsController().show(
//                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // Called when Activity becomes visible or invisible to the user
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            tryToSpeak("Welcome ! - Do you want me to find anyone? ");
        } else {
            // Stopping the animation when not in Focus
            frameAnimation.stop();
        }
    }

    private void tryToSpeak(String prompt) {
        if (permissionsGranded && canSpeak) {
            SpeechHandler.getSingleObject().speak(prompt);
            // Starting the animation
            frameAnimation.start();
        }

    }




    @Override
    public void speechEngineOK() {
        canSpeak = true;
    }

    @Override
    public void speechInitiated() {

    }

    @Override
    public void speechEnded() {
        // Stopping the animation
        frameAnimation.stop();
        // start listening
        ListeningHandler.getSingleObject().startListening();
    }

    // PERMISSIONS

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                permissionsGranded = true;
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_AUDIO) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        RobotFaceActivity.this,
                        "Camera permission is required",
                        Toast.LENGTH_LONG)
                        .show();
            }
            if (shouldShowRequestPermissionRationale(PERMISSION_AUDIO)) {
                Toast.makeText(
                        RobotFaceActivity.this,
                        "Audio listening permission is required",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_AUDIO}, PERMISSIONS_REQUEST);
        }
    }

    // Speech Recognition events

    @Override
    public void listeningEngineOK() {
        canListen = true;
    }

    @Override
    public void listeningResults(ArrayList<String> results) {

        Log.i(TAG, "onResults - " + results.get(0));

        RoboBrain.getSingleObject().processAudioCommand(results.get(0));


    }
}