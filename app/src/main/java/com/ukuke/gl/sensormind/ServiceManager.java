package com.ukuke.gl.sensormind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.support.FeedJSON;
import com.ukuke.gl.sensormind.support.SensormindAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gildoandreoni on 20/01/15.
 */

public class ServiceManager {
    // Singleton Class
    SharedPreferences prefs = null;

    private static final String TAG = SensormindAPI.class.getSimpleName();

    private static ServiceManager mInstance = null;
    private List<ServiceComponent> serviceComponentList = new ArrayList<>();
    private List<ServiceComponent> serviceComponentActiveList = new ArrayList<>();

    public List<FeedJSON> allFeedList = new ArrayList<>();

    SensormindAPI API = null;

    SensorManager sensorManager;
    private boolean scanDone = false;
    Context cn;

    private boolean USE_DB = false;
    DbHelper dbHelper;

    ServiceManager(Context cn) {
        this.cn = cn;
        prefs = cn.getSharedPreferences("com.ukuke.gl.sensormind", cn.MODE_PRIVATE);
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
        boolean logging = false;

        Configuration activeConfiguration = null;

        public List<Configuration> configurationList = new ArrayList<>();

        public void setActiveConfiguration(Configuration activeConfiguration) {
            this.activeConfiguration = activeConfiguration;
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

        ServiceComponent(String dysplayName, boolean exists) {
            this.dysplayName = dysplayName;
            this.exists = exists;
            if (this.exists) {
                availableImageID = R.drawable.ic_check_grey600_36dp;
            } else {
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

        public boolean getExists() {
            return exists;
        }

        public Configuration getActiveConfiguration() {
            return activeConfiguration;
        }

        public static class Configuration {
            //TODO inserito dbID, gestirlo in dbHelper

            private long interval = 1000;
            private int window = 2;
            private String configurationName;
            private String path;
            private boolean attachGPS;
            private int dbId;


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

            public boolean isAttachGPS() {
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

    public int initializeFromDB() {
        // TODO: Da implementare
        // TODO: recuperare la feed list da database e salvarla nelle shared preferencies per gli id  (perché nella tabella dei dati invece che utilizzare una stringa per individuare il feed si usa un intero per ridurre la mole di dati)
        USE_DB = true;
        dbHelper = new DbHelper(cn);
        int numConf = dbHelper.numberOfConfigurations();
        Log.d("Service Manager", "Found in DB " + numConf + " configurations");
        if (numConf > 0) {
            //setTransferToDbInterval(MainActivity.INTERVAL_TRANSFER_TO_DB);
            Cursor cursor;
            ArrayList<String> array_list = new ArrayList<>();
            array_list = dbHelper.getAllConfigurationsWithoutOrder();
            for (int i = 0; i < array_list.size(); i++) {
                int k = i + 1;
                cursor = dbHelper.getConfCursorByName(array_list.get(i).toString());
                cursor.moveToFirst();

                int sensorType = cursor.getInt(cursor.getColumnIndex(dbHelper.Samp_conf_type));
                int interval = cursor.getInt(cursor.getColumnIndex(dbHelper.Samp_conf_time));
                int window = cursor.getInt(cursor.getColumnIndex(dbHelper.Samp_conf_window));

                if (!cursor.isClosed()) {
                    cursor.close();
                }

                ServiceComponent service = getServiceComponentAvailableBySensorType(sensorType);
                ServiceComponent.Configuration configuration;
                configuration = new ServiceComponent.Configuration();

                configuration.setInterval(interval);
                configuration.setConfigurationName("DAMMI UN NOME");
                configuration.setPath("/Path/1");
                configuration.setAttachGPS(true);
                configuration.setWindow(window);

                service.addConfiguration(configuration); //TODO: Prendere il gps da database
                service.setActiveConfiguration(configuration);
                addServiceComponentActive(service); // TODO: Gestire + configurazioni in un servizio

                startScheduleService(service);
            }
        }


        return 0;
    }

    public void addServiceComponentActive(ServiceComponent serviceComponent) {
        boolean alreadyExists;
        if (getServiceComponentActiveBySensorType(serviceComponent.sensorType).getDysplayName() == "NULL") {
            serviceComponentActiveList.add(serviceComponent);
        }
    }

    public void removeServiceComponentActive(int sensorType) {

        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
            if (serviceComponentActiveList.get(i).getSensorType() == sensorType) {
                serviceComponentActiveList.remove(i);
            }
        }
    }

    public List<ServiceComponent> getServiceComponentActiveList() {
        return serviceComponentActiveList;
    }

    public ServiceComponent getServiceComponentActiveBySensorType(int serviceType) {
        ServiceComponent service = new ServiceComponent("NULL", false);
        for (int i = 0; i < serviceComponentActiveList.size(); i++) {
            service = serviceComponentActiveList.get(i);
            if (service.getSensorType() == serviceType) {
                return service;
            }
        }
        ServiceComponent service_NULL = new ServiceComponent("NULL", false);
        return service_NULL;
    }

    public ServiceComponent getServiceComponentAvailableBySensorType(int serviceType) {
        ServiceComponent service = new ServiceComponent("NULL", false);
        for (int i = 0; i < getServiceComponentAvailableList().size(); i++) {
            service = getServiceComponentAvailableList().get(i);
            if (service.getSensorType() == serviceType) {
                return service;
            }
        }
        return service;
    }

    public List<ServiceComponent> getServiceComponentAvailableList() {
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

    public ServiceComponent getServiceComponentActiveBySensorName(String name) {
        ServiceComponent service = new ServiceComponent("NULL", true);

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

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Magnetic Field", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            serviceComponentList.add(new ServiceComponent("Accelerometer", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Accelerometer", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            serviceComponentList.add(new ServiceComponent("Temperature", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Temperature", false));
        }


        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            serviceComponentList.add(new ServiceComponent("Gyroscope", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Gyroscope", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Light Sensor", true));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Proximity Sensor", false));
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", true));
            numAvailableServices++;
        } else {
            serviceComponentList.add(new ServiceComponent("Pressure Sensor", false));
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

    public void startScheduleService(ServiceComponent component) {
        ServiceComponent.Configuration configuration;
        configuration = component.getActiveConfiguration();

        if (prefs.getBoolean("enableGrabbing", true)) {

            AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(cn, SensorBackgroundService.class);

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
            } catch (Exception e) {
            }
            try {
                args.putBoolean(SensorBackgroundService.KEY_ATTACH_GPS, configuration.attachGPS);
            } catch (Exception e) {
            }

            intent.putExtras(args);

            // Start the service

            PendingIntent scheduledIntent = PendingIntent.getService(cn, component.getSensorType(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), configuration.getInterval(), scheduledIntent);
        }
    }

    public void stopScheduleService(ServiceComponent component) {
        AlarmManager scheduler = (AlarmManager) cn.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(cn, SensorBackgroundService.class);
        PendingIntent scheduledIntent = PendingIntent.getService(cn, component.getSensorType(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.cancel(scheduledIntent);
    }

    public void addConfigurationServiceToDB(ServiceComponent component, ServiceComponent.Configuration configuration) {
        if (USE_DB) {
            dbHelper.newConfiguration(component.getDysplayName(), component.getSensorType(), (int) configuration.getInterval(), "sec", configuration.getWindow(), false);
        }
    }

    public void removeConfigurationServiceToDB(ServiceComponent component, ServiceComponent.Configuration configuration) {
        if (USE_DB) {
            // TODO: Aggiungere anche il tipo di sensore!
            dbHelper.deleteConfigurationByName(configuration.getConfigurationName());
        }
    }

    public void syncAllFeedList() {
        new getAllFeed_asynk().execute();
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
