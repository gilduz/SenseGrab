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
import android.widget.ToggleButton;

import com.ukuke.gl.sensormind.services.*;


/**
 * A Fragment for managing the background service
 */
public class ScheduleService extends Activity {

    private static final String TAG = SensorBackgroundService.class.getSimpleName();
    public static final String KEY_SENSOR_TYPE = "sensor_type";

    Intent launchActivityIntent;
    int typeSensor;

    EditText editMin;
    EditText editMax;
    EditText editInterval;
    CheckBox chkLogging;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScheduleServiceFragment.
     */
    public static ScheduleService newInstance() {
        ScheduleService fragment = new ScheduleService();
        //Bundle args = new Bundle();
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_service);

        editMin = (EditText)findViewById(R.id.editMin);
        editMax = (EditText)findViewById(R.id.editMax);
        editInterval = (EditText)findViewById(R.id.editInterval);
        chkLogging = (CheckBox)findViewById(R.id.chkLogging);

        launchActivityIntent = getIntent();
        typeSensor = launchActivityIntent.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule_service, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_test) {
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();



        if (on) {
            // Enable vibrate
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, SensorBackgroundService.class);

            // add some extras for config
            Bundle args = new Bundle();
            try {
                float value = Float.parseFloat(editMin.getText().toString());
                args.putFloat(SensorBackgroundService.KEY_THRESHOLD_MIN_VALUE, value);
            } catch (Exception e) {}
            try {
                float value = Float.parseFloat(editMax.getText().toString());
                args.putFloat(SensorBackgroundService.KEY_THRESHOLD_MAX_VALUE, value);
            } catch (Exception e) {}
            try {
                args.putBoolean(SensorBackgroundService.KEY_LOGGING, chkLogging.isChecked());
            } catch (Exception e) {}
            try {
                args.putBoolean(SensorBackgroundService.KEY_LOGGING, chkLogging.isChecked());
            } catch (Exception e) {}
            try {
                args.putInt(SensorBackgroundService.KEY_SENSOR_TYPE, typeSensor);
            } catch (Exception e) {
                Log.d(TAG,"Occhio mi ha dato errore in sensor type");
            }

            intent.putExtras(args);

            // try getting interval option
            long interval;
            try {
                interval = Long.parseLong(editInterval.getText().toString());
            } catch (Exception e) {
                // use the default in that case
                interval = 1000L;

            }

            try {
            PendingIntent scheduledIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // start the service
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, scheduledIntent);
            } catch (Exception e) {
                Log.v(TAG,"ERRORE: " + e);

            }
        } else {
            // Disable vibrate
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, SensorBackgroundService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            scheduler.cancel(scheduledIntent);
        }
    }


}
