package com.ukuke.gl.sensormind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.support.FeedJSON;
import com.ukuke.gl.sensormind.support.SensormindAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gildoandreoni on 20/01/15.
 *
 */

public class ServiceManager {
    // Singleton Class
    private SharedPreferences prefs = null;

    private static final String TAG = SensormindAPI.class.getSimpleName();

    private static ServiceManager mInstance = null;
    private List<ServiceComponent> serviceComponentList = new ArrayList<>();
//    private List<ServiceComponent> serviceComponentConfiguredList = new ArrayList<>();
//    private List<ServiceComponent> serviceComponentActiveList = new ArrayList<>();

    public static final String PATH_ACTIVITY_IN_VEHICLE = "/activity/in_vehicle";
    public static final String PATH_ACTIVITY_ON_BICYCLE = "/activity/on_bicycle";
    public static final String PATH_ACTIVITY_ON_FOOT = "/activity/on_foot";
    public static final String PATH_ACTIVITY_RUNNING = "/activity/running";
    public static final String PATH_ACTIVITY_STILL = "/activity/still";
    public static final String PATH_ACTIVITY_TILTING = "/activity/tilting";
    public static final String PATH_ACTIVITY_UNKNOWN = "/activity/unknown";
    public static final String PATH_ACTIVITY_WALKING = "/activity/walking";
    public static final String PATH_MOST_PROBABLE_ACTIVITY ="/activity/most_probable_activity";
    public static final String PATH_ACTIVITY ="/activity";

    public static final int SENSOR_TYPE_ACTIVITY = 100;

    public List<FeedJSON> allFeedList = new ArrayList<>();

    SensormindAPI API = null;

    Context cn;
    SensorManager sensorManager;
    private boolean scanDone = false;

    int OFFSET_INTENT = 199;


    private boolean USE_DB = false;
    DbHelper dbHelper;

    ServiceManager(Context cn) {
        this.cn = cn;
        prefs = PreferenceManager.getDefaultSharedPreferences(cn);
        populateServiceComponentList();
        initializeFromDB();
    }

    public static ServiceManager getInstance(Context cn) {
        if (mInstance == null) {
            mInstance = new ServiceManager(cn);
        }
        return mInstance;
    }


    public static class ServiceComponent {
        // Object to describe a component
        private String dysplayName;
        private boolean exists;
        private int availableImageID;
        private int componentImageID;
        private int sensorType;
        private int minDelay;
        private String defaultPath;
        private static String modelName;
        boolean logging = false;

        private Configuration activeConfiguration = null;

        public List<Configuration> configurationList = new ArrayList<>();

        public void setActiveConfiguration(Configuration activeConfiguration) {
            this.activeConfiguration = activeConfiguration;
        }

        public String getDefaultPath() {
            return defaultPath;
        }

        public void setDefaultPath(String defaultPath) {
            this.defaultPath = defaultPath;
        }

        public int addConfiguration(String configurationName, String path, long interval, int window, boolean attachGPS) {
            // Se egiste già con lo stesso nome ritorna -1
            for (int i = 0; i < configurationList.size(); i++) {
                if (configurationList.get(i).getConfigurationName().equals(configurationName)) {
                    return -1;
                }
            }
            // Altrimenti aggiungi
            configurationList.add(new Configuration(configurationName, path, interval, window, attachGPS));
            return configurationList.size();
        }

        public void addConfiguration(Configuration configuration) {
            configurationList.add(configuration);
        }

        public Configuration getConfiguration(String configurationName) {
            for (int i = 0; i < configurationList.size(); i++) {
                if (configurationList.get(i).getConfigurationName().equals(configurationName)) {
                    return configurationList.get(i);
                }
            }
            return null;
        }

        public Configuration getConfiguration(int id) {
            if (configurationList.size() > id) {
                return configurationList.get(id);
            }
            return null;
        }

