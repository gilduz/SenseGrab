package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
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


//public class DeviceCapabilitiesActivity extends ActionBarActivity {
public class DeviceCapabilitiesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_capabilities);
        Log.i("DeviceCapabilities", "Configure your app!");

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

    @Override
    public void onBackPressed(){
        //Your code here
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private class MyListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent> {
        public MyListAdapter() {
            super(DeviceCapabilitiesActivity.this, R.layout.item_view_2img, ServiceManager.getInstance(DeviceCapabilitiesActivity.this).getServiceComponentList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view_2img, parent, false);
            }

            ServiceManager.ServiceComponent currentServiceComponent = ServiceManager.getInstance(this.getContext()).getServiceComponentList().get(position);

            ImageView  imageView_1 = (ImageView) itemView.findViewById(R.id.item_imageView_1);
            imageView_1.setImageResource(currentServiceComponent.getComponentImageID());

            ImageView  imageView_2 = (ImageView) itemView.findViewById(R.id.item_imageView_2);
            imageView_2.setImageResource(currentServiceComponent.getAvailableImageID());

            TextView myText = (TextView) itemView.findViewById(R.id.item_textView);
            myText.setText(currentServiceComponent.getDysplayName());

            return itemView;
            //return super.getView(position, convertView, parent);
        }
    }
}
