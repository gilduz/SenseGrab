package com.ukuke.gl.sensegrab;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.SensorManager;
import android.content.Context;
import android.hardware.Sensor;

public class DeviceCapabilities extends ActionBarActivity {

    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_capabilities);
        Log.i("DeviceCapabilities", "Configure your app!");

        // Check Sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_MAGNETIC_FIELD!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_MAGNETIC_FIELD!");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_ACCELEROMETER!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_ACCELEROMETER!");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_AMBIENT_TEMPERATURE!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_AMBIENT_TEMPERATURE!");
        }


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_GYROSCOPE!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_GYROSCOPE!");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_LIGHT!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_LIGHT!");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_PROXIMITY!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_PROXIMITY!");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            // Success! There's a magnetometer.
            Log.i("DeviceCapabilities", "YES TYPE_PRESSURE!");
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_PRESSURE!");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_initial_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
