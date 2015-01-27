package com.ukuke.gl.sensormind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ukuke.gl.sensormind.services.SensorBackgroundService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gildoandreoni on 20/01/15.
 */

public class ServiceManager {
    // Singleton Class
    SharedPreferences prefs = null;

    private static ServiceManager mInstance = null;
    private List<ServiceComponent> serviceComponentList = new ArrayList<>();
    private List<ServiceComponent> serviceComponentActiveList = new ArrayList<>();

    SensorManager sensorManager;
    private boolean scanDone = false;
    Context cn;

    private boolean USE_DB = false;

    DbHelper dbHelper;


    public static ServiceManager getInstance(Context cn){
        if(mInstance == null)
        {
            mInstance = new ServiceManager(cn);
        }

        return mInstance;
    }

    ServiceManager (Context cn) {
        this.cn = cn;
        prefs = cn.getSharedPreferences("com.ukuke.gl.sensormind", cn.MODE_PRIVATE);
        populateServiceComponentList();
    }

    public List<ServiceComponent> getServiceComponentList() {
        return serviceComponentList;
    }

    public void addServiceComponentActive(ServiceComponent serviceComponent) {
        serviceComponentActiveList.add(serviceComponent);
    }

    public int initializeFromDB(){
        // TODO: Da implementare
        USE_DB = true;
        dbHelper = new DbHelper(cn);
        Log.d("Service Manager", "Found in DB " + dbHelper.numberOfConfigurations() + " configurations");
        if (dbHelper.numberOfConfigurations()>0) {
            Cursor cursor;
            ArrayList<String> array_list = null;
            Log.d("Service Manager", "Sono arrivato a prima del DBHELPER");
            array_list = dbHelper.getAllConfigurationsWithoutOrder();

            Log.d("Service Manager", "Sono arrivato a prima del FOR");

            for (int i = 0; i < array_list.size(); i++) {
                cursor = dbHelper.getConfCursorByName(array_list.get(i).toString());
                int sensorType = cursor.getInt(cursor.getColumnIndex("type"));
                int interval = cursor.getInt(cursor.getColumnIndex("time"));
                int window = cursor.getInt(cursor.getColumnIndex("window"));

                startScheduleService(sensorType, true, (long) interval, window);
            }
        }


        return 0;
    }

