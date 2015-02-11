package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
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

public class MainActivity extends Activity {
//    public class MainActivity extends ActionBarActivity {

    SharedPreferences prefs = null;
    boolean toggleGrabbingEnabled = true;
    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    public static final int INTERVAL_TRANSFER_TO_DB = 1 * 60; //[sec]
    public static final int INTERVAL_TRANSFER_TO_SENSORMIND = 2 * 60; //[sec]
    public static final String IP_MQTT = "137.204.213.190";
    public static final int PORT_MQTT = 1884;
    public static final String MODEL_NAME = android.os.Build.MODEL.replaceAll("\\s","");
    public static final boolean MANAGE_MULTIPLE_CONFIGURATION = false;//TODO finire di implemetare l'utilizzo di questa variabile per differenziare la gestione a singola configurazione o configurazioni multiple dentro a configuration activity
    private static long back_pressed;
    String username;
    String password;

    // MQTT
    private Messenger service = null;
    private final Messenger serviceHandler = new Messenger(new ServiceHandler());
    private IntentFilter intentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Carica le impostazioni da database impostazioni
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check shared preferences
        View v = new View(this);

        initEverything();
    }

    private void initEverything() {
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);
        ServiceManager.getInstance(MainActivity.this).initializeFromDB();
        // Get credentials if stored on shared preferences
        username = prefs.getString("username", "NULL");
        password = prefs.getString("password", "NULL");
        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(prefs.getBoolean("enableGrabbing", false));
        prefs.edit().putString("ip_MQTT",IP_MQTT).apply();
        prefs.edit().putInt("port_MQTT",PORT_MQTT).apply();
        prefs.edit().commit();
        //createAllFeeds();



        if (prefs.getBoolean("enableGrabbing",false) && prefs.getBoolean("loggedIn",false)) {
            launchMQTTService();
            ServiceManager.getInstance(MainActivity.this).setTransferToDbInterval(INTERVAL_TRANSFER_TO_DB);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        else if (id == R.id.action_test) {
            //API = new SensormindAPI(prefs.getString("username","test_3"),

            createAllFeeds();

            // prefs.getString("password","test_3"));
           // ServiceManager.getInstance(MainActivity.this).syncAllFeedList();
            Toast.makeText(getApplicationContext(), "THIS WAS A TEST", Toast.LENGTH_LONG).show();
            //AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }

        else if (id == R.id.action_settings) {
           // TODO Da aggiungere una activity settings semplice
           Intent intent = new Intent(this, SettingsActivity.class);
           startActivity(intent);
        }

        else if (id == R.id.action_log_in) {
            Intent intent = new Intent(this, LogInActivity.class);
            startActivity(intent);
        }

        else if (id == R.id.action_logout) {
            prefs.edit().putBoolean("loggedIn",false);
            stopMQTTService();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickedToggle(View view) {
        ToggleButton toggleButton;
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        prefs.edit().putBoolean("enableGrabbing", toggleButton.isChecked()).apply();
        prefs.edit().commit();

        if (toggleButton.isChecked()) {

            launchMQTTService();
            ServiceManager.getInstance(MainActivity.this).setTransferToDbInterval(INTERVAL_TRANSFER_TO_DB);


            for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
                ServiceManager.ServiceComponent service;
                service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
                ServiceManager.getInstance(MainActivity.this).startScheduleService(service);
            }
            if (ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size() > 0) {
                Toast.makeText(this, "Acquisition started", Toast.LENGTH_LONG).show();
            }

        }
        else {
            // STOP all schedules
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MQTTService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.cancel(scheduledIntent);
            ServiceManager.getInstance(MainActivity.this).stopFluentSampling();

            stopMQTTService();

            ServiceManager.getInstance(MainActivity.this).stopTransferToDb();
            for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
                ServiceManager.ServiceComponent service;
                service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
                ServiceManager.getInstance(MainActivity.this).stopScheduleService(service);
            }
            if (ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size() > 0) {
                Toast.makeText(this, "Acquisition stopped", Toast.LENGTH_LONG).show();
            }
            stopService(new Intent(this, SensorBackgroundService.class));
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
            Intent intent = new Intent(this, LogInActivity.class);
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
        ListView list = (ListView) findViewById(R.id.listViewMain);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                // Go to Configuration Activity to modify current active configuration
                Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
                intent.putExtra(AddDeviceActivity.TYPE_SENSOR, ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(position).getSensorType());
                //intent.putExtra(AddDeviceActivity.ENABLES_SENSOR, false);
                intent.putExtra(AddDeviceActivity.MODIFY_CONFIGURATION, true);
                intent.putExtra(ConfigurationActivity.CONFIGURATION_DB_ID,ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(position).getActiveConfiguration().getDbId());
                startActivity(intent);
            }
        });
    }

    private void populateListView() {
        ArrayAdapter<ServiceManager.ServiceComponent> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.listViewMain);
        list.setAdapter(adapter);
    }

    class ServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MQTTService.SUBSCRIBE: 	break;
                case MQTTService.PUBLISH:		break;
                case MQTTService.REGISTER:		break;
                default:
                    super.handleMessage(msg);
                    return;
            }

            Bundle b = msg.getData();
            if (b != null)
            {
                Boolean status = b.getBoolean(MQTTService.STATUS);
                if (status == false)
                {
                    Log.d(TAG,"Fail");
                }
                else
                {
                    Log.d(TAG, "Success");
                }
            }
        }
    }



    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(MainActivity.this, R.layout.item_view, ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(position);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getComponentImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
        }

    }

    private void createAllFeeds() {
        ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_x","null", MODEL_NAME + "/accelerometer/1",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_y","null", MODEL_NAME + "/accelerometer/2",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_z","null", MODEL_NAME + "/accelerometer/3",2);

        ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_x","null", MODEL_NAME + "/gyroscope/1",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_y","null", MODEL_NAME + "/gyroscope/2",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_z","null", MODEL_NAME + "/gyroscope/3",2);

        ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_x","null", MODEL_NAME + "/magnetometer/1",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_y","null", MODEL_NAME + "/magnetometer/2",2);
        ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_z","null", MODEL_NAME + "/magnetometer/3",2);

        ServiceManager.getInstance(MainActivity.this).createFeed("Light","lux", MODEL_NAME + "/light",1);
        ServiceManager.getInstance(MainActivity.this).createFeed("Pressure","bar", MODEL_NAME + "/pressure",1);
        ServiceManager.getInstance(MainActivity.this).createFeed("Proximity","null", MODEL_NAME + "/proximity",1);
        ServiceManager.getInstance(MainActivity.this).createFeed("Temperature","null", MODEL_NAME + "/temperature",1);
    }

    private void launchMQTTService() {
            if (prefs.getBoolean("loggedIn",false)) {
                Log.d(TAG, "Activate Mqtt service");
                AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, MQTTService.class);

                PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL_TRANSFER_TO_SENSORMIND * 1000, scheduledIntent);
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

    private void descheduleMQTTService () {
        Log.d(TAG, "Deschedule Mqtt service");
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MQTTService.class);
        //TODO finire qua
        PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL_TRANSFER_TO_SENSORMIND * 1000, scheduledIntent);
    }

    public class MyReceiver extends BroadcastReceiver {
        //TODO Istanziare all'avvio del telefono
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Launch services after boot");
            initEverything();
        }
    }
}
