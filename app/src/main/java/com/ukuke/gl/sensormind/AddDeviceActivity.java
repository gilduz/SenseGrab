package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;

public class AddDeviceActivity extends Activity {

    public final static String TYPE_SENSOR = "sensor_type";
    public final static String MODIFY_CONFIGURATION = "modify_configuration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        populateListView();
        registerClickCallback();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Actually there isn't a menu in this activity
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    private void registerClickCallback() {
        // Set listener for the list view
        ListView list = (ListView) findViewById(R.id.listViewAddDevice);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                                Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
                intent.putExtra(TYPE_SENSOR, ServiceManager.getInstance(AddDeviceActivity.this).getServiceComponentUnusedList().get(position).getSensorType());
                startActivity(intent);
                finish();
            }
        });
    }

    private void populateListView() {
        ArrayAdapter<ServiceManager.ServiceComponent> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.listViewAddDevice);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(AddDeviceActivity.this, R.layout.item_view, ServiceManager.getInstance(AddDeviceActivity.this).getServiceComponentUnusedList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance(this.getContext()).getServiceComponentUnusedList().get(position);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getComponentImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
        }
    }
}