        public Configuration getConfigurationByDbId(int dbId) {
            for (int i = 0; i < configurationList.size(); i++) {
                if (configurationList.get(i).getDbId() == dbId) {
                    return configurationList.get(i);
                }
            }
            return null;
        }

        public int getConfigurationSize() {
            return configurationList.size();
        }

        public int removeConfiguration(String configurationName) {
            for (int i = 0; i < configurationList.size(); i++) {
                if (configurationList.get(i).getConfigurationName() == configurationName) {
                    configurationList.remove(i);
                    return i;
                }
            }
            return -1;
        }

        public int removeConfigurationByDbId(int dbId) {
            for (int i = 0; i < configurationList.size(); i++) {
                if (configurationList.get(i).getDbId() == dbId) {
                    configurationList.remove(i);
                    return i;
                }
            }
            return -1;
        }

        ServiceComponent(String dysplayName, String ModelName, boolean exists) {
            this.dysplayName = dysplayName;
            this.modelName = ModelName;
            this.exists = exists;
            if (this.exists) {
                availableImageID = R.drawable.ic_check_grey600_36dp;
            } else {
                availableImageID = R.drawable.ic_close_grey600_36dp;
            }

            switch (dysplayName) {
                case "Activity":
                    componentImageID = R.drawable.ic_directions_walk_grey600_48dp;
                    sensorType = ServiceManager.SENSOR_TYPE_ACTIVITY;
                    defaultPath = modelName + PATH_ACTIVITY;
                    break;
                case "Magnetic Field":
                    componentImageID = R.drawable.ic_language_grey600_48dp;
                    sensorType = Sensor.TYPE_MAGNETIC_FIELD;
                    defaultPath = modelName + "/magnetometer";
                    break;
                case "Accelerometer":
                    componentImageID = R.drawable.ic_vibration_grey600_48dp;
                    sensorType = Sensor.TYPE_ACCELEROMETER;
                    defaultPath = modelName + "/accelerometer";
                    break;
                case "Temperature":
                    componentImageID = R.drawable.ic_whatshot_grey600_48dp;
                    sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE;
                    defaultPath = modelName + "/temperature";
                    break;
                case "Gyroscope":
                    componentImageID = R.drawable.ic_autorenew_grey600_48dp;
                    sensorType = Sensor.TYPE_GYROSCOPE;
                    defaultPath = modelName + "/gyroscope";
                    break;
                case "Light Sensor":
                    componentImageID = R.drawable.ic_flare_grey600_48dp;
                    sensorType = Sensor.TYPE_LIGHT;
                    defaultPath = modelName + "/light";
                    break;
                case "Proximity Sensor":
                    componentImageID = R.drawable.ic_filter_list_grey600_48dp;
                    sensorType = Sensor.TYPE_PROXIMITY;
                    defaultPath = modelName + "/proximity";
                    break;
                case "Pressure Sensor":
                    componentImageID = R.drawable.ic_filter_hdr_grey600_48dp;
                    sensorType = Sensor.TYPE_PRESSURE;
                    defaultPath = modelName + "/pressure";
                    break;
                default:
                    componentImageID = R.drawable.ic_close_grey600_48dp;
                    break;
            }


        }

