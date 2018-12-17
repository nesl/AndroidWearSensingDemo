package ucla_sensing.com.wearablesensor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class sensingService extends Service implements SensorEventListener {

    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;
    // 3 = @Deprecated Orientation
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;

    // 7 = @Deprecated Temperature
    private final static int SENS_GRAVITY = Sensor.TYPE_GRAVITY;
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    private final static int SENS_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;

    SensorManager mSensorManager;

    private boolean beginStreaming = false;

    private long lastTimeStartMillisec = 0;
    private int numAccelSamples = 0;
    private int numGyroSamples = 0;
    private int numGravSamples = 0;
    private int numLinearAccSamples = 0;
    private int numMagSamples = 0;

    private long lastTimeSampledAcc = 0;
    private long lastTimeSampledGyro = 0;
    private long lastTimeSampledGrav = 0;
    private long lastTimeSampledLinearAcc = 0;
    private long throttle_delay = 8;  //At least 9 milliseconds must pass before sending data

    private String mCurrentBuffer = "";
    private long mCurrentBufferBytes = 0;
    private long BUFFER_LIMIT = 2000;  //Maximum 500 bytes per transmitted 'packet'

    private String uuidStr = "71b37966-2466-45b7-ae2c-f42851fcac8e";

    private BluetoothSocket btSocket;

    private static final String TAG = "DBG-sensingService";


    private OutputStream outputStream;

    //Initiates the Bluetooth connection by connecting via RFCOMM to the connected Bluetooth device i.e. smartphone
    private void initBluetooth() throws IOException {
        Log.d(TAG, "Initiating Bluetooth Connection...");
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled()) {

                Set<BluetoothDevice> bondedDevices = blueAdapter.getBondedDevices();

                if(bondedDevices.size() > 0) {
                    Object[] devices = (Object []) bondedDevices.toArray();
                    BluetoothDevice device = (BluetoothDevice) devices[0]; //Probably only connected to one device


                    ParcelUuid[] uuids = device.getUuids();

                    btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuidStr));
                    btSocket.connect();
                    if(btSocket.isConnected()) {
                        Log.d(TAG, "Socket connected!");
                    }
                    else {
                        Log.d(TAG, "Socket is not connnected!");
                    }
                    beginStreaming = true;
                    outputStream = btSocket.getOutputStream();

                }

                Log.e("error", "No appropriate paired devices.");
            } else {
                Log.e("error", "Bluetooth is disabled.");
            }
        }
    }


    //This accumulates data to a packet, and sends it to writeToDevice
    public void writeBufferToDevice(String s) throws IOException {

        long numBytes = s.getBytes().length;
        String tempBuffer = mCurrentBuffer;

        //If we go over the buffer limit by adding this string, we send what we have
        if(mCurrentBufferBytes + numBytes > BUFFER_LIMIT) {
            writeToDevice(tempBuffer);
            mCurrentBuffer = s;
            mCurrentBufferBytes = numBytes;
        }
        else {  //We are under the buffer limit, so we just keep adding to the buffer.
            mCurrentBuffer += s;
            mCurrentBufferBytes += numBytes;
        }


    }

    //After the string has been formatted, it transmits to the bluetooth device.
    //This sets the string to have specific formatting - mainly the formatting is like this:
    //
    //  *[#accelerometer samples in this packet, #gyroscope samples in this packet, #gravity samples in this packet]
    //   { SENSOR_NAME:
    //        {
    //            timestamp:timemilliseconds,
    //            values:[value1, value2, value3],       //These correspond to X,Y,Z values
    //        }
    //   }^          //Yes, I used ^ as the separator to simplify things for separation
    //   { SENSOR_NAME:
    //        {
    //            timestamp:timemilliseconds,
    //            values:[value1, value2, value3],       //These correspond to X,Y,Z values
    //        }
    //   }...*
    public void writeToDevice(String s) throws IOException {

        //Add additional metadata about the current number of samples in this transmission.
        s = "*[" + numAccelSamples + "," + numGyroSamples + "," + numGravSamples + "," + numLinearAccSamples + "]" + s + "*";
        //* signifies the start of an entry
        //Log.d(TAG, "Writing to device: " + s);
        outputStream.write(s.getBytes());

        //Reset the counts.
        numAccelSamples = 0;
        numGyroSamples = 0;
        numGravSamples = 0;
        numLinearAccSamples = 0;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        //Being tracking the sensor data
        startMeasurement();

        try {
            initBluetooth();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            stopMeasurement(); //Stop tracking sensor data
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //Register listeners for sensor data.
    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        if (true) {
            logAvailableSensors();
        }

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(SENS_ACCELEROMETER);

        Sensor gravitySensor = mSensorManager.getDefaultSensor(SENS_GRAVITY);
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor(SENS_GYROSCOPE);

        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor(SENS_LINEAR_ACCELERATION);
        Sensor magneticFieldSensor = mSensorManager.getDefaultSensor(SENS_MAGNETIC_FIELD);
        Sensor rotationVectorSensor = mSensorManager.getDefaultSensor(SENS_ROTATION_VECTOR);


        // Register the listener
        if (mSensorManager != null) {
            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
                lastTimeStartMillisec = System.currentTimeMillis();
            } else {
                Log.w(TAG, "No Accelerometer found");
            }

            if (gravitySensor != null) {
                mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Log.w(TAG, "No Gravity Sensor");
            }

            if (gyroscopeSensor != null) {
                mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Log.w(TAG, "No Gyroscope Sensor found");
            }

            //These are extra sensors that I chose not to include, but there are many sensors that can be added beyond these commented ones, including HRV

            if (linearAccelerationSensor != null) {
                mSensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Log.d(TAG, "No Linear Acceleration Sensor found");
            }

            /*if (magneticFieldSensor != null) {
                mSensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Log.d(TAG, "No Magnetic Field Sensor found");
            }*/

            /*if (rotationVectorSensor != null) {
                mSensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Rotation Vector Sensor found");
            }*/

        }
    }


    //Stop tracking sensor data
    private void stopMeasurement() throws IOException {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        btSocket.close();
    }

    //This is the listener callback for when a sensor has data.
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(!btSocket.isConnected()) {
            try {
                stopMeasurement();
            } catch (IOException e) {
                Log.d(TAG, "Stopping measurement!");
                e.printStackTrace();
            }
        }

        int sensorType = event.sensor.getType();
        boolean isValidData = false;

        String toSend = "";
        long currentTimeMS = System.currentTimeMillis();

        if(sensorType == SENS_ACCELEROMETER) {
            toSend = "{\"ACCELEROMETER\": {";

            if(currentTimeMS >= lastTimeSampledAcc + throttle_delay) {
                numAccelSamples++;
                lastTimeSampledAcc = currentTimeMS;
                isValidData = true;
            }
        }
        else if(sensorType == SENS_GYROSCOPE) {
            toSend = "{\"GYROSCOPE\": {";

            if(currentTimeMS >= lastTimeSampledGyro + throttle_delay) {
                numGyroSamples++;
                lastTimeSampledGyro = currentTimeMS;
                isValidData = true;
            }
        }
        else if(sensorType == SENS_GRAVITY) {
            toSend = "{\"GRAVITY\": {";

            if(currentTimeMS >= lastTimeSampledGrav + throttle_delay) {
                numGravSamples++;
                lastTimeSampledGrav = currentTimeMS;
                isValidData = true;
            }
        }
        else if(sensorType == SENS_LINEAR_ACCELERATION) {
            toSend = "{\"LINEAR_ACCELEROMETER\": {";

            if(currentTimeMS >= lastTimeSampledLinearAcc + throttle_delay) {
                numLinearAccSamples++;
                lastTimeSampledLinearAcc = currentTimeMS;
                isValidData = true;
            }
        }
        /*else if(sensorType == SENS_MAGNETIC_FIELD) {
            toSend = "{\"MAGNETIC_FIELD\": {";
            numMagSamples++;
            isValidData = true;
        }*/

        //Only if the sensor event falls into one of the three categories above, we send it.
        //We also only buffer this data if sufficient time has passed - this is to throttle the amount of data sent.
        if(isValidData) {

            //We only stream data every second.
            if(System.currentTimeMillis() > lastTimeStartMillisec + 1000) {
                Log.d(TAG, "Received: " + numAccelSamples + " accel," + numGyroSamples + " gyro," + numGravSamples + " grav, " +
                numLinearAccSamples + " linear acc");
                lastTimeStartMillisec = System.currentTimeMillis();
            }


                toSend += "\"timestamp\":" + event.timestamp + ",";
                toSend += "\"values\":" + Arrays.toString(event.values);
                toSend += "}}^";

                //Log.d(TAG, toSend);
                try {
                    if(beginStreaming) {
                        writeBufferToDevice(toSend);
                    }
                } catch (IOException e) {
                    Log.d(TAG, " Could not write to Phone!");
                    e.printStackTrace();
                }



        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Log all available sensors to logcat
     */
    private void logAvailableSensors() {
        final List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
        Log.d(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", "SensorName", "StringType", "Type"));
        for (Sensor sensor : sensors) {
            Log.v(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", sensor.getName(), sensor.getStringType(), sensor.getType()));
        }

        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
    }

}