package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ukuke.gl.sensormind.services.*;

import java.util.ArrayList;


public class ScheduleService extends Activity {

    private static final String TAG = SensorBackgroundService.class.getSimpleName();

    Intent intentAddDevice;
    int typeSensor;

    EditText editInterval;
    CheckBox chkLogging;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_service);

        editInterval = (EditText)findViewById(R.id.editInterval);
        chkLogging = (CheckBox)findViewById(R.id.chkLogging);

        intentAddDevice = getIntent();
        typeSensor = intentAddDevice.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_schedule_service, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_test) {
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonStartClicked(View view) {
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, SensorBackgroundService.class);

        Bundle args = new Bundle();

        try {
            args.putBoolean(SensorBackgroundService.KEY_LOGGING, chkLogging.isChecked());
        } catch (Exception e) {}
        try {
            args.putInt(SensorBackgroundService.KEY_SENSOR_TYPE, typeSensor);
        } catch (Exception e) {}

        intent.putExtras(args);

        // try getting interval option
        long interval;
        try {
            interval = Long.parseLong(editInterval.getText().toString());
        } catch (Exception e) {
            interval = 1000L;
        }

        // Start the service

        PendingIntent scheduledIntent = PendingIntent.getService(this, typeSensor, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, scheduledIntent);

        // Go back to main activity
        Toast.makeText(ScheduleService.this, "Service added", Toast.LENGTH_LONG).show();
        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);
    }

    public void onButtonDeleteClicked(View view) {
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, SensorBackgroundService.class);
        PendingIntent scheduledIntent = PendingIntent.getService(this, typeSensor, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        scheduler.cancel(scheduledIntent);

        ServiceManager.getInstance().removeServiceComponentActive(typeSensor);
        // Go back to main activity
        Toast.makeText(ScheduleService.this, "Service deleted", Toast.LENGTH_LONG).show();        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);
    }



}