        public int getSensorType() {
            return sensorType;
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

        public int getMinDelay() {
            return minDelay;
        }

        public void setMinDelay(int minDelay) {
            this.minDelay = minDelay;
        }

        public boolean getExists() {
            return exists;
        }

        public Configuration getActiveConfiguration() {
            return activeConfiguration;
        }

        public static class Configuration {

            private long interval = 1000;
            private int window = 2;
            private String configurationName;
            private String path;
            private boolean attachGPS;
            private int dbId = -1;


            Configuration() {
            }

            Configuration(String configurationName, String path, long interval, int window, boolean attachGPS) {
                this.configurationName = configurationName;
                this.interval = interval;
                this.window = window;
                this.attachGPS = attachGPS;
                this.path = path;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public long getInterval() {
                return interval;
            }

            public int getWindow() {
                return window;
            }

            public int getDbId() {
                return dbId;
            }

            public void setDbId(int dbId) {
                this.dbId = dbId;
            }

            public String getConfigurationName() {
                return configurationName;
            }

            public void setInterval(long interval) {
                this.interval = interval;
            }

            public void setWindow(int window) {
                this.window = window;
            }

            public void setConfigurationName(String configurationName) {
                this.configurationName = configurationName;
            }

            public boolean getAttachGPS() {
                return attachGPS;
            }

            public void setAttachGPS(boolean attachGPS) {
                this.attachGPS = attachGPS;
            }
        }
    }

    public List<ServiceComponent> getServiceComponentList() {
        return serviceComponentList;
    }
//
//    @Deprecated
//    public int initializeFromDB_old() {
//        USE_DB = true;
//        dbHelper = new DbHelper(cn);
//        int numConf = dbHelper.numberOfConfigurations();
//        Log.d("Service Manager", "Found in DB " + numConf + " configurations");
//        if (numConf > 0) {
//            //setTransferToDbInterval(MainActivity.INTERVAL_TRANSFER_TO_DB);
//            Cursor cursor;
//            ArrayList<String> array_list = new ArrayList<>();
//            array_list = dbHelper.getAllConfigurationsWithoutOrder();
//            for (int i = 0; i < array_list.size(); i++) {
//                int k = i + 1;
//                cursor = dbHelper.getConfCursorByName(array_list.get(i).toString());
//                cursor.moveToFirst();
//
//                int sensorType = cursor.getInt(cursor.getColumnIndex(DbHelper.Samp_conf_type));
//                int interval = cursor.getInt(cursor.getColumnIndex(DbHelper.Samp_conf_time));
//                int window = cursor.getInt(cursor.getColumnIndex(DbHelper.Samp_conf_window));
//
//                if (!cursor.isClosed()) {
//                    cursor.close();
//                }
//
//                ServiceComponent service = getServiceComponentAvailableBySensorType(sensorType);
//                ServiceComponent.Configuration configuration;
//                configuration = new ServiceComponent.Configuration();
//
//                configuration.setInterval(interval);
//                configuration.setConfigurationName("DAMMI UN NOME");
//                //configuration.setPath("/Path/1");
//                configuration.setPath(service.getDefaultPath());
//
//                configuration.setAttachGPS(true);
//                configuration.setWindow(window);
//
//                service.addConfiguration(configuration); //TODO: Prendere il gps da database
//                service.setActiveConfiguration(configuration);
//                addServiceComponentActive(service); // TODO: Gestire + configurazioni in un servizio
//
//                startScheduleService(service);
//            }
//        }
//
//
//        return 0;
//    }

    public int initializeFromDB() {
        USE_DB = true;
        dbHelper = new DbHelper(cn);

        int numConf = dbHelper.populateServiceComponentListWithAllConfigurations(serviceComponentList);

        Log.d(TAG,"Found " + numConf + " configurations in Db");

        //serviceComponentConfiguredList.clear();

        for (int i = 0; i < getServiceComponentAvailableList().size(); i++) {
            ServiceComponent serviceComponent = getServiceComponentAvailableList().get(i);
            if (serviceComponent.getActiveConfiguration() != null) {
                //addServiceComponentActive(getServiceComponentAvailableList().get(i));
                //startScheduleService(getServiceComponentAvailableList().get(i));
            }
            if (serviceComponent.configurationList.size()>0) {
                //serviceComponentConfiguredList.add(serviceComponent);
            }
        }



        return 0;
    }

//    public void addServiceComponentActive(ServiceComponent serviceComponent) {
//        boolean alreadyExists;
//        if (getServiceComponentActiveBySensorType(serviceComponent.sensorType) == null) {//.getDysplayName() == "NULL") {
//            serviceComponentActiveList.add(serviceComponent);
//        }
//    }

//    public void removeServiceComponentActive(int sensorType) {
//
//        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
//            if (serviceComponentActiveList.get(i).getSensorType() == sensorType) {
//                serviceComponentActiveList.remove(i);
//            }
//        }
//    }

    private int getListIndexFromSensorType (List<ServiceComponent> list, int sensorType){
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSensorType()== sensorType) return i;
        }
        return -1;
    }

