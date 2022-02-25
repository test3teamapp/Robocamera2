package org.tensorflow.lite.examples.detection.speech;

import java.util.ArrayList;

public interface ListeningEventsListener {
    void listeningEngineOK();
    void listeningResults(ArrayList<String> results);
}
