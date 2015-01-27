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
import android.widget.Toast;
import android.widget.AdapterView;

//public class AddDeviceActivity extends ActionBarActivity {

public class AddDeviceActivity extends Activity {

    public final static String TYPE_SENSOR = "sensor_type";
    public final static String ENABLES_SENSOR = "sensor_enable";

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

    @Override
    public void onBackPressed(){
        //Your code here
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.listViewAddDevice);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                ServiceManager.ServiceComponent clickedServiceComponent = ServiceManager.getInstance(AddDeviceActivity.this).getAvailableServiceComponentList().get(position);
                // Add service component to active services component
                //ServiceManager.getInstance(AddDeviceActivity.this).addServiceComponentActive(clickedServiceComponent);
                // Come back to Main Activity
                Intent intent = new Intent(getApplicationContext(), ScheduleService.class);
                intent.putExtra(TYPE_SENSOR, ServiceManager.getInstance(AddDeviceActivity.this).getAvailableServiceComponentList().get(position).getSensorType());
                startActivity(intent);
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
            super(AddDeviceActivity.this, R.layout.item_view, ServiceManager.getInstance(AddDeviceActivity.this).getAvailableServiceComponentList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance(this.getContext()).getAvailableServiceComponentList().get(position);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_imageView);
            imageView.setImageResource(currentServiceComponent.getComponentImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
            //return super.getView(position, convertView, parent);
        }
    }
}
