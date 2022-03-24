package org.tensorflow.lite.examples.detection.arduino;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.DetectionListener;
import org.tensorflow.lite.examples.detection.DetectorActivity;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.env.StorageHandler;
import org.tensorflow.lite.examples.detection.speech.ListeningHandler;
import org.tensorflow.lite.examples.detection.speech.SpeechHandler;
import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier;

public class RoboBrain implements DetectionListener, BluetoothListener {

    private static final Logger LOGGER = new Logger();
    private String TAG = "RoboBrain";
    private static RoboBrain singleObject = null;
    private Context context;

    private String personToLookForString = "";
    private String spokenCommandString = "";
    private boolean shouldFindOrFollow = false;

    private RoboBrain(Context context) {
        this.context = context;
        DetectorActivity.addListener(this);
        BluetoothHandler.addListener(this);

    }

    public static RoboBrain getSingleObject(Context context) {
        if (singleObject == null) {
            singleObject = new RoboBrain(context);
        }
        return singleObject;
    }

    /**
     * @return StorageHandler COULD RETURN NULL
     */
    public static RoboBrain getSingleObject() {
        return singleObject;
    }


    private void resetTrackingData() {
        personToLookForString = "";
        spokenCommandString = "";
        shouldFindOrFollow = false;
    }

    public void processAudioCommand(String audioTranscript) {

        boolean needToStartCamera = false;
        resetTrackingData();

        if (audioTranscript.contains("find")) {
            spokenCommandString = "find";
            shouldFindOrFollow = true;
            needToStartCamera = true;
        } else if (audioTranscript.contains("follow")) {
            spokenCommandString = "follow";
            shouldFindOrFollow = true;
            needToStartCamera = true;
        } else if (audioTranscript.contains("drive") || audioTranscript.contains("Drive")) {
            spokenCommandString = "drive";
        } else if (audioTranscript.contains("stop") || audioTranscript.contains("Stop")) {
            spokenCommandString = "stop";
        } else if (audioTranscript.contains("scan") || audioTranscript.contains("Scan")) {
            spokenCommandString = "scan";
        } else if (audioTranscript.contains("test") || audioTranscript.contains("Test")) {
            spokenCommandString = "test";
        }
        if (shouldFindOrFollow) {
            if (StorageHandler.getSingleObject().getRegisteredFaces().keySet() != null &&
                    StorageHandler.getSingleObject().getRegisteredFaces().keySet().size() != 0) {
                int bestScore = 3; // best score can be 0 if string are identical.
                // 1,2,3,4,5...etc are scores for less than identical strings
                // higher "similarity distance"
                // we set a threashold of 3.
                // match a name.
                // TODO NOT VERY SMART MATCHING. THE NAME MUST BE AFTER THE VERB "FIND" or "FOLLOW"
                String currentNameFromListeningResults = audioTranscript.split(spokenCommandString)[1];
                for (String name : StorageHandler.getSingleObject().getRegisteredFaces().keySet()) {
                    if (StringUtils.getLevenshteinDistance(name, currentNameFromListeningResults) < bestScore) {
                        bestScore = StringUtils.getLevenshteinDistance(name, currentNameFromListeningResults);
                        personToLookForString = name;
                    }
                }
            }
        }

        Log.i(TAG, "onResults - I was ordered to : '" + spokenCommandString + "' '" + personToLookForString + "'");

        if (needToStartCamera) {
            Intent intent = new Intent(context, DetectorActivity.class);
            context.startActivity(intent);
        }

        if (spokenCommandString.equals("drive")) {
            //TODO
            BluetoothHandler.getSingleObject().sendToArduino(BTCOMMAND_FW);
        }
        if (spokenCommandString.equals("stop")) {
            //TODO
            BluetoothHandler.getSingleObject().sendToArduino(BTCOMMAND_STOPALL);
        }
        if (spokenCommandString.equals("scan")) {
            //TODO
            BluetoothHandler.getSingleObject().sendToArduino(BTCOMMAND_SONARSCAN);
        }
        if (spokenCommandString.equals("test")) {
            //TODO
            BluetoothHandler.getSingleObject().initialMotorTest();
        }


        // start listening -- Next command could be e.g. STOP
        ListeningHandler.getSingleObject().startListening();
    }

    @Override
    public void detectorFoundAFace(SimilarityClassifier.Recognition result) {
        // WITH OLDER PHONES CAMERA SHOULD BE PAUSED FOR THE TTS TO WORK DUE TO CPU POWER


        if (result.getTitle().compareTo(this.personToLookForString) == 0) {
            // we found what we were looking for
            // close the camera activity
            Intent intent = new Intent(CameraActivity.CAMERA_ACTIVITY_BROADCAST_MESSAGE_FINNISH_CAMERA);
            context.sendBroadcast(intent);
            SpeechHandler.getSingleObject().speak("I found you " + result.getTitle() + "! ");
        }
    }


    @Override
    public void messageReceivedFromBluetooth(String msg) {

    }
}
