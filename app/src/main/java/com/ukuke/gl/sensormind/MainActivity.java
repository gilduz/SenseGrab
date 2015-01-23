package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
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

import com.ukuke.gl.sensormind.services.SensorBackgroundService;

public class MainActivity extends Activity {
//    public class MainActivity extends ActionBarActivity {

    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Check on file if it's the first app launch. If yes launch Initial setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check shared preferences
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);

        // Search for services
        int numAvailableServices = ServiceManager.getInstance().populateServiceComponentList(this);
        //Toast.makeText(getApplicationContext(), "Found " + numAvailableServices + " available services" , Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent(this, SensorBackgroundService.class);
        startService(intent);
        stopService(intent);
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

            Toast.makeText(getApplicationContext(), "THIS WAS A TEST", Toast.LENGTH_LONG).show();
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, SensorBackgroundService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.cancel(scheduledIntent);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            Log.i("MainActivity","This is a first run. Set up everything!");
            prefs.edit().putBoolean("firstrun", false).apply();
            // TODO: Launch initial setup activity
            //Intent intent = new Intent(this, DeviceCapabilitiesActivity.class);
            //startActivity(intent);
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
                Intent intent = new Intent(getApplicationContext(), ScheduleService.class);
                intent.putExtra(AddDeviceActivity.TYPE_SENSOR, ServiceManager.getInstance().getserviceComponentActiveList().get(position).getSensorType());
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

    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(MainActivity.this, R.layout.item_view, ServiceManager.getInstance().getserviceComponentActiveList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance().getserviceComponentActiveList().get(position);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getComponentImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
        }
    }

}
