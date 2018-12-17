package ucla_sensing.com.wearablesensor;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class AcceptThread extends Thread {

    private final String TAG = "DBG-AcceptThread";

    private final BluetoothServerSocket mmServerSocket;
    private BluetoothAdapter mBluetoothAdapter;

    readBluetoothThread btThread = null;

    private Context mCTX = null;


    //This is the thread for accepting bluetooth connections, and spawns the readBluetoothThread
    // for reading data from the bluetooth socket.
    public AcceptThread(BluetoothAdapter btAdapter, UUID uuid, Context ctx) {
        mCTX = ctx;
        mBluetoothAdapter = btAdapter;
        Log.d(TAG, "Creating Server Thread for reading!");
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BT_SERVER", uuid);
        } catch (IOException e) {
            Log.d(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void beginSavingData() {
        btThread.beginSavingData();
    }

    private void sendConnectionStatus() {
        Intent in = new Intent();
        in.setAction("DEVICE_CONNECTED");
        LocalBroadcastManager.getInstance(mCTX).sendBroadcast(in);
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();

            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {

                Log.d(TAG, "Server accepted Connection!");
                sendConnectionStatus();
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                if(btThread == null) {
                    btThread = new readBluetoothThread(socket);
                    btThread.run();
                }


                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.d(TAG, "SOCKET COULD NOT CLOSE");
                    e.printStackTrace();
                }
                Log.d(TAG, "Stopping Accept Thread!");
                break;
            }

        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            Log.d(TAG, "Closing connection thread and interrupting reading thread");
            sendConnectionStatus();
            mmServerSocket.close();
            btThread.cancel();
            btThread.interrupt();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }


    /*
           This is the thread for pulling data from the bluetooth buffer.  It will use the
           collectBluetoothData thread to parse the data.
     */
    private class readBluetoothThread extends Thread {

        BluetoothSocket btSock;
        InputStream inStream;

        boolean stop = false;
        collectBluetoothData ctdThread = null;

        readBluetoothThread(BluetoothSocket bsock) {
            btSock = bsock;
        }

        public void beginSavingData() {
            ctdThread.beginSavingData();
        }

        public void run() {
            final int BUFFER_SIZE = 2048;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes = 0;
            int b = BUFFER_SIZE;

            ctdThread = new collectBluetoothData();
            ctdThread.run();

            long lastSamplingTime = 0;
            long samplingDelay = 1000; //1 second
            int numSamples = 0;

            Log.d(TAG, "Listening for Bluetooth streams");
            while (true) {

                if(stop) {
                    Log.d(TAG, "Stopping Thread Loop!");
                    break;
                }

                try {
                    inStream = btSock.getInputStream();
                } catch (IOException e) {
                    Log.d(TAG, "IO EXCEPTION");
                    break;
                }

                try {
                    /*if(System.currentTimeMillis() > lastSamplingTime + samplingDelay ) {
                        lastSamplingTime = System.currentTimeMillis();
                        Log.d(TAG, "Number of samples/Second " + numSamples);
                        numSamples = 0;
                    }*/
                    if(!btSock.isConnected()) {
                        break;
                    }
                    bytes = inStream.read(buffer, 0, buffer.length);
                    if(bytes == -1) {
                        break;
                    }
                    ctdThread.parseString(buffer, bytes);

                    //Log.d(TAG, "Num bytes: " + bytes);

                    //String metadata = fixedOutput.substring(fixedOutput.indexOf("["), fixedOutput.indexOf("]"));
                    //Log.d(TAG, metadata);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() throws IOException {
            Log.d(TAG, "Closing Reading Thread");
            btSock.close();
            stop = true;
        }
    }



    /*
        This is the thread for tracking and saving Bluetooth Data.
        Data transmitted is in the following packet form:

        [number accelerometer samples, number gyroscope samples, number gravity samples]
        {
            SENSOR_NAME:
            {
                timestamp:timemilliseconds,
                values:[value1, value2, value3],       //These correspond to X,Y,Z values
            }
        }^          //Yes, I used ^ as the separator to simplify things for separation
        {
            SENSOR_NAME:
            {
                timestamp:timemilliseconds,
                values:[value1, value2, value3],       //These correspond to X,Y,Z values
            }
        },
        ....

        SENSOR_NAME could be ACCELEROMETER, GYROSCOPE, or GRAVITY
        timemilliseconds is just the timestamp in milliseconds
        value1, value2, value3 are floating point values.

     */
    private class collectBluetoothData extends Thread {

        String tempString = "";
        String dataString = "";
        String backupDataString = "";
        boolean continuedString = false;
        long timeLastSampled = 0;
        final long samplingDelay = 1000;
        int numAccelSamples = 0;
        int numGyroSamples = 0;
        int numGravSamples = 0;
        int numLinearAccSamples = 0;

        int numberDroppedPackets = 0;
        int recoveredPackets = 0;
        boolean savingData = false;

        dataPackager dpackager = new dataPackager();

        public void collectBluetoothData() {
            dpackager.start();
        }

        public void beginSavingData() {
            savingData = true;
        }

        private void addMotionSamples(String vals) {
            int accelSamples = Character.getNumericValue(vals.charAt(1));
            int gyroSamples = Character.getNumericValue(vals.charAt(3));
            int gravSamples = Character.getNumericValue(vals.charAt(5));
            int linAccSamples = Character.getNumericValue(vals.charAt(7));
            //Log.d(TAG, vals + " : " + accelSamples + "," + gyroSamples + "," + gravSamples);
            numAccelSamples += accelSamples;
            numGyroSamples  += gyroSamples;
            numGravSamples += gravSamples;
            numLinearAccSamples += linAccSamples;
        }

        private void sendDataReceived(String msg) {
            Intent in = new Intent();
            in.setAction("INFO_RECEIVED");
            in.putExtra("data", msg);
            LocalBroadcastManager.getInstance(mCTX).sendBroadcast(in);
        }

        public void parseString(byte[] buffer, int bytes) throws UnsupportedEncodingException {
            String message = new String(buffer, "UTF-8");
            String fixedOutput = message.substring(0, bytes);

            Log.d(TAG, "Packet: " + fixedOutput);
            boolean isPerfect = false;

            //This is a perfect packet - no need to do extra processing
            if(fixedOutput.charAt(0) == '*' && fixedOutput.charAt(fixedOutput.length()-1) == '*' ) {
                dataString = fixedOutput;
                isPerfect = true;
            }
            else {

                /*
                    Here are some Cases:
                    A:  *...**....  contains a perfect packet, but also a broken packet
                    B:  ...**...**...  contains a continued broken packet, a perfect packet, and a broken packet
                    C:  ...**...* contains a continued broken packet, and a perfect packet
                    D:  ...*    contains a continued broken packet
                    E:  *...   contains a broken packet
                    F:  ...**... contains a continued broken packet, and a broken packet
                 */

                //We could have case A B or C
                if(fixedOutput.contains("**")) {

                    int endIndicator = fixedOutput.indexOf("**", 1);

                    //Case A
                    if(fixedOutput.charAt(0) == '*' && endIndicator != -1) {
                        dataString = fixedOutput.substring(0, endIndicator);  //Retrieve the perfect packet
                        tempString = fixedOutput.substring(endIndicator);
                        continuedString = true;
                    }
                    //Case B and C
                    else if(fixedOutput.charAt(0) != '*' && endIndicator != -1 && continuedString) {

                       int secondEndIndicator = fixedOutput.indexOf("**", endIndicator);

                       //There's a second set of **, implying case B
                       if(secondEndIndicator != -1) {
                            dataString = tempString + fixedOutput.substring(0, endIndicator);
                            backupDataString = fixedOutput.substring(endIndicator, secondEndIndicator);
                            tempString = fixedOutput.substring(secondEndIndicator);
                       }
                       //There isn't a second set of **, but there is an ending indicator, implying case C
                       else if( (fixedOutput.charAt(fixedOutput.length()-1)) == '*') {
                           dataString = tempString + fixedOutput.substring(0, endIndicator);
                           backupDataString = fixedOutput.substring(endIndicator, fixedOutput.length()-1);
                           continuedString = false;
                       }
                       else {
                           Log.d(TAG, "Missed Case1!");
                       }
                    }
                    else {
                        Log.d(TAG, "continued: " + continuedString);
                        Log.d(TAG, "2: " + fixedOutput);
                        //Log.d(TAG, "Missed Case2!");
                    }


                }
                //We could have case D or E
                else if(fixedOutput.contains("*")) {

                    int endIndicator = fixedOutput.indexOf('*', 1);
                    //Case E - we have a broken packet
                    if(fixedOutput.charAt(0) == '*' && endIndicator == -1) {
                        tempString = fixedOutput;
                        continuedString = true;
                    }
                    //Case D - we have a continued broken packet
                    else if(continuedString && fixedOutput.charAt(fixedOutput.length()-1) == '*') {
                        String remaining = fixedOutput.substring(0, fixedOutput.indexOf("*"));
                        dataString = tempString + remaining;
                        continuedString = false;
                    }
                    else {
                        Log.d(TAG, "3 continued: " + continuedString);
                        Log.d(TAG, "3: " + fixedOutput);
                        //Log.d(TAG, "Missed Case3!");
                    }


                }
                else {
                    numberDroppedPackets++;
                    Log.d(TAG, "Dropped: " + fixedOutput);
                }

            }

            //Parse the data somewhat - remove the "*" from the beginning and end
            dataString = dataString.replaceAll("\\*", "");
            backupDataString = backupDataString.replaceAll("\\*", "");

            if(!isPerfect) {
                if(dataString.length() > 10) {
                    String vals = dataString.substring(0,9);
                    addMotionSamples(vals);

                    //Log.d(TAG, vals);
                    if(backupDataString.length() > 9) {
                        //Log.d(TAG, backupDataString);
                        vals = backupDataString.substring(1,10);
                        addMotionSamples(vals);
                        //Log.d(TAG, vals);
                    }
                }
            }
            else {
                if(dataString.length() > 10) {
                    String vals = dataString.substring(0, 9);
                    addMotionSamples(vals);
                }
            }


            if(timeLastSampled + samplingDelay < System.currentTimeMillis() ) {  //If sufficient delay passed since the last time we output a sampling rate


                String output = "Number of samples: " + numAccelSamples + " Accelerometer, " + numGyroSamples + " Gyro, " + numGravSamples +
                        " Grav, " + numLinearAccSamples + " Linear acc";
                Log.d(TAG, output);
                sendDataReceived(output);

                numAccelSamples = 0;
                numGyroSamples = 0;
                numGravSamples = 0;
                numLinearAccSamples = 0;
                timeLastSampled = System.currentTimeMillis();

                Log.d(TAG, "Dropped " + numberDroppedPackets);




                numberDroppedPackets = 0;
            }

            /*try {
                dpackager.convertPacketToJSON(dataString);
            } catch (JSONException e) {
                Log.d(TAG, "BAD CONVERSION FROM STRING TO JSON");
                e.printStackTrace();
            }*/
            if(savingData) {
                dpackager.directWriteToFile(dataString);
            }


            //Reset the Datastrings and backup strings
            dataString = "";
            backupDataString = "";

        }

    }


}