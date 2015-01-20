package com.ukuke.gl.sensegrab;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class DeviceCapabilitiesActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_capabilities);
        Log.i("DeviceCapabilities", "Configure your app!");

        int numAvailableServices = ServiceManager.getInstance().populateServiceComponentList(this);
        Toast.makeText(getApplicationContext(), "Found " + numAvailableServices + " available services" , Toast.LENGTH_LONG).show();

        populateListView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_initial_setup, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateListView() {
        ArrayAdapter<ServiceManager.ServiceComponent> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.listViewDeviceCapabilities);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(DeviceCapabilitiesActivity.this, R.layout.item_view, ServiceManager.getInstance().getServiceComponentList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance().getServiceComponentList().get(position);

            ImageView  imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getAvailableImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
            //return super.getView(position, convertView, parent);
        }
    }
}
