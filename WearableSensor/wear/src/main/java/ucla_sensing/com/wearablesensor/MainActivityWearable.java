package ucla_sensing.com.wearablesensor;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivityWearable extends WearableActivity {

    private final static String TAG = "DBG-MainActivityWear";

    private boolean startTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wearable);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Stop Tracking");
        Intent intent = new Intent(this, sensingService.class);
        stopService(intent);
    }


    //Begin tracking the sensor data and transmitting over bluetooth to the phone device
    public void startTracking(View view) {
        Log.d(TAG, "Start Tracking");
        startTracking = true;
        Intent intent = new Intent(this, sensingService.class);
        startService(intent);
    }

    //Stop tracking, close bluetooth connection.
    public void stopTracking(View view) {
        Log.d(TAG, "Stop Tracking");
        Intent intent = new Intent(this, sensingService.class);
        stopService(intent);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        if(startTracking) {
            //If we are going into Ambient mode, we lower the frequency throttle delay - basically sample faster
            Log.d(TAG, "Entered Ambient Mode.");
            Intent intent = new Intent(this, sensingService.class);
            intent.putExtra("ALTER_FREQ", 6);
            startService(intent);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        if(startTracking) {
            //If we are going into Ambient mode, we increase the frequency throttle delay - basically sample slower
            Log.d(TAG, "Exited Ambient Mode.");
            Intent intent = new Intent(this, sensingService.class);
            intent.putExtra("ALTER_FREQ", 8);
            startService(intent);
        }

    }


}