    public void removeServiceComponentActive(int sensorType) {
        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
            if (serviceComponentActiveList.get(i).getSensorType() == sensorType) {
                if (USE_DB) {
                    dbHelper.deleteConfigurationById(serviceComponentActiveList.get(i).getSensorType());
                }
                serviceComponentActiveList.remove(i);
            }
        }
    }

    public List<ServiceComponent> getServiceComponentActiveList() {
        return serviceComponentActiveList;
    }

    public ServiceComponent getServiceComponentActiveBySensorType(int serviceType) {
        ServiceComponent service = new ServiceComponent("NULL",false);
        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
            service = serviceComponentActiveList.get(i);
            if (service.getSensorType() == serviceType) {
                return service;
            }
        }
        return service;
    }

    public List<ServiceComponent> getAvailableServiceComponentList() {
        //if (!scanDone) {
        //    populateServiceComponentList(cn);
        //}
        List<ServiceComponent> mList = new ArrayList<>();

        for (int i = 0; i < serviceComponentList.size(); i++) {
            if (serviceComponentList.get(i).exists) {
                mList.add(serviceComponentList.get(i));
            }
        }
        return mList;
    }

    public ServiceComponent getServiceComponentActiveBySensorType(String name) {
        ServiceComponent service = new ServiceComponent("NULL",true);

        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
            if (serviceComponentActiveList.get(i).getDysplayName() == name) {
                service = serviceComponentActiveList.get(i);
            }
        }
        return service;
    }

    public int populateServiceComponentList() {
        // Discovery Components
        int numAvailableServices = 0;

        sensorManager = (SensorManager) cn.getSystemService(Context.SENSOR_SERVICE);

        serviceComponentList.clear();

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            serviceComponentList.add(new ServiceComponent("Magnetic Field", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            serviceComponentList.add(new ServiceComponent("Accelerometer", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Accelerometer", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            serviceComponentList.add(new ServiceComponent("Temperature", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Temperature", false));
        }


        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            serviceComponentList.add(new ServiceComponent("Gyroscope", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Gyroscope", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", false));
        }

        scanDone = true;
        return numAvailableServices;
    }

    public class ServiceComponent {
        // Object to describe a component
        private String dysplayName;
        private boolean exists;
        private int availableImageID;
        private int componentImageID;
        private int sensorType;
        boolean logging = false;
        long interval = 1000;
        int window = 1;

        public void setLogging(boolean value) {
            this.logging = value;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public void setWindow(int window) {
            this.window = window;
        }

        public boolean getLogging() {
            return logging;
        }

        public long getInterval() {
            return interval;
        }

        public int getWindow() {
            return window;
        }

        ServiceComponent(String dysplayName, boolean exists) {
            this.dysplayName = dysplayName;
            this.exists = exists;
            if (this.exists) {
                availableImageID = R.drawable.ic_check_grey600_36dp;
            }
            else {
                availableImageID = R.drawable.ic_close_grey600_36dp;
            }

            switch (dysplayName) {
                case "Magnetic Field":
                    componentImageID = R.drawable.ic_language_grey600_48dp;
                    sensorType = Sensor.TYPE_MAGNETIC_FIELD;
                    break;
                case "Accelerometer":
                    componentImageID = R.drawable.ic_vibration_grey600_48dp;
                    sensorType = Sensor.TYPE_ACCELEROMETER;
                    break;
                case "Temperature":
                    componentImageID = R.drawable.ic_whatshot_grey600_48dp;
                    sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE;
                    break;
                case "Gyroscope":
                    componentImageID = R.drawable.ic_autorenew_grey600_48dp;
                    sensorType = Sensor.TYPE_GYROSCOPE;
                    break;
                case "Light Sensor":
                    componentImageID = R.drawable.ic_flare_grey600_48dp;
                    sensorType = Sensor.TYPE_LIGHT;
                    break;
                case "Proximity Sensor":
                    componentImageID = R.drawable.ic_filter_list_grey600_48dp;
                    sensorType = Sensor.TYPE_PROXIMITY;
                    break;
                case "Pressure Sensor":
                    componentImageID = R.drawable.ic_filter_hdr_grey600_48dp;
                    sensorType = Sensor.TYPE_PRESSURE;
                    break;
                default: componentImageID = R.drawable.ic_close_grey600_48dp;
                    break;
            }


        }

        public int getSensorType() {
            return sensorType;
        }

        public int getAvailableImageID() {
            return availableImageID;
        }

        public boolean isScanDone() {
            return scanDone;
        }

        public int getComponentImageID() {
            return componentImageID;
        }

        public String getDysplayName() {
            return dysplayName;
        }

        public boolean getExists() {
            return  exists;
        }
    }

    // Service related methods

    public void startScheduleService(int typeSensor) {
        startScheduleService(typeSensor, getServiceComponentActiveBySensorType(typeSensor).getLogging(), getServiceComponentActiveBySensorType(typeSensor).getInterval(), getServiceComponentActiveBySensorType(typeSensor).getWindow());
    }

    public void startScheduleService(int typeSensor, boolean logging, long interval, int window) {

        getServiceComponentActiveBySensorType(typeSensor).setInterval(interval);
        getServiceComponentActiveBySensorType(typeSensor).setLogging(logging);
        getServiceComponentActiveBySensorType(typeSensor).setWindow(window);

        if (prefs.getBoolean("enableGrabbing",true)) {

            AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(cn, SensorBackgroundService.class);

            Bundle args = new Bundle();

            try {
                args.putBoolean(SensorBackgroundService.KEY_LOGGING, logging);
            } catch (Exception e) {
            }
            try {
                args.putInt(SensorBackgroundService.KEY_SENSOR_TYPE, typeSensor);
            } catch (Exception e) {
            }
            try {
                args.putInt(SensorBackgroundService.KEY_WINDOW, window);
            } catch (Exception e) {
            }

            intent.putExtras(args);

            // Start the service

            PendingIntent scheduledIntent = PendingIntent.getService(cn, typeSensor, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, scheduledIntent);
        }
    }

    public void stopScheduleService(int typeSensor) {
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        PendingIntent scheduledIntent = PendingIntent.getService(cn, typeSensor, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.cancel(scheduledIntent);
    }

    public void addScheduleServiceToDB(int typeSensor, boolean logging, long interval, int window) {
        if (USE_DB) {
            dbHelper.newConfiguration(getServiceComponentActiveBySensorType(typeSensor).getDysplayName(), typeSensor, (int) interval/1000, "sec", window/1000, false);
            Log.d("Service Manager", "Found in DB " + dbHelper.numberOfConfigurations() + " configurations");
        }
    }

}
