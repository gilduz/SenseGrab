package com.ukuke.gl.sensormind.services;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.ActivityRecognitionResultCreator;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ukuke.gl.sensormind.DataDbHelper;
import com.ukuke.gl.sensormind.MainActivity;
import com.ukuke.gl.sensormind.ServiceManager;
import com.ukuke.gl.sensormind.support.DataSample;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class SensorBackgroundService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String KEY_SENSOR_TYPE = "sensor_type";
    public static final String KEY_LOGGING = "logging";
    public static final String KEY_WINDOW = "num_samples";
    public static final String KEY_ATTACH_GPS = "attach_gps";
    public static final String KEY_PERFORM_DATABASE_TRANSFER = "perform_database_transfer";
    public static final String KEY_FLUENT_SAMPLING = "fluent_sampling";
    public static final String KEY_DELETE_OLD_DATA = "delete_old_data";

    public static final long INTERVAL_UPDATE_LOCATION_MS = 120 * 1000; //[ms]
    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    int counterAccelerometer = 0;
    int counterGyroscope = 0;
    int counterMagnetometer = 0;
    int windowAccelerometer = 1;
    int windowGyroscope = 1;
    int windowMagnetometer = 1;
    boolean fluentSamplingAccelerometer = false;
    boolean fluentSamplingGyroscope = false;
    boolean fluentSamplingMagnetometer = false;
    private SensorManager mSensorManager = null;
    private boolean logging = false;
    private long timeOfLastLocationUpdateMs = 0;
    private List<DataSample> listDataSample = new ArrayList<>();
    private DataDbHelper dataDbHelper = null;
    private double TRUCCA_COORDINATE = 0.9123456;
    private Double lastLatitude;
    private Double lastLongitude;
    private boolean attachGPS_acc = false;
    private boolean attachGPS_gyro = false;
    private boolean attachGPS_magn = false;
    private boolean attachGPS_light = false;
    private boolean attachGPS_proximity = false;
    private boolean attachGPS_pressure = false;
    private boolean attachGPS_temperature = false;
    private SharedPreferences prefs;
    private LocationRequest mLocationRequest; // Se si vuole implementare....
    private MyResultReceiver resultReceiver;
    private long lastTimeActivity = System.currentTimeMillis();
    private long intervalActivity = 30 * 1000;
    private boolean enableActivity = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle args = null;

        int sensorType = -1;// = Sensor.TYPE_LIGHT;

        try {
            args = intent.getExtras();
        }catch (Exception e) {}

        boolean launchSensorAcquisition = false;
        int window = 1;
        boolean fluentSampling = false;
        boolean attachGPS = false;


        // get some properties from the intent
        if (args != null) {
            if (args.containsKey(KEY_SENSOR_TYPE)) {
                sensorType = args.getInt(KEY_SENSOR_TYPE);
                launchSensorAcquisition = true;
            }
            if (args.containsKey(KEY_WINDOW)) {
                window = args.getInt(KEY_WINDOW);
            }
            if (args.containsKey(KEY_ATTACH_GPS)) {
                attachGPS = args.getBoolean(KEY_ATTACH_GPS);
            }
            if (args.containsKey(KEY_LOGGING)) {
                logging = args.getBoolean(KEY_LOGGING);
            }
            if (args.containsKey(KEY_PERFORM_DATABASE_TRANSFER)) {
                if (args.getBoolean(KEY_PERFORM_DATABASE_TRANSFER)) {
                    new saveListSampleOnDb().execute();
                }
            }
            if (args.containsKey(KEY_FLUENT_SAMPLING)) {
                fluentSampling = args.getBoolean(KEY_FLUENT_SAMPLING);
                if (!fluentSampling) {
                    // Deregistro tutti i sensori (soprattutto per acc magn e gyro)
                    mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(sensorType));
                }
            }
            if (args.containsKey(KEY_DELETE_OLD_DATA)) {
                if (args.getBoolean(KEY_DELETE_OLD_DATA)) {
                    new deleteOldDataFromDb().execute();
                }
            }

        }

        // Se voglio aggiungere la posizione ed è passato l'intervallo minimo per l'aggiornamento
        if ((attachGPS) && (System.currentTimeMillis() > (timeOfLastLocationUpdateMs + INTERVAL_UPDATE_LOCATION_MS))) {
            updateLocation();
            timeOfLastLocationUpdateMs = System.currentTimeMillis();
        }
        ;

        // Se negli extra c'è il tipo di sensore lancia acquisizione sensore
        if (launchSensorAcquisition) {
            switch (sensorType) {
                case ServiceManager.SENSOR_TYPE_ACTIVITY:
                    intervalActivity = window;
                    if (window == 0) { // TODO ma usare interval e creare un altro metodo da chiamare apposta per stoppare no?
                        enableActivity = false;
                        deActivateActivityRecognition();
                    }
                    else {
                        enableActivity = true;
                        activateActivityRecognition(window); // TODO e se non sono ancora connesso!??!?!
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    attachGPS_acc = attachGPS;
                    windowAccelerometer = window;
                    fluentSamplingAccelerometer = fluentSampling;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    attachGPS_gyro = attachGPS;
                    windowGyroscope = window;
                    fluentSamplingGyroscope = fluentSampling;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    attachGPS_magn = attachGPS;
                    windowMagnetometer = window;
                    fluentSamplingMagnetometer = fluentSampling;
                    break;
                case Sensor.TYPE_LIGHT:
                    attachGPS_light = attachGPS;
                    break;
                case Sensor.TYPE_PRESSURE:
                    attachGPS_pressure = attachGPS;
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    attachGPS_temperature = attachGPS;
                    break;
                case Sensor.TYPE_PROXIMITY:
                    attachGPS_proximity = attachGPS;
                    break;
            }
            Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }


        return START_STICKY;
    }

    @TargetApi(19)
    private void flush() {
        mSensorManager.flush(this);
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
        addDataSampleToList(event);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                counterAccelerometer++;
                if (counterAccelerometer >= windowAccelerometer) {
                    counterAccelerometer = 0;
                }
                if (!fluentSamplingAccelerometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                counterGyroscope++;
                if (counterGyroscope >= windowGyroscope) {
                    counterGyroscope = 0;
                }
                if (!fluentSamplingGyroscope) {
                    mSensorManager.unregisterListener(this, event.sensor);
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                counterMagnetometer++;
                if (counterMagnetometer >= windowMagnetometer) {
                    counterMagnetometer = 0;
                }
                if (!fluentSamplingMagnetometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                }

                break;
            default:
                mSensorManager.unregisterListener(this, event.sensor);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timeOfLastLocationUpdateMs = System.currentTimeMillis();
        buildGoogleApiClient();
        updateLocation();
        dataDbHelper = new DataDbHelper(this);
        mGoogleApiClient.connect();

        IntentFilter filter = new IntentFilter(ActivityRecognitionIntentService.KEY_BROADCAST_RESULT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        resultReceiver = new MyResultReceiver();
        registerReceiver(resultReceiver, filter);
    }

    public synchronized void addDataSampleToList(SensorEvent event) {
        DataSample dataSample;
        ServiceManager.ServiceComponent.Configuration conf;
        try {
            ServiceManager.ServiceComponent component = ServiceManager.getInstance(SensorBackgroundService.this).getServiceComponentActiveBySensorType(event.sensor.getType());
            conf = component.getActiveConfiguration();
            String path = conf.getPath();
            //String path = component.getDefaultPath();//TODO Sistemare path configurazione... non lo trova

            switch (event.sensor.getType()) {
                case Sensor.TYPE_LIGHT:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR LIGHT: \t\t\t" + event.values[0]);
                    if (attachGPS_light)
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_PROXIMITY:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR PROXIMITY: \t" + event.values[0]);
                    if (attachGPS_proximity)
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR TEMPERATURE: \t" + event.values[0]);
                    if (attachGPS_temperature)
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_PRESSURE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR PRESSURE: \t\t" + event.values[0]);
                    if (attachGPS_pressure)
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR ACCELEROMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    if (attachGPS_acc)
                        dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterAccelerometer, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterAccelerometer, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR GYROSCOPE: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    if (attachGPS_gyro)
                        dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterGyroscope, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    else
                        dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterGyroscope, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR MAGNETOMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    if (attachGPS_magn)
                        dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterMagnetometer, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterMagnetometer, System.currentTimeMillis(), null, null);
                    listDataSample.add(dataSample);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        new saveListSampleOnDb().execute();

        //Toast.makeText(this, "SensorBackgroundService Destroyed", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"SensorBackgroundService Destroyed... all data is on Db");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        updateLocation();
        if (enableActivity) {
            activateActivityRecognition(30000);
        }
    }

    private void activateActivityRecognition(long interval) {
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, interval, mActivityRecognitionPendingIntent);
    }

    private void deActivateActivityRecognition() {
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityRecognitionPendingIntent);
    }

    public void updateLocation() {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lastLatitude = mLastLocation.getLatitude() + TRUCCA_COORDINATE;
            lastLongitude = mLastLocation.getLongitude() + TRUCCA_COORDINATE * 1.1;
            if (prefs.getBoolean("HEAVY_LOG",false)) {
                Log.d(TAG, "New location requested: LAT: " + lastLatitude + " LONG: " + lastLongitude);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO: Se la connessione ai servizi google fallisce....che si fa?
    }

    private class saveListSampleOnDb extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // Call saveListFeedOnDB somethimes to transfer data on database
            if (listDataSample.size() > 0) {
                dataDbHelper.insertListOfData(listDataSample);
                listDataSample.clear();
                Log.d(TAG, "Transferred data to DB. Now db has " + dataDbHelper.numberOfEntries() + " entries with " + dataDbHelper.numberOfUnsentEntries() + " unsent samples");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //Log.i(TAG, "Sensormind Sync completed");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class deleteOldDataFromDb extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // Call delete old data from db
            Long time = System.currentTimeMillis() -
                    (Integer.parseInt(prefs.getString("dbFrequency","1800")) * 1000); //timestamp in millis
            int deleted = dataDbHelper.deleteSentDataSamplesBeforeTimestamp(time);
            //Log.d(TAG,"Current: "+System.currentTimeMillis()+" - "+prefs.getString("dbFrequency","1800"));
            Log.d(TAG, "Deleted from db " + deleted + " sent samples before "+time);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //Log.i(TAG, "Sensormind Sync completed");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    public class MyResultReceiver extends BroadcastReceiver {


        // RECEIVER TO MANAGE ACTIVITY RECOGNITION
        public static final String KEY_BROADCAST_RESULT = "com.ukuke.gl.sensormind.intent.action.PROCESS_RESPONSE";


        @Override
        public void onReceive(Context context, Intent intent) {

            if ((System.currentTimeMillis() - lastTimeActivity) < intervalActivity) {
                return;
            }

            Bundle args = intent.getExtras();

            //if (args.containsKey(ActivityRecognitionIntentService.KEY_MOST_PROBABLE_ACTIVITY)) {
            //String supp = args.getString(ActivityRecognitionIntentService.KEY_MOST_PROBABLE_ACTIVITY);
            int supp = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_IN_VEHICLE);

            int activity_in_vehicle = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_IN_VEHICLE);
            int activity_on_bicycle = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_ON_BICYCLE);
            int activity_on_foot = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_ON_FOOT);
            int activity_running = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_RUNNING);
            int activity_still = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_STILL);
            int activity_tilting = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_TILTING);
            int activity_unknown = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_UNKNOWN);
            int activity_walking = args.getInt(ActivityRecognitionIntentService.KEY_ACTIVITY_WALKING);
            int most_probable_activity = args.getInt(ActivityRecognitionIntentService.KEY_MOST_PROBABLE_ACTIVITY);

            long time = System.currentTimeMillis();

            DataSample dataSample;
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_IN_VEHICLE, (float)activity_in_vehicle, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_ON_BICYCLE, (float)activity_on_bicycle, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_ON_FOOT, (float)activity_on_foot, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_RUNNING, (float)activity_running, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_STILL, (float)activity_still, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_TILTING, (float)activity_tilting, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_UNKNOWN, (float)activity_unknown, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_ACTIVITY_WALKING, (float)activity_walking, null, null, -1, time, null, null);
            listDataSample.add(dataSample);
            dataSample = new DataSample(MainActivity.MODEL_NAME + ServiceManager.PATH_MOST_PROBABLE_ACTIVITY, (float)most_probable_activity, null, null, -1, time, null, null);
            listDataSample.add(dataSample);

            Log.v(TAG,"Acquired activity: " + supp);
        }
    }
}