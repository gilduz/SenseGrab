package com.ukuke.gl.sensegrab;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.SensorManager;
import android.content.Context;
import android.hardware.Sensor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceCapabilities extends ActionBarActivity {

    /*private SensorManager mSensorManager;*/
    public List<ServiceComponent> serviceComponentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_capabilities);
        Log.i("DeviceCapabilities", "Configure your app!");

        populateServiceComponentList();

        populateListView();

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

    private void populateListView() {
        ArrayAdapter<ServiceComponent> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.listViewDeviceCapabilities);
        list.setAdapter(adapter);
    }

    private int populateServiceComponentList() {
        // Check Sensors
        int numServices = 0;

        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            Log.i("DeviceCapabilities", "YES TYPE_MAGNETIC_FIELD!");
            serviceComponentList.add(new ServiceComponent("Magnetic Field", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_MAGNETIC_FIELD!");
            serviceComponentList.add(new ServiceComponent("Magnetic Field", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            Log.i("DeviceCapabilities", "YES TYPE_ACCELEROMETER!");
            serviceComponentList.add(new ServiceComponent("Accelerometer", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_ACCELEROMETER!");
            serviceComponentList.add(new ServiceComponent("Accelerometer", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            Log.i("DeviceCapabilities", "YES TYPE_AMBIENT_TEMPERATURE!");
            serviceComponentList.add(new ServiceComponent("Temperature", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_AMBIENT_TEMPERATURE!");
            serviceComponentList.add(new ServiceComponent("Temperature", false));
        }


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            Log.i("DeviceCapabilities", "YES TYPE_GYROSCOPE!");
            serviceComponentList.add(new ServiceComponent("Gyroscope", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_GYROSCOPE!");
            serviceComponentList.add(new ServiceComponent("Gyroscope", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            Log.i("DeviceCapabilities", "YES TYPE_LIGHT!");
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_LIGHT!");
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            Log.i("DeviceCapabilities", "YES TYPE_PROXIMITY!");
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_PROXIMITY!");
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            Log.i("DeviceCapabilities", "YES TYPE_PRESSURE!");
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", true));
            numServices++;
        }
        else {
            Log.i("DeviceCapabilities", "NO TYPE_PRESSURE!");
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", false));
        }

        return numServices;
    }

    private class MyListAdapter extends ArrayAdapter<ServiceComponent> {
        public MyListAdapter() {
            super(DeviceCapabilities.this, R.layout.item_view, serviceComponentList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceComponent currentServiceComponent = serviceComponentList.get(position);

            ImageView  imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.imageID);

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.dysplayName);

            return itemView;
            //return super.getView(position, convertView, parent);
        }
    }
}
