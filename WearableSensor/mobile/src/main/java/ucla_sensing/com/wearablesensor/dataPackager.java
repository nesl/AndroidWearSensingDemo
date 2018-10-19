package ucla_sensing.com.wearablesensor;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class dataPackager {

    final String TAG = "DBG-DATAPACKAGER:";

    //String mCurrentDateString = "";

    public dataPackager() {
        //mCurrentDateString = getCurrentDate();
        //Log.d(TAG, "Setting current date: " + mCurrentDateString);
    }

    /*private String getCurrentDate() {

        String currentDateString = "";
        SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd");
        currentDateString = formatter.format(new Date(System.currentTimeMillis()));
        return currentDateString;
    }*/


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
                Log.d(TAG, "Successfully wrote to " + fileName);
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
