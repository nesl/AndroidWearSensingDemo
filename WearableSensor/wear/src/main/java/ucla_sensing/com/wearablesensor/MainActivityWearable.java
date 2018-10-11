package ucla_sensing.com.wearablesensor;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivityWearable extends WearableActivity {

    private final static String TAG = "DBG-MainActivityWear";

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
        Intent intent = new Intent(this, sensingService.class);
        startService(intent);
    }

    //Stop tracking, close bluetooth connection.
    public void stopTracking(View view) {
        Log.d(TAG, "Stop Tracking");
        Intent intent = new Intent(this, sensingService.class);
        stopService(intent);
    }
}
