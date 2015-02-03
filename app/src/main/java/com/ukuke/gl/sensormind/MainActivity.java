package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;
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
    public static final int INTERVAL_TRANSFER_TO_DB = 5; //[sec]
    public static final int INTERVAL_TRANSFER_TO_SENSORMIND = 10; //[sec]
    public static final String IP_MQTT = "137.204.213.190";
    public static final int PORT_MQTT = 1884;
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
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);


        ServiceManager.getInstance(MainActivity.this).initializeFromDB();

        // Search  services
        //int numAvailableServices = ServiceManager.getInstance(MainActivity.this).populateServiceComponentList();
        //Toast.makeText(getApplicationContext(), "Found " + numAvailableServices + " available services" , Toast.LENGTH_LONG).show();

        View v = new View(this);

        username = prefs.getString("username", "NULL");
        password = prefs.getString("password", "NULL");

        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(prefs.getBoolean("enableGrabbing", true));
        prefs.edit().putString("ip_MQTT",IP_MQTT).apply();
        prefs.edit().putInt("port_MQTT",PORT_MQTT).apply();

        prefs.edit().commit();





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

            ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_x","null","accelerometer/1",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_y","null","accelerometer/2",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Accelerometer_z","null","accelerometer/3",2);

            ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_x","null","gyroscope/1",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_y","null","gyroscope/2",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Gyroscope_z","null","gyroscope/3",2);

            ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_x","null","magnetometer/1",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_y","null","magnetometer/2",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Magnetometer_z","null","magnetometer/3",2);

            ServiceManager.getInstance(MainActivity.this).createFeed("Light","lux","light",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Pressure","bar","pressure",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Proximity","null","proximity",2);
            ServiceManager.getInstance(MainActivity.this).createFeed("Temperature","null","temperature",2);

            // prefs.getString("password","test_3"));
           // ServiceManager.getInstance(MainActivity.this).syncAllFeedList();
            Toast.makeText(getApplicationContext(), "THIS WAS A TEST", Toast.LENGTH_LONG).show();
            //AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }

        else if (id == R.id.action_log_in) {
            Intent intent = new Intent(this, LogInActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickedToggle(View view) {
        ToggleButton toggleButton;
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        prefs.edit().putBoolean("enableGrabbing", toggleButton.isChecked()).apply();
        prefs.edit().commit();

        if (toggleButton.isChecked()) {

            if (prefs.getBoolean("loggedIn",false)) {
                AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, MQTTService.class);

                PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL_TRANSFER_TO_SENSORMIND * 1000, scheduledIntent);
            }

            ServiceManager.getInstance(MainActivity.this).setTransferToDbInterval(INTERVAL_TRANSFER_TO_DB);
            for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
                ServiceManager.ServiceComponent service;
                service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
                ServiceManager.getInstance(MainActivity.this).startScheduleService(service);
            }

        }
        else {
            // STOP all schedules
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MQTTService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.cancel(scheduledIntent);
           // stopService(new Intent(this, MQTTService.class));

            ServiceManager.getInstance(MainActivity.this).stopTransferToDb();
            for (int i = 0; i < ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().size(); i++) {
                ServiceManager.ServiceComponent service;
                service = ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(i);
                ServiceManager.getInstance(MainActivity.this).stopScheduleService(service);
            }
            stopService(new Intent(this, SensorBackgroundService.class));
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            Log.i("MainActivity","This is a first run. Set up everything!");
            prefs.edit().putBoolean("firstrun", false).apply();
        }
        else {
            Log.v("MainActivity", "This is not a first run. Let's continue");
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
                // Come back to Main Activity
                Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
                intent.putExtra(AddDeviceActivity.TYPE_SENSOR, ServiceManager.getInstance(MainActivity.this).getServiceComponentActiveList().get(position).getSensorType());
                intent.putExtra(AddDeviceActivity.ENABLES_SENSOR, false);
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


}
