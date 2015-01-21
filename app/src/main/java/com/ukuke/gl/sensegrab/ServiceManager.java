package com.ukuke.gl.sensegrab;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gildoandreoni on 20/01/15.
 */
public class ServiceManager {
    // Singleton Class

    private static ServiceManager mInstance = null;
    private String txt;
    private List<ServiceComponent> serviceComponentList = new ArrayList<>();
    private List<ServiceComponent> serviceComponentActiveList = new ArrayList<>();

    private boolean scanDone = false;

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

    public void addServiceComponentActive(ServiceComponent serviceComponent) {
        serviceComponentActiveList.add(serviceComponent);
    }

    public List<ServiceComponent> getserviceComponentActiveList() {
        return serviceComponentActiveList;
    }

    public List<ServiceComponent> getAvailableServiceComponentList() {
        //if (!scanDone) {
        //    populateServiceComponentList(cn);
        //}
        List<ServiceComponent> mList = new ArrayList<>();

        for (int i = 0; i < serviceComponentList.size()-1; i++) {
            if (serviceComponentList.get(i).exists) {
                mList.add(serviceComponentList.get(i));
            }
        }
        return mList;
    }

    public int populateServiceComponentList(Context cn) {
        // Discovery Components
        int numAvailableServices = 0;

        SensorManager mSensorManager = (SensorManager) cn.getSystemService(Context.SENSOR_SERVICE);

        serviceComponentList.clear();

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            serviceComponentList.add(new ServiceComponent("Magnetic Field", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            serviceComponentList.add(new ServiceComponent("Accelerometer", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Accelerometer", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            serviceComponentList.add(new ServiceComponent("Temperature", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Temperature", false));
        }


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            serviceComponentList.add(new ServiceComponent("Gyroscope", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Gyroscope", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", true));
            numAvailableServices++;
        }
        else {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", false));
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
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
                case "Magnetic Field": componentImageID = R.drawable.ic_language_grey600_48dp;
                    break;
                case "Accelerometer": componentImageID = R.drawable.ic_vibration_grey600_48dp;
                    break;
                case "Temperature": componentImageID = R.drawable.ic_whatshot_grey600_48dp;
                    break;
                case "Gyroscope": componentImageID = R.drawable.ic_autorenew_grey600_48dp;
                    break;
                case "Light Sensor": componentImageID = R.drawable.ic_flare_grey600_48dp;
                    break;
                case "Proximity Sensor": componentImageID = R.drawable.ic_filter_list_grey600_48dp;
                    break;
                case "Pressure Sensor": componentImageID = R.drawable.ic_filter_hdr_grey600_48dp;
                    break;
                default: componentImageID = R.drawable.ic_close_grey600_48dp;
                    break;
            }


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

}
