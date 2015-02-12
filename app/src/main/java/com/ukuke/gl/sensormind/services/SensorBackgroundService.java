package com.ukuke.gl.sensormind.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ukuke.gl.sensormind.DataDbHelper;
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
    public static final long INTERVAL_UPDATE_LOCATION_MS = 60 * 1000; //[ms]
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
    private boolean attachGPS = true;
    private LocationRequest mLocationRequest; // Se si vuole implementare....


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Bundle args = null;

        int sensorType = -1;// = Sensor.TYPE_LIGHT;

        try {
            args = intent.getExtras();
        }catch (Exception e) {}

        boolean launchSensorAcquisition = false;
        int window = 1;
        boolean fluentSampling = false;

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
                    mSensorManager.unregisterListener(this);
                }
            }


        }



        //TODO: Bisognerebbe aggiornare la posizione in background con asynktask
        // Se voglio aggiungere la posizione ed è passato l'intervallo minimo per l'aggiornamento
        if ((attachGPS) && (System.currentTimeMillis() > (timeOfLastLocationUpdateMs + INTERVAL_UPDATE_LOCATION_MS))) {
            updateLocation();
            timeOfLastLocationUpdateMs = System.currentTimeMillis();
            flush(); // TODO è un test.... se funziona cambia il timer
        }
        ;

        // Se negli extra c'è il tipo di sensore lancia acquisizione sensore
        if (launchSensorAcquisition) {
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    windowAccelerometer = window;
                    fluentSamplingAccelerometer = fluentSampling;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    windowGyroscope = window;
                    fluentSamplingGyroscope = fluentSampling;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    //Log.d(TAG, "Scusi dovrei loggare il magnetometro");
                    windowMagnetometer = window;
                    fluentSamplingMagnetometer = fluentSampling;
                    break;
            }
            Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
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
                    dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_PROXIMITY:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR PROXIMITY: \t" + event.values[0]);
                    dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR TEMPERATURE: \t" + event.values[0]);
                    dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_PRESSURE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR PRESSURE: \t\t" + event.values[0]);
                    dataSample = new DataSample(path, event.values[0], null, null, -1, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR ACCELEROMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterAccelerometer, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR GYROSCOPE: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterGyroscope, System.currentTimeMillis(), lastLatitude, lastLongitude);
                    listDataSample.add(dataSample);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (logging)
                        Log.v(TAG, listDataSample.size() + ": SENSOR MAGNETOMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                    dataSample = new DataSample(path, event.values[0], event.values[1], event.values[2], counterMagnetometer, System.currentTimeMillis(), lastLatitude, lastLongitude);
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
                //.addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        updateLocation();
    }

    public void updateLocation() {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lastLatitude = mLastLocation.getLatitude() + TRUCCA_COORDINATE;
            lastLongitude = mLastLocation.getLongitude() + TRUCCA_COORDINATE * 1.1;
            Log.d(TAG, "New location requested: LAT: " + lastLatitude + " LONG: " + lastLongitude);
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
}