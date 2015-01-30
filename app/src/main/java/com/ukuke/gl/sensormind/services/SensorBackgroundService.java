package com.ukuke.gl.sensormind.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.ukuke.gl.sensormind.ServiceManager;
import com.ukuke.gl.sensormind.support.FeedJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * for a background service not linked to an activity it's important to use the command approach
 * instead of the Binder. For starting use the alarm manager
 */
public class SensorBackgroundService extends Service implements SensorEventListener {

    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    private SensorManager mSensorManager = null;
    private boolean logging = false;

    public static final String KEY_SENSOR_TYPE = "sensor_type";
    public static final String KEY_LOGGING = "logging";
    public static final String KEY_WINDOW = "num_samples";

    private List<ServiceManager.DataSample> listDataSample = new ArrayList<>();

    List<FeedJSON> listFeed = new ArrayList<FeedJSON>();

    private int lastStartId;
    private int window = 1;

    int counterAccelerometer = 0;
    int counterGyroscope = 0;
    int counterMagnetometer = 0;

    int windowAccelerometer = 1;
    int windowGyroscope = 1;
    int windowMagnetometer = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        lastStartId = startId;

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // have a default sensor configured
        int sensorType = Sensor.TYPE_LIGHT;

        Bundle args = intent.getExtras();

        // get some properties from the intent
        if (args != null) {

            // set sensortype from bundle
            if (args.containsKey(KEY_SENSOR_TYPE)) {
                sensorType = args.getInt(KEY_SENSOR_TYPE);
            }
            if (args.containsKey(KEY_WINDOW)) {
                window = args.getInt(KEY_WINDOW);
            }
            // optional logging
            logging = args.getBoolean(KEY_LOGGING);
        }


        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                windowAccelerometer = window;
                break;
            case Sensor.TYPE_GYROSCOPE:
                windowGyroscope = window;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                windowMagnetometer = window;
                break;
        }

        //count++;
        Sensor sensor = mSensorManager.getDefaultSensor(sensorType);


        mSensorManager.registerListener(this, sensor,  SensorManager.SENSOR_DELAY_NORMAL);

        //Log.d(TAG,"Registrato sensore: " + sensor.getName());

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // ignore this since not linked to an activity
        return null;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        addFeedToList(event);
        //count++;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                counterAccelerometer++;
                if (counterAccelerometer >= windowAccelerometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterAccelerometer = 0;
                }
                    break;
            case Sensor.TYPE_GYROSCOPE:
                counterGyroscope++;
                if (counterGyroscope >= windowGyroscope) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterGyroscope = 0;
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                counterMagnetometer++;
                if (counterMagnetometer >= windowMagnetometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterMagnetometer = 0;
                }
                break;
            default:
                mSensorManager.unregisterListener(this, event.sensor);
        }



        // if (count >= window) {

        //      count = 0;
        //  }
        //stopSelfResult(lastStartId);
    }

    // Call saveListFeedOnDB somethimes to transfer data on database
    public int saveListFeedOnDB() {
        int feedsSaved = 0;
        FeedJSON currentFeed;

        try {
            for (int i = 0; i < listFeed.size(); i++) {
                currentFeed = listFeed.remove(i);
                // TODO: Leo, qui devi salvare il currentfeed su DB. Anzi prima bisogna cambiare struttura... x caricare non è FeedJSON
                feedsSaved++;
            }
        }catch (Exception e) {
            Log.e(TAG, "Error saving on DB: " + e);
        }

        return feedsSaved;
    }

    private void addFeedToList(SensorEvent event) {
        // TODO: Creare un feed ed aggiungerlo alla feedList prima di ogni if(logging)

        listFeed.add(new FeedJSON("Ciao", false, "Stringa", "Stringa", 1));

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                if (logging)
                    Log.d(TAG, listFeed.size() + ": SENSOR LIGHT: \t\t\t" + event.values[0]);
                break;
            case Sensor.TYPE_PROXIMITY:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR PROXIMITY: \t" + event.values[0]);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR TEMPERATURE: \t" + event.values[0]);
                break;
            case Sensor.TYPE_PRESSURE:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR PRESSURE: \t\t" + event.values[0]);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR ACCELEROMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR GYROSCOPE: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (logging)
                    Log.d(TAG, listFeed.size() +  ": SENSOR MAGNETOMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                break;
        }
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "SensorBackgroundService Stopped", Toast.LENGTH_LONG).show();
    }

}