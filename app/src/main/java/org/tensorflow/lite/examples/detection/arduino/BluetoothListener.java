package org.tensorflow.lite.examples.detection.arduino;

public interface BluetoothListener {
        static final String  BTCOMMAND_FW = "F";
        static final String  BTCOMMAND_BACK = "B";
        static final String  BTCOMMAND_RIGHT = "R";
        static final String  BTCOMMAND_LEFT = "L";
        static final String  BTCOMMAND_STOP = "S";
        static final String  BTCOMMAND_STOPALL = "D";
        static final String  BTCOMMAND_SONARSCAN = "SCAN";

        void messageReceivedFromBluetooth(String msg);

}