    public List<ServiceComponent> getServiceComponentActiveList() {
        List<ServiceComponent> listActive = new ArrayList<>();
        for (int i = 0; i < getServiceComponentAvailableList().size(); i++) {
            if(getServiceComponentAvailableList().get(i).getActiveConfiguration() != null) {
                listActive.add(getServiceComponentAvailableList().get(i));
            }
        }
        return listActive;
    }

    public ServiceComponent getServiceComponentActiveBySensorType(int serviceType) {
        ServiceComponent service = null;
        for (int i = 0; i < getServiceComponentActiveList().size(); i++) {
            service = getServiceComponentActiveList().get(i);
            if (service.getSensorType() == serviceType) {
                return service;
            }
        }
        //ServiceComponent service_NULL = new ServiceComponent("NULL", false);
        //return service_NULL;
        return null;
    }

    public ServiceComponent getServiceComponentAvailableBySensorType(int serviceType) {
        ServiceComponent service = new ServiceComponent("NULL", "NULL", false);
        for (int i = 0; i < getServiceComponentAvailableList().size(); i++) {
            service = getServiceComponentAvailableList().get(i);
            if (service.getSensorType() == serviceType) {
                return service;
            }
        }
        return service;
    }

    public List<ServiceComponent> getServiceComponentConfiguredList() {
        List<ServiceComponent> listConfigured = new ArrayList<>();
        for (int i = 0; i < getServiceComponentAvailableList().size(); i++) {
            if(getServiceComponentAvailableList().get(i).configurationList.size()>0) {
                listConfigured.add(getServiceComponentAvailableList().get(i));
            }
        }
        return listConfigured;
    }

    public List<ServiceComponent> getServiceComponentAvailableList() {
        List<ServiceComponent> mList = new ArrayList<>();
        for (int i = 0; i < serviceComponentList.size(); i++) {
            if (serviceComponentList.get(i).exists) {
                mList.add(serviceComponentList.get(i));
            }
        }
        return mList;
    }

    public List<ServiceComponent> getServiceComponentUnusedList() {
        List<ServiceComponent> mList = new ArrayList<>();
        for (int i = 0; i < serviceComponentList.size(); i++) {
            if ((serviceComponentList.get(i).exists) && (serviceComponentList.get(i).configurationList.size()==0)) {
                mList.add(serviceComponentList.get(i));
            }
        }
        return mList;
    }

//    public ServiceComponent getServiceComponentActiveBySensorName(String name) {
//        ServiceComponent service = new ServiceComponent("NULL", true);
//
//        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
//            if (serviceComponentActiveList.get(i).getDysplayName() == name) {
//                service = serviceComponentActiveList.get(i);
//            }
//        }
//        return service;
//    }

