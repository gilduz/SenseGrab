package com.ukuke.gl.sensormind;

import android.app.Activity;
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

public class MainActivity extends Activity {
//    public class MainActivity extends ActionBarActivity {

    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Check on file if it's the first app launch. If yes launch Initial setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check shared preferences
        prefs = getSharedPreferences("com.ukuke.gl.sensegrab", MODE_PRIVATE);

        // Search for services
        int numAvailableServices = ServiceManager.getInstance().populateServiceComponentList(this);
        Toast.makeText(getApplicationContext(), "Found " + numAvailableServices + " available services" , Toast.LENGTH_LONG).show();
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
            startDeviceCapabilities();
        }
        else if (id == R.id.action_add_new_service) {
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_test) {

            Toast.makeText(getApplicationContext(), "THIS WAS A TEST", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            // Do first run stuff here then set 'firstrun' as false
            // using the following line to edit/commit prefs
            Log.i("MainActivity","This is a first run. Set up everything!");
            prefs.edit().putBoolean("firstrun", false).apply();

            // TODO: Launch initial setup activity
            startDeviceCapabilities();
        }
        else {
            Log.v("MainActivity", "This is not a first run. Let's continue");
        }

        populateListView();
        //registerClickCallback();
    }

    private boolean startDeviceCapabilities () {

        Intent intent = new Intent(this, DeviceCapabilitiesActivity.class);
        startActivity(intent);

        return true;
    }

    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.listViewMain);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {

                ServiceManager.ServiceComponent clickedServiceComponent = ServiceManager.getInstance().getAvailableServiceComponentList().get(position);
                // Add service component to active services component
                ServiceManager.getInstance().addServiceComponentActive(clickedServiceComponent);
                Toast.makeText(MainActivity.this, ServiceManager.getInstance().getAvailableServiceComponentList().get(position).getDysplayName() + " added to monitored services.", Toast.LENGTH_LONG).show();
                // Come back to Main Activity
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
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
            //return super.getView(position, convertView, parent);
        }
    }

    private int testWork() {
        int a;
        a = 3+2;
        return a;
    }
}
