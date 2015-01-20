package com.ukuke.gl.sensegrab;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Check on file if it's the first app launch. If yes launch Initial setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check shared preferences
        prefs = getSharedPreferences("com.ukuke.gl.sensegrab", MODE_PRIVATE);
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
    }

    private boolean startDeviceCapabilities () {

        Intent intent = new Intent(this, DeviceCapabilitiesActivity.class);
        startActivity(intent);

        return true;
    }
}