    public int populateServiceComponentList() {
        serviceComponentList.clear();
        int numAvailableServices = 0;

        String MODELNAME = prefs.getString(MainActivity.MODEL_NAME, "NULL");

        // Add activity for all
        serviceComponentList.add(new ServiceComponent("Activity", MODELNAME, true));
        numAvailableServices++;

        // Discovery Components
        int index = -1;

        sensorManager = (SensorManager) cn.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_MAGNETIC_FIELD);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", MODELNAME, false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            serviceComponentList.add(new ServiceComponent("Accelerometer", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_ACCELEROMETER);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Accelerometer", MODELNAME, false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            serviceComponentList.add(new ServiceComponent("Temperature", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Temperature", MODELNAME, false));
        }


        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            serviceComponentList.add(new ServiceComponent("Gyroscope", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_GYROSCOPE);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Gyroscope", MODELNAME, false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            serviceComponentList.add(new ServiceComponent("Light Sensor", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_LIGHT);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Light Sensor", MODELNAME, true));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_PROXIMITY);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", MODELNAME, false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", MODELNAME, true));
            //serviceComponentList.get(numAvailableServices).setModelName(prefs.getString(MainActivity.MODEL_NAME,"NULL"));
            index=getListIndexFromSensorType(serviceComponentList,Sensor.TYPE_PRESSURE);
            if (index > -1) {
                serviceComponentList.get(numAvailableServices).setMinDelay(sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).getMinDelay());
            }
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", MODELNAME, false));
        }

        scanDone = true;
        return numAvailableServices;
    }

    public void setTransferToDbInterval(int sec) {
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        Bundle args = new Bundle();
        args.putBoolean(SensorBackgroundService.KEY_PERFORM_DATABASE_TRANSFER, true);
        intent.putExtras(args);
        PendingIntent scheduledIntent = PendingIntent.getService(cn, 12345, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * sec, scheduledIntent);
    }

    public void stopTransferToDb() {
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        PendingIntent scheduledIntent = PendingIntent.getService(cn, 12345, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.cancel(scheduledIntent);
    }

    public void setDeleteOldDataInterval(int sec) {
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        Bundle args = new Bundle();
        args.putBoolean(SensorBackgroundService.KEY_DELETE_OLD_DATA, true);
        intent.putExtras(args);
        PendingIntent scheduledIntent = PendingIntent.getService(cn, 234, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * sec, scheduledIntent);
    }

    public void startScheduleService(ServiceComponent component) {
        ServiceComponent.Configuration configuration;
        configuration = component.getActiveConfiguration();

        if (prefs.getBoolean("enableGrabbing", false)) {

            AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(cn, SensorBackgroundService.class);
            long interval = 1000; //default value

            Bundle args = new Bundle();

            try {
                args.putBoolean(SensorBackgroundService.KEY_LOGGING, true);
            } catch (Exception e) {
            }
            try {
                args.putInt(SensorBackgroundService.KEY_SENSOR_TYPE, component.getSensorType());
            } catch (Exception e) {
            }
            try {
                args.putInt(SensorBackgroundService.KEY_WINDOW, configuration.getWindow());
                if (component.getSensorType() == SENSOR_TYPE_ACTIVITY) {
                    args.putInt(SensorBackgroundService.KEY_WINDOW, (int) configuration.getInterval()); // Se richhiedo una attivirà passo nella varibile window il sampling time
                }
            } catch (Exception e) {
            }
            try {
                args.putBoolean(SensorBackgroundService.KEY_ATTACH_GPS, configuration.attachGPS);
            } catch (Exception e) {
            }

            try {
                boolean value;
                interval = configuration.getInterval();
                if (interval == 0) { // Interval == 0 per i sensori con streaming attiva lo streaming
                    value = true;
                    args.putBoolean(SensorBackgroundService.KEY_FLUENT_SAMPLING, value);
                }

            } catch (Exception e) {
            }

            intent.putExtras(args);

            // Start the service
            PendingIntent scheduledIntent = PendingIntent.getService(cn, component.getSensorType() + OFFSET_INTENT, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // This doesn't manage streaming wake up after a crash
            /*if (interval == 0) {
                // If streaming is true i need to start the service until stop occurs
                scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),scheduledIntent);
            } else {
                scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), configuration.getInterval(), scheduledIntent);
            }*/
            // This manages streaming wake up after a crash
            if (interval == 0) {
                // If streaming: set wake up after a crash, ten minutes
                interval = 10*60*1000;
            }


            if (!(component.getSensorType() == SENSOR_TYPE_ACTIVITY)) {
                scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, scheduledIntent);
            }else {
                cn.startService(intent);
            }

        }
    }

    public void stopScheduleService(ServiceComponent component) { //TODO finire con fluent
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);

        intent.putExtra(SensorBackgroundService.KEY_SENSOR_TYPE, component.sensorType);

        if (component.getSensorType() == SENSOR_TYPE_ACTIVITY) {
            intent.putExtra(SensorBackgroundService.KEY_WINDOW, 0); // per stoppare l'acquisizione attività
        }
        else {
            intent.putExtra(SensorBackgroundService.KEY_FLUENT_SAMPLING, false);
        }

        PendingIntent scheduledIntent = PendingIntent.getService(cn, component.getSensorType() + OFFSET_INTENT, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if ((component.getActiveConfiguration().getInterval()==0) || (component.getSensorType() == SENSOR_TYPE_ACTIVITY)) {
            cn.startService(intent);
        }
        if (!(component.getSensorType() == SENSOR_TYPE_ACTIVITY)) {
            scheduler.cancel(scheduledIntent);
        }
    }

    public void stopFluentSampling() {
        // Stop fluent sampling for all
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        intent.putExtra(SensorBackgroundService.KEY_FLUENT_SAMPLING, false);
        cn.startService(intent);
    }

    public void stopFluentSampling(ServiceComponent component) {
        // Stop fluent sampling for a specified sensor
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        intent.putExtra(SensorBackgroundService.KEY_FLUENT_SAMPLING, false);
        intent.putExtra(SensorBackgroundService.KEY_SENSOR_TYPE, component.getSensorType());
        cn.startService(intent);
    }

    @Deprecated
    public void addConfigurationServiceToDB_old(ServiceComponent component, ServiceComponent.Configuration configuration) {
        if (USE_DB) {
            dbHelper.newConfiguration(component.getDysplayName(), component.getSensorType(), (int) configuration.getInterval(), "sec", configuration.getWindow(), false);
        }
    }

    public void addOrUpdateConfigurationServiceToDB(ServiceComponent component, ServiceComponent.Configuration configuration, boolean isActive) {
        if (USE_DB) {
//            dbHelper.newConfiguration(component.getDysplayName(), component.getSensorType(), (int) configuration.getInterval(), "sec", configuration.getWindow(), false);
            dbHelper.addOrUpdateConfiguration(configuration, component, isActive);
        }
    }

    public void removeConfigurationServiceToDB(ServiceComponent.Configuration configuration) {
        if (USE_DB) {
            dbHelper.deleteConfigurationById(configuration.getDbId());
        }
    }
    @Deprecated
    public void removeConfigurationServiceToDB_old(ServiceComponent component, ServiceComponent.Configuration configuration) {
        if (USE_DB) {
            dbHelper.deleteConfigurationByName(configuration.getConfigurationName());
        }
    }

    public void syncAllFeedList() {
        new getAllFeed_asynk().execute();
    }

    public void createDeviceFeeds() {
        // Create just the services available for the device
        String model_name = prefs.getString(MainActivity.MODEL_NAME,"NULL");
        createActivitiesFeed(model_name);
        List<ServiceManager.ServiceComponent> list;
        list = getServiceComponentAvailableList();
        for (int i=0; i<list.size(); i++) {
            createServiceFeed(list.get(i), model_name);
        }
    }

    public void createActivitiesFeed(String modelName) {
        createFeed("In_vehicle", "", modelName + PATH_ACTIVITY_IN_VEHICLE, 1);
        createFeed("On_bibycle", "", modelName + PATH_ACTIVITY_ON_BICYCLE, 1);
        createFeed("On_foot", "", modelName + PATH_ACTIVITY_ON_FOOT, 1);
        createFeed("Running", "", modelName + PATH_ACTIVITY_RUNNING, 1);
        createFeed("Still", "", modelName + PATH_ACTIVITY_STILL, 1);
        createFeed("Tilting", "", modelName + PATH_ACTIVITY_TILTING, 1);
        createFeed("Unknown", "", modelName + PATH_ACTIVITY_UNKNOWN, 1);
        createFeed("Walking", "", modelName + PATH_ACTIVITY_WALKING, 1);
        createFeed("Most_probable_activity", "", modelName + PATH_MOST_PROBABLE_ACTIVITY, 1);
        createFeed("Most_probable_activity_string", "", modelName + PATH_MOST_PROBABLE_ACTIVITY + "_string", 3);

    }

    public void createServiceFeed(ServiceComponent component, String modelName) {

        switch (component.getSensorType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                createFeed("Accelerometer_x", "", modelName + "/accelerometer/x", 2);
                createFeed("Accelerometer_y", "", modelName + "/accelerometer/y", 2);
                createFeed("Accelerometer_z", "", modelName + "/accelerometer/z", 2);
                break;
            case (Sensor.TYPE_GYROSCOPE):
                createFeed("Gyroscope_x", "", modelName + "/gyroscope/x", 2);
                createFeed("Gyroscope_y", "", modelName + "/gyroscope/y", 2);
                createFeed("Gyroscope_z", "", modelName + "/gyroscope/z", 2);
                break;
            case (Sensor.TYPE_MAGNETIC_FIELD):
                createFeed("Magnetometer_x", "", modelName + "/magnetometer/x", 2);
                createFeed("Magnetometer_y", "", modelName + "/magnetometer/y", 2);
                createFeed("Magnetometer_z", "", modelName + "/magnetometer/z", 2);
                break;
            case (Sensor.TYPE_LIGHT):
                createFeed("Light", "lux", modelName + "/light", 1);
                break;
            case (Sensor.TYPE_PRESSURE):
                createFeed("Pressure", "hPa", modelName + "/pressure", 1);
                break;
            case (Sensor.TYPE_PROXIMITY):
                createFeed("Proximity", "", modelName + "/proximity", 1);
                break;
            case (Sensor.TYPE_AMBIENT_TEMPERATURE):
                createFeed("Temperature", "", modelName + "/temperature", 1);
                break;
        }
    }

    public void createFeed(String label, String measureUnit, String path, int type) {

        String fullPath = "/" + prefs.getString("username", "NULL") + "/v1/bm/" + path;

        String params[] = new String[4];


        params[0] = label;
        params[1] = measureUnit;
        params[2] = path;
        params[3] = Integer.toString(type);

        new createFeed_asynk().execute(params);
    }

//    private class createFeed_asynk extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... params) {
//            boolean test;
//            API = new SensormindAPI(prefs.getString("username","test_3"), prefs.getString("password","test_3"));
//            allFeedList = API.createFeed(String label, boolean is_static_located, String measure_unit, String s_uid, int type_id)
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            Log.d(TAG, allFeedList.size() + " feeds sync!");
//        }
//
//        @Override
//        protected void onPreExecute() {}
//
//        @Override
//        protected void onProgressUpdate(Void... values) {}
//    }

    private class getAllFeed_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            boolean test;
            API = new SensormindAPI(prefs.getString("username", "test_3"), prefs.getString("password", "test_3"));
            allFeedList = API.getAllFeed();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, allFeedList.size() + " feeds sync!");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class createFeed_asynk extends AsyncTask<String, Void, String> {

        private boolean res;

        @Override
        protected String doInBackground(String... params) {
            boolean test;

            if (prefs.getBoolean("loggedIn", false)) {
                String username = prefs.getString("username", "NULL");
                String password = prefs.getString("password", "NULL");

                API = new SensormindAPI(username, password);
                boolean result;

                // Parametro 0: label
                // Parametro 1: unità misura
                // Parametro 2: s_uid
                // Parametro 3: type_id da int

                res = API.createFeed(params[0], false, params[1], params[2], Integer.valueOf(params[3]));
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Feed Creation: " + res );
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }



}
