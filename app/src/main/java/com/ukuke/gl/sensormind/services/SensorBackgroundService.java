package com.ukuke.gl.sensormind.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * for a background service not linked to an activity it's important to use the command approach
 * instead of the Binder. For starting use the alarm manager
 */
public class SensorBackgroundService extends Service implements SensorEventListener {

    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    private SensorManager mSensorManager = null;
    private boolean mLogging = false;
    private static float previousValue;

    public static final String KEY_SENSOR_TYPE = "sensor_type";
    public static final String KEY_LOGGING = "logging";

    private int count;

    private int lastStartId;
       // LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

//    // Define a listener that responds to location updates
//    LocationListener locationListener = new LocationListener() {
//        public void onLocationChanged(Location location) {
//            // Called when a new location is found by the network location provider.
//            //makeUseOfNewLocation(location);
//            Log.d(TAG, "LOCATION: " + location.toString());
//        }
//
//        public void onStatusChanged(String provider, int status, Bundle extras) {}
//
//        public void onProviderEnabled(String provider) {}
//
//        public void onProviderDisabled(String provider) {}
//    };



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
            // optional logging
            mLogging = args.getBoolean(KEY_LOGGING);
        }

        //count++;
        Sensor sensor = mSensorManager.getDefaultSensor(sensorType);


        mSensorManager.registerListener(this, sensor,  SensorManager.SENSOR_DELAY_NORMAL);

        //Log.d(TAG,"Registrato sensore: " + sensor.getName());

        // Register the listener with the Location Manager to receive location updates
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

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

        StringBuilder sb = new StringBuilder();
        sb.append(event.sensor.getName()+": \t\t");
        for (float value : event.values)
            sb.append(String.valueOf(value)).append(" | ");
        Log.d(TAG, sb.toString());

        //count--;

//        switch (event.sensor.getType()) {
//            case Sensor.TYPE_LIGHT:
//                Log.d(TAG,"SENSOR LIGHT: \t\t\t" + event.values[0]);
//                break;
//            case Sensor.TYPE_PROXIMITY:
//                Log.d(TAG,"SENSOR PROXIMITY: \t" + event.values[0]);
//                break;
//            case Sensor.TYPE_AMBIENT_TEMPERATURE:
//                Log.d(TAG,"SENSOR TEMPERATURE: \t" + event.values[0]);
//                break;
//            case Sensor.TYPE_PRESSURE:
//                Log.d(TAG,"SENSOR PRESSURE: \t\t" + event.values[0]);
//                break;
//            case Sensor.TYPE_ACCELEROMETER:
//                Log.d(TAG,"SENSOR ACCELEROMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
//                break;
//            case Sensor.TYPE_GYROSCOPE:
//                Log.d(TAG,"SENSOR GYROSCOPE: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
//                break;
//            case Sensor.TYPE_MAGNETIC_FIELD:
//                Log.d(TAG,"SENSOR MAGNETOMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
//                break;
//        }


        mSensorManager.unregisterListener(this,event.sensor);

        float sensorValue = event.values[0];
        previousValue = sensorValue;
        // stop the sensor and service

        stopSelfResult(lastStartId);

    }



}
