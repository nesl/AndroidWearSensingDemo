package ucla_sensing.com.wearablesensor;

import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class dataPackager extends Thread {

    final String TAG = "DBG-DATAPACKAGER:";

    File mExportRoot;
    File mExportDir;

    //String mCurrentDateString = "";

    public dataPackager() {
        //mCurrentDateString = getCurrentDate();
        //Log.d(TAG, "Setting current date: " + mCurrentDateString);

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d(TAG, "No SD card, can't export Mood Data");
        } else {
            //We use the Download directory for saving our .csv file.
            mExportRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            mExportDir = new File(mExportRoot, "WEARABLE_DATA");
            if (!mExportDir.exists()) {
                mExportDir.mkdirs();
            }
        }
    }


    public void directWriteToFile(String packet) {

            File file;
            PrintWriter printWriter = null;
            try
            {
                file = new File(mExportDir,  "RAW.txt");
                file.createNewFile();
                printWriter = new PrintWriter(new FileWriter(file, true));


                printWriter.println(packet); //write the record to the mood textfile
                //Log.d(TAG, "Successfully wrote to " + fileName);
            }

            catch(Exception exc) {
                //if there are any exceptions, return false
                Log.d(TAG, exc.getMessage());
            }
            finally {
                if(printWriter != null) printWriter.close();
            }
    }

    public void unpackageFromRaw() throws IOException {
        FileInputStream is = null;
        BufferedReader reader;
        final File file = new File(mExportDir, "RAW.txt");

        if (file.exists()) {
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if(is != null) {
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while(line != null){
                    line = reader.readLine();
                    try {
                        convertPacketToJSON(line);
                    } catch (JSONException e) {
                        Log.d(TAG, "BAD CONVERSION FROM PACKET TO JSON/CSV");
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    public void convertPacketToJSON(String packet) throws JSONException {


        String packetData = packet.substring(packet.indexOf("]")+1);  //We want everything after the sampling information
        String[] packetEntries = packetData.split("\\^");

        for(String packetEntry : packetEntries) {
            //Log.d(TAG, packetEntry);

            JSONObject packetJSON = new JSONObject(packetEntry);
            JSONObject sensorJSON = null;
            String SensorName = "";
            if(packetJSON.has("ACCELEROMETER")) {
                SensorName = "ACCELEROMETER";
                sensorJSON = packetJSON.getJSONObject("ACCELEROMETER");
            }
            else if(packetJSON.has("GYROSCOPE")) {
                SensorName = "GYROSCOPE";
                sensorJSON = packetJSON.getJSONObject("GYROSCOPE");
            }
            else if(packetJSON.has("GRAVITY")) {
                SensorName = "GRAVITY";
                sensorJSON = packetJSON.getJSONObject("GRAVITY");
            }
            else if(packetJSON.has("LINEAR_ACCELEROMETER")) {
                SensorName = "LINEAR_ACCELEROMETER";
                sensorJSON = packetJSON.getJSONObject("LINEAR_ACCELEROMETER");
            }

            if(sensorJSON != null) {
                long timeStamp = sensorJSON.getLong("timestamp");
                double xVal = sensorJSON.getJSONArray("values").getDouble(0);
                double yVal = sensorJSON.getJSONArray("values").getDouble(1);
                double zVal = sensorJSON.getJSONArray("values").getDouble(2);

                //Log.d(TAG, SensorName + " Data: t=" + timeStamp + " vals: " + xVal + "," + yVal + "," + zVal);

                String fileMessage = timeStamp + "," + xVal + "," + yVal + "," + zVal;

                exportData(SensorName, fileMessage);
            }

        }
    }


    //THIS EXPORTS ONE LINE OF DATA FROM A DATABASE
    public boolean exportData(String fileName, String message) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d(TAG, "No SD card, can't export Mood Data");
            return false;
        } else {
            //We use the Download directory for saving our .csv file.
            File exportRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File exportDir = new File(exportRoot, "WEARABLE_DATA");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file;
            PrintWriter printWriter = null;
            try
            {
                String fileHeader = "";
                file = new File(exportDir,  fileName + ".csv");

                if(file.createNewFile()) {
                    fileHeader = "TIMESTAMP(MS),X,Y,Z";
                }

                printWriter = new PrintWriter(new FileWriter(file, true));

                //If we just created the file, then we add in the header.
                if(!fileHeader.isEmpty()) {
                    printWriter.println(fileHeader);
                }

                printWriter.println(message); //write the record to the mood textfile
                //Log.d(TAG, "Successfully wrote to " + fileName);
            }

            catch(Exception exc) {
                //if there are any exceptions, return false
                Log.d(TAG, exc.getMessage());
                return false;
            }
            finally {
                if(printWriter != null) printWriter.close();
            }

            return true;
        }
    }



}
