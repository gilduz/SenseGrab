package com.ukuke.gl.sensormind.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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

        Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

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
        for (float value : event.values)
            sb.append(String.valueOf(value)).append(" | ");
        Log.d(TAG, "SENSOR: " + sb.toString()+ " and previous  was: " + previousValue);


        float sensorValue = event.values[0];
        previousValue = sensorValue;
        // stop the sensor and service
        mSensorManager.unregisterListener(this);
        stopSelf();

    }



}
