package org.tensorflow.lite.examples.detection.env;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class StorageHandler {

    private static final Logger LOGGER = new Logger();
    private String TAG = "StorageHandler";
    private static StorageHandler singleObject = null;
    private HashMap<String, SimilarityClassifier.Recognition> registeredFaces;
    private HashMap<String, Integer> registeredNamesAndLastInstance;
    private Context context;

    private StorageHandler(Context context) {
        this.context = context;
        registeredFaces = null;
        registeredNamesAndLastInstance = null;
        readRegisteredFacesFromStorage();
    }

    public static StorageHandler getSingleObject(Context context) {
        if (singleObject == null) {
            singleObject = new StorageHandler(context);
        }
        return singleObject;
    }

    /**
     *
     * @return StorageHandler COULD RETURN NULL
     */
    public static StorageHandler getSingleObject() {
        return singleObject;
    }


    private HashMap<String, SimilarityClassifier.Recognition> readRegisteredFacesFromStorage() {

        try {
            FileInputStream fin = this.context.openFileInput("registeredFaces.dat");

            // Wrapping our stream
            ObjectInputStream oin = new ObjectInputStream(fin);

            // Reading in our object
            registeredFaces = (HashMap)oin.readObject();
            // Closing our object stream which also closes the wrapped stream.
            oin.close();

            // write also the registeredNamesAndLastInstance catalog for keeping
            // track of registered faces (so as not to loop through the registeredFaces map all the
            // time to find the next instance to use in creating new recognitions
            fin = this.context.openFileInput("registeredNamesAndLastInstance.dat");
            oin = new ObjectInputStream(fin);
            registeredNamesAndLastInstance = (HashMap)oin.readObject();
            oin.close();

        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(this.context, "Could not read registeredFaces from storage",Toast.LENGTH_LONG);

            if (registeredFaces == null) {
                registeredFaces = new HashMap<>();
            }
            if (registeredNamesAndLastInstance == null) {
                registeredNamesAndLastInstance = new HashMap<>();
            }
        }

        return registeredFaces;
    }

    public HashMap<String, SimilarityClassifier.Recognition> getRegisteredFaces() {

        return registeredFaces;
    }

    public void setRegisteredFaces(HashMap<String, SimilarityClassifier.Recognition> registeredFaces) {
        this.registeredFaces = registeredFaces;

        // store as serialized object

        saveRegisteredFaces();
    }

    private void saveRegisteredFaces() {

        // store as serialized object

        try {
            FileOutputStream fos = this.context.openFileOutput("registeredFaces.dat", Context.MODE_PRIVATE);

            // Wrapping our file stream.
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            // Writing the serializable object to the file
            oos.writeObject(registeredFaces);

            // Closing our object stream which also closes the wrapped stream.
            oos.close();

            //the same for ...
            fos = this.context.openFileOutput("registeredNamesAndLastInstance.dat", Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(registeredNamesAndLastInstance);
            oos.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this.context, "Could not store registeredFaces",Toast.LENGTH_LONG);
        }
    }

    public HashMap<String, Integer> getRegisteredNamesAndLastInstance() {
        return registeredNamesAndLastInstance;
    }


    public void saveFaceBitmap(Bitmap faceImage, String faceName, String fileName){

        File directory = context.getDir(faceName, Context.MODE_PRIVATE);
        if (!directory.exists())
            directory.mkdir();

        File file = new File(directory, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            faceImage.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
