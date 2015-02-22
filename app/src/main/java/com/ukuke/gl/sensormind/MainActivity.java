package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import android.os.Handler;
import android.os.Message;

import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.services.MQTTService;
import com.ukuke.gl.sensormind.support.AboutActivity;
import com.ukuke.gl.sensormind.support.DeviceInfo;

import android.provider.Settings.Secure;


import java.util.List;

public class MainActivity extends Activity {

    SharedPreferences prefs = null;
    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    public int INTERVAL_TRANSFER_TO_DB; //[sec]
    public int INTERVAL_DELETE_SENT_DATA; //[sec]
    public int INTERVAL_TRANSFER_TO_SENSORMIND; //[sec]
    public static final String URL_BROWSER_SENSORMIND = "http://137.204.213.190/it";
    public static final String IP_MQTT = "137.204.213.190";
    public static final int PORT_MQTT = 1884;
    public static String MODEL = android.os.Build.MODEL.replaceAll("\\s","");
    public static String MODEL_NAME; //TODO passare a shared preferences, sia qui che in tutti i posti dove viene usato, GILDOCULOECULOCHINONCLODICECULO!!!!!
    public static final boolean MANAGE_MULTIPLE_CONFIGURATION = false;//TODO finire di implemetare l'utilizzo di questa variabile per differenziare la gestione a singola configurazione o configurazioni multiple dentro a configuration activity
    private static long back_pressed;
    public static final boolean HEAVY_LOG = false;
    public long LAST_SCHEDULE_DELETE;
    public static String ANDROID_ID;
    private ListView listView = null;

