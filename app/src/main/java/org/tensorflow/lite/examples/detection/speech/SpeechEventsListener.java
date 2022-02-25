package org.tensorflow.lite.examples.detection.speech;

public interface SpeechEventsListener {
    void speechEngineOK();
    void speechInitiated();
    void speechEnded();
}
