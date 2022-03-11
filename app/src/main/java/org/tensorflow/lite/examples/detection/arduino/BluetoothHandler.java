package org.tensorflow.lite.examples.detection.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import android.util.Log;

import org.tensorflow.lite.examples.detection.DetectionListener;
import org.tensorflow.lite.examples.detection.env.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothHandler {

    private String deviceName = null;
    private String deviceAddress = null;

    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update


    private static final Logger LOGGER = new Logger();
    private String TAG = "BluetoothHandler";
    private static BluetoothHandler singleObject = null;
    private Context context;

    public static List<BluetoothListener> listeners;

    private BluetoothHandler(Context context) {
        this.context = context;
        deviceName = "HC-06";
        deviceAddress = "98:D3:61:F9:48:80";

         /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
        createConnectThread.start();

    }

    public static BluetoothHandler getSingleObject(Context context) {
        if (singleObject == null) {
            singleObject = new BluetoothHandler(context);
        }
        return singleObject;
    }

    /**
     *
     * @return StorageHandler COULD RETURN NULL
     */
    public static BluetoothHandler getSingleObject() {
        return singleObject;
    }


    public static void addListener(BluetoothListener toAdd) {
        if (listeners == null){
            listeners = new ArrayList<BluetoothListener>();
        }
        listeners.add(toAdd);
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    private static class CreateConnectThread extends Thread {

        private final String TAG = "CreateConnectThread";

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.i(TAG, "Bluetooth Device connected");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e(TAG, "Cannot connect to Bluetooth device");

                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the Bluetooth client socket : " + closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the Bluetooth client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    private static class ConnectedThread extends Thread {

        private final String TAG = "ConnectedThread";

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.i(TAG,"Arduino Message received : " + readMessage);
                        // notify all listeners
                        for(BluetoothListener bll: listeners){
                            bll.messageReceivedFromBluetooth(readMessage);
                        }
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG,"Unable to send message to arduino",e);
            }
        }

        public void write(char input) {
            byte[] bytes = new byte[1];
            bytes[0] = (byte) input; //converts entered char into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG,"Unable to send message to arduino",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

        public void initialMotorTest(){
            write('F');
            write('\n');
            write('B');
            write('\n');
            write('L');
            write('\n');
            write('R');
            write('\n');
            write('S');
            write('\n');

        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */

    public void terminateConnection() {
        // Terminate Bluetooth Connection
        if (createConnectThread != null){
            createConnectThread.cancel();
        }

    }

    public void sendToArduino(String msg){
        // Send command to Arduino board
        connectedThread.write(msg);
    }
}