    String username;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check shared preferences
        View v = new View(this);
        initEverything();
        ANDROID_ID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        MODEL_NAME = MODEL + "_" + ANDROID_ID;
        Log.d(TAG, "Device identifier: " + MODEL_NAME);
    }

    private void initEverything() {
        listView = (ListView) findViewById(R.id.listViewMain);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ServiceManager.getInstance(MainActivity.this).initializeFromDB();
        // Get credentials if stored on shared preferences
        username = prefs.getString("username", "NULL");
        password = prefs.getString("password", "NULL");
        INTERVAL_DELETE_SENT_DATA = Integer.parseInt(prefs.getString("dbFrequency", "1800"));
        INTERVAL_TRANSFER_TO_SENSORMIND = Integer.parseInt(prefs.getString("syncFrequency", "300"));
        if (INTERVAL_TRANSFER_TO_SENSORMIND <= 300) {
            INTERVAL_TRANSFER_TO_DB = INTERVAL_TRANSFER_TO_SENSORMIND - 2; //set the interval as a bit less of the other interval
        } else {
            INTERVAL_TRANSFER_TO_DB = 300;
        }

        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(prefs.getBoolean("enableGrabbing", true)); //TODO SICURO A TRUE!=??!?!?
        toggle.setVisibility(View.GONE);
        prefs.edit().putBoolean("enableGrabbing", true).apply();
        prefs.edit().putString("ip_MQTT",IP_MQTT).apply();
        prefs.edit().putInt("port_MQTT",PORT_MQTT).apply();
        prefs.edit().putBoolean("HEAVY_LOG", HEAVY_LOG).apply();

        LAST_SCHEDULE_DELETE = prefs.getLong("last_delete",-1);
        if (LAST_SCHEDULE_DELETE == -1 || (System.currentTimeMillis()-LAST_SCHEDULE_DELETE)>(INTERVAL_DELETE_SENT_DATA*1000)) {
            // Delete non yet scheduled or current - last is greater than interval
            // Schedule it now
            ServiceManager.getInstance(this)
                    .setDeleteOldDataInterval(INTERVAL_DELETE_SENT_DATA);
            prefs.edit().putLong("last_delete",System.currentTimeMillis()).apply();
        }

        if (prefs.getBoolean("loggedIn",false)) {
            launchMQTTService();
        }

        ServiceManager.getInstance(MainActivity.this).setTransferToDbInterval(INTERVAL_TRANSFER_TO_DB);
        //Log.d(TAG,"I have set interval transfer to db to [sec]: "+ INTERVAL_TRANSFER_TO_DB);

        if ((prefs.getBoolean("loggedIn",false)) && (prefs.getBoolean("enableGrabbing",false))) {
            startScheduleAllActiveServices();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item_login = menu.findItem(R.id.action_log_in);
        MenuItem item_logout = menu.findItem(R.id.action_logout);

        item_login.setVisible(!prefs.getBoolean("loggedIn",false));
        item_logout.setVisible(prefs.getBoolean("loggedIn", false));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_device_capabilities) {

            Intent intent = new Intent(this, DeviceCapabilitiesActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_log_in) {
            Intent intent = new Intent(this, LogInActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_logout) {
            prefs.edit().putBoolean("loggedIn", false).apply();
            prefs.edit().commit();
            stopMQTTService();
        }
        else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickedToggle(View view) {
//        ToggleButton toggleButton;
//        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
//
//        prefs.edit().putBoolean("enableGrabbing", toggleButton.isChecked()).apply();
//
//        if (toggleButton.isChecked()) {
//            startScheduleAllActiveServices();
//            //launchMQTTService();
//            ServiceManager.getInstance(MainActivity.this).setTransferToDbInterval(INTERVAL_TRANSFER_TO_DB);
//        }
//        else {
//            // STOP all schedules
//            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            //Intent intent = new Intent(this, MQTTService.class);
//            //PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            //scheduler.cancel(scheduledIntent);
//
//            ServiceManager.getInstance(MainActivity.this).stopFluentSampling();
//
//            //stopMQTTService();
//
//            ServiceManager.getInstance(MainActivity.this).stopTransferToDb();
//            for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
//                ServiceManager.ServiceComponent service;
//                service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
//                ServiceManager.getInstance(MainActivity.this).stopScheduleService(service);
//            }
//            if (prefs.getBoolean("HEAVY_LOG",false)) {
//                if (ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size() > 0) {
//                    Toast.makeText(this, "Acquisition stopped", Toast.LENGTH_LONG).show();
//                }
//            }
//            //stopService(new Intent(this, SensorBackgroundService.class));
//        }
//
//
    }

    public void startScheduleAllActiveServices() {
        for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
            ServiceManager.ServiceComponent service;
            service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
            ServiceManager.getInstance(MainActivity.this).startScheduleService(service);
        }
        if (ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size() > 0) {
            //Toast.makeText(this, "Acquisition started", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (back_pressed + 2000 > System.currentTimeMillis()) super.onBackPressed();
        else Toast.makeText(getBaseContext(), "Press once again to exit!", Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            Log.i("MainActivity","This is a first run. Set up everything!");
            prefs.edit().putBoolean("firstrun", false).apply();
            Intent intent = new Intent(this, DisclaimerActivity.class);
            startActivity(intent);
        }
        else {
            //Log.v("MainActivity", "This is not a first run. Let's continue");
        }

        populateListView();
        registerClickCallback();
    }

    public void onPlusCircleClicked(View view) {
        Intent intent = new Intent(this, AddDeviceActivity.class);
        startActivity(intent);
    }

    private void registerClickCallback() {
        //ListView list = (ListView) findViewById(R.id.listViewMain);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                // Go to Configuration Activity to modify current active configuration
                Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
                intent.putExtra(AddDeviceActivity.TYPE_SENSOR, ServiceManager.getInstance(MainActivity.this).getServiceComponentConfiguredList().get(position).getSensorType());
                //intent.putExtra(AddDeviceActivity.ENABLES_SENSOR, false);
                intent.putExtra(AddDeviceActivity.MODIFY_CONFIGURATION, true);
                intent.putExtra(ConfigurationActivity.CONFIGURATION_DB_ID, ServiceManager.getInstance(MainActivity.this).getServiceComponentConfiguredList().get(position).configurationList.get(0).getDbId());
                startActivity(intent);
            }


        });
    }

    private void populateListView() {
        ArrayAdapter<ServiceManager.ServiceComponent> adapter = new MyListAdapter();
        //ListView list = (ListView) findViewById(R.id.listViewMain);
        listView.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(MainActivity.this, R.layout.item_view_check, ServiceManager.getInstance(MainActivity.this).getServiceComponentConfiguredList());
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view_check, parent, false);
            }

            final ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance(MainActivity.this).getServiceComponentConfiguredList().get(position);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getComponentImageID());

            final ToggleButton toggleButton = (ToggleButton) itemView.findViewById(R.id.item_toggle);

            if (currentServiceComponent.getActiveConfiguration() != null) {
                toggleButton.setChecked(true);
            }
            else {
                toggleButton.setChecked(false);
            }

            toggleButton.setOnClickListener( new View.OnClickListener() {
                                                 @Override
                                                 public void onClick(View v) {
                                                     ServiceManager.ServiceComponent.Configuration configuration = currentServiceComponent.configurationList.get(0);
                                                     if (toggleButton.isChecked()) {
                                                         currentServiceComponent.setActiveConfiguration(configuration);
                                                         //ServiceManager.getInstance(MainActivity.this).addServiceComponentActive(currentServiceComponent);
                                                         ServiceManager.getInstance(MainActivity.this).startScheduleService(currentServiceComponent);
                                                     } else {
                                                         ServiceManager.getInstance(MainActivity.this).stopScheduleService(currentServiceComponent);
                                                         currentServiceComponent.setActiveConfiguration(null);
                                                     }
                                                     ServiceManager.getInstance(MainActivity.this).addOrUpdateConfigurationServiceToDB(currentServiceComponent,configuration, toggleButton.isChecked());
                                                 }
                                             }
            );

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
        }

    }

    private void launchMQTTService() {
        if (prefs.getBoolean("loggedIn",false)) {
            Log.d(TAG, "Activate Mqtt service");
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MQTTService.class);

            PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL_TRANSFER_TO_SENSORMIND * 1000, scheduledIntent);
        }
        else {
            Log.d(TAG,"You need to login or register before send data via MQTT");
        }
    }

    private void stopMQTTService() {
        //if (prefs.getBoolean("loggedIn",false)) {
        //Log.d(TAG, "Deactivate Mqtt service");
        stopService(new Intent(this, MQTTService.class));
        //}
    }

    /*private void descheduleMQTTService () {
        Log.d(TAG, "Deschedule Mqtt service");
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MQTTService.class);
        //TODO finire qua
        PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL_TRANSFER_TO_SENSORMIND * 1000, scheduledIntent);
    }
*/
    public class MyReceiver extends BroadcastReceiver {
        //TODO Istanziare all'avvio del telefono
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Launch services after boot");
            initEverything();
        }
    }
}
