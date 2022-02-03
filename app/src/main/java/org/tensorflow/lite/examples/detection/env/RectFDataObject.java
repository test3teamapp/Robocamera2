package org.tensorflow.lite.examples.detection.env;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class RectFDataObject implements Serializable {

    private RectF currentRectF;

    public RectFDataObject(RectF rectF)
    {
        currentRectF = rectF;
    }

    public RectF getcurrentRectF() {
        return currentRectF;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        byte[] byteArray = currentRectF.toString().getBytes(StandardCharsets.UTF_8);

        out.writeInt(byteArray.length);
        out.write(byteArray);

    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {


        int bufferLength = in.readInt();

        byte[] byteArray = new byte[bufferLength];

        int pos = 0;
        do {
            int read = in.read(byteArray, pos, bufferLength - pos);

            if (read != -1) {
                pos += read;
            } else {
                break;
            }

        } while (pos < bufferLength);

        String strRectF = new String(byteArray,StandardCharsets.UTF_8);
        // this has the form :
        //  "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")"
        strRectF = strRectF.split("\\(")[1];
        strRectF = strRectF.split("\\)")[0];
        float left = Float.parseFloat(strRectF.split(",")[0]);
        float top = Float.parseFloat(strRectF.split(",")[1]);
        float right = Float.parseFloat(strRectF.split(",")[2]);
        float bottom = Float.parseFloat(strRectF.split(",")[3]);
        this.currentRectF = new RectF(left,top,right,bottom);

    }
}
