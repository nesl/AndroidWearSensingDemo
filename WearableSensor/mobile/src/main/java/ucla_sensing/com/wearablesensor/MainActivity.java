package ucla_sensing.com.wearablesensor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import android.provider.Settings.Secure;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "DBG-MainActivity";

    AcceptThread btThread;

    private String UUIDStr = "71b37966-2466-45b7-ae2c-f42851fcac8e";

    private TextView mConnectionText;
    private TextView mDataBoxText;

    private boolean isConnected = false;

    dataPackager dpackager = new dataPackager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionText = findViewById(R.id.connectStatus);
        mDataBoxText = findViewById(R.id.dataBox);

        try {
            initBluetooth();  //Initiate the Bluetooth
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        requestExternalWriteAccess();

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(connBroadcastReceiver, new IntentFilter("DEVICE_CONNECTED"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(dataBroadcastReceiver, new IntentFilter("INFO_RECEIVED"));
    }

    public void requestExternalWriteAccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    100);
        }
    }

    private BroadcastReceiver connBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!isConnected) {
                Log.d(TAG, "CONNECTED");
                mConnectionText.setText("CONNECTED TO WATCH");
            }
            else {
                Log.d(TAG, "DISCONNECTED");
                mConnectionText.setText("DISCONNECTED FROM WATCH");
            }


        }
    };
    private BroadcastReceiver dataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            Log.d(TAG, data);
            //mConnectionText.setText("CONNECTED TO WATCH");
            mDataBoxText.setText(data);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying Activity");

        btThread.cancel();
        btThread.interrupt();

    }

    public void startSavingData(View view) {
        Log.d(TAG, "Begin Saving Data!");
        btThread.beginSavingData();
    }

    //Stops the Bluetooth connection, distributes the data from the raw text file into each sensor
    public void stopAndSaveData(View view) {

        Log.d(TAG, "STOP");
        btThread.cancel();
        btThread.interrupt();

        mConnectionText.setText("Closed Connection!");

        /*try {
            dpackager.unpackageFromRaw();
        } catch (IOException e) {
            Log.d(TAG, "ERROR IN UNPACKING DATA");
            e.printStackTrace();
        }*/
    }

    /*
        Initiate the Bluetooth opening the worker thread AcceptThread for handling bluetooth connections.
     */
    private void initBluetooth() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Log.d(TAG, "Initiating Bluetooth Connection...");
        final BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled()) {

                Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(blueAdapter, null);


                //Have a worker thread instantiate and run the bluetooth communication thread
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        btThread = new AcceptThread(blueAdapter, UUID.fromString(UUIDStr), getApplicationContext());
                        btThread.run();
                    }
                }).start();

                Log.d(TAG, "UUID: " + uuids[0].getUuid().toString());

                //Log.e("error", "No appropriate paired devices.");
            } else {
                Log.e("error", "Bluetooth is disabled.");
            }

        }
    }

}
