package com.ukuke.gl.sensormind.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationServices;
import com.ukuke.gl.sensormind.DataDbHelper;
import com.ukuke.gl.sensormind.DbHelper;
import com.ukuke.gl.sensormind.support.DataSample;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;

/**
 * for a background service not linked to an activity it's important to use the command approach
 * instead of the Binder. For starting use the alarm manager
 */
public class SensorBackgroundService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    private SensorManager mSensorManager = null;
    private boolean logging = false;

    public static final String KEY_SENSOR_TYPE = "sensor_type";
    public static final String KEY_LOGGING = "logging";
    public static final String KEY_WINDOW = "num_samples";
    public static final String KEY_ATTACH_GPS = "attach_gps";
    public static final String KEY_PERFORM_DATABASE_TRANSFER = "perform_database_transfer";

    public static final long INTERVAL_UPDATE_LOCATION_MS = 60 * 1000; //[ms]
    private long timeOfLastLocationUpdateMs = 0;

    private List<DataSample> listDataSample = new ArrayList<>();
    private DataDbHelper dataDbHelper = null;

    private int lastStartId;

    private Double lastLatitude;
    private Double lastLongitude;
    private boolean attachGPS = true;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    int counterAccelerometer = 0;
    int counterGyroscope = 0;
    int counterMagnetometer = 0;

    int windowAccelerometer = 1;
    int windowGyroscope = 1;
    int windowMagnetometer = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        lastStartId = startId;

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // have a default sensor configured
        int sensorType = Sensor.TYPE_LIGHT;

        Bundle args = intent.getExtras();

        boolean launchSensorAcquisition = false;
        int window = 1;


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
                    saveListSampleOnDb();
                };
            }
        }

        //TODO: Bisognerebbe aggiornare la posizione in background con asynktask
        // Se voglio aggiungere la posizione ed è passato l'intervallo minimo per l'aggiornamento
        if ((attachGPS) && (System.currentTimeMillis() > (timeOfLastLocationUpdateMs + INTERVAL_UPDATE_LOCATION_MS))) {
            updateLocation();
            timeOfLastLocationUpdateMs = System.currentTimeMillis();
        };

        // Se negli extra c'è il tipo di sensore lancia acquisizione sensore
        if (launchSensorAcquisition) {
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER: windowAccelerometer = window; break;
                case Sensor.TYPE_GYROSCOPE: windowGyroscope = window; break;
                case Sensor.TYPE_MAGNETIC_FIELD: windowMagnetometer = window; break;
            }
            Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
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

        addDataSampleToList(event);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                counterAccelerometer++;
                if (counterAccelerometer >= windowAccelerometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterAccelerometer = 0;
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                counterGyroscope++;
                if (counterGyroscope >= windowGyroscope) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterGyroscope = 0;
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                counterMagnetometer++;
                if (counterMagnetometer >= windowMagnetometer) {
                    mSensorManager.unregisterListener(this, event.sensor);
                    counterMagnetometer = 0;
                }
                break;
            default:
                mSensorManager.unregisterListener(this, event.sensor);
        }

    }

    // Call saveListFeedOnDB somethimes to transfer data on database
    public synchronized int saveListSampleOnDb() {
        int dataTransferred = 0;
        // TODO: Leo qui è dove richiamo il tuo metodo del DB passandogli listDataSample
        if (listDataSample.size()>0) {
            //dataDbHelper.insertListData(listDataSample);
            dataTransferred = listDataSample.size();
            listDataSample.clear();
            Log.d(TAG, "Transferred data to DB. Now db has entries");
        }
        return dataTransferred;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timeOfLastLocationUpdateMs = System.currentTimeMillis();
        buildGoogleApiClient();
        updateLocation();
        dataDbHelper = new DataDbHelper(this);
    }

    public synchronized void addDataSampleToList(SensorEvent event) {

        DataSample dataSample;
        dataSample = new DataSample(event.sensor.getName(), event.values[1], null, null, 1, event.timestamp*1000, lastLongitude, lastLongitude);
        listDataSample.add(dataSample);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                if (logging)
                    Log.d(TAG, listDataSample.size() + ": SENSOR LIGHT: \t\t\t" + event.values[0]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], null, null, 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_PROXIMITY:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR PROXIMITY: \t" + event.values[0]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], null, null, 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR TEMPERATURE: \t" + event.values[0]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], null, null, 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_PRESSURE:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR PRESSURE: \t\t" + event.values[0]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], null, null, 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR ACCELEROMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], event.values[1], event.values[2], 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR GYROSCOPE: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], event.values[1], event.values[2], 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (logging)
                    Log.d(TAG, listDataSample.size() +  ": SENSOR MAGNETOMETER: \t" + event.values[0] + " \t " + event.values[1] + " \t " + event.values[2]);
                dataSample = new DataSample(event.sensor.getName(), event.values[0], event.values[1], event.values[2], 1, event.timestamp*1000, lastLongitude, lastLongitude);
                listDataSample.add(dataSample);
                break;
        }

        // Print to log the location
        if (attachGPS) {
            updateLocation();
            Log.d(TAG, "Location: LAT " + lastLatitude + " LONG " + lastLongitude);
        };
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "SensorBackgroundService Stopped", Toast.LENGTH_LONG).show();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        updateLocation();
    }

    public void updateLocation() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lastLatitude = mLastLocation.getLatitude();
            lastLongitude = mLastLocation.getLongitude();
        }
        else {
            Log.d(TAG, "Location NULL");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO: Se la connessione fallisce....che si fa?
    }

}