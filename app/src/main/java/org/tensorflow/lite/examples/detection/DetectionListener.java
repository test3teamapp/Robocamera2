package org.tensorflow.lite.examples.detection;

import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier;

public interface DetectionListener {
     void detectorFoundAFace(SimilarityClassifier.Recognition result);
}
