package com.ukuke.gl.sensegrab;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gildoandreoni on 20/01/15.
 */
public class ServiceManager {
    // Public

    // Private
    private static ServiceManager mInstance = null;
    private String txt;
    private List<ServiceComponent> serviceComponentList = new ArrayList<>();

    ServiceManager () {
    }

    public static ServiceManager getInstance(){
        if(mInstance == null)
        {
            mInstance = new ServiceManager();
        }
        return mInstance;
    }

    public List<ServiceComponent> getServiceComponentList() {
        return serviceComponentList;
    }

    public int populateServiceComponentList(Context cn) {
        // Discovery Components

        SensorManager mSensorManager = (SensorManager) cn.getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            serviceComponentList.add(new ServiceComponent("Magnetic Field", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            serviceComponentList.add(new ServiceComponent("Accelerometer", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Accelerometer", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            serviceComponentList.add(new ServiceComponent("Temperature", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Temperature", false));
        }


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            serviceComponentList.add(new ServiceComponent("Gyroscope", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Gyroscope", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", true));
        }
        else {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", false));
        }

        return serviceComponentList.size();
    }

    public class ServiceComponent {
        // Object to describe a component
        private String dysplayName;
        private boolean exists;
        private int availableImageID;
        private int componentImageID;

        ServiceComponent(String dysplayName, boolean exists) {
            this.dysplayName = dysplayName;
            this.exists = exists;
            if (this.exists) {
                availableImageID = R.drawable.ic_check_grey600_36dp;
            }
            else {
                availableImageID = R.drawable.ic_close_grey600_36dp;
            }
        }

        public int getAvailableImageID() {
            return availableImageID;
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

}
