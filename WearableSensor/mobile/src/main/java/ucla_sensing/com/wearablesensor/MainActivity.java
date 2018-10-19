package ucla_sensing.com.wearablesensor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
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
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "DBG-MainActivity";

    AcceptThread btThread;

    private String UUIDStr = "71b37966-2466-45b7-ae2c-f42851fcac8e";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    public void requestExternalWriteAccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying Activity");

        btThread.cancel();
        btThread.interrupt();
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
                        btThread = new AcceptThread(blueAdapter, UUID.fromString(UUIDStr));
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
