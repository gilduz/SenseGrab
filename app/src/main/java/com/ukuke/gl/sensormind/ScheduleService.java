//package com.ukuke.gl.sensormind;
//
//import android.app.Activity;
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.hardware.Sensor;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.Toast;
//import android.widget.ToggleButton;
//
//import com.ukuke.gl.sensormind.services.*;
//
//import java.util.ArrayList;
//
//
//public class ScheduleService extends Activity {
//
//    private static final String TAG = SensorBackgroundService.class.getSimpleName();
//
//    Intent intentAddDevice;
//    int typeSensor;
//
//    EditText editInterval;
//    CheckBox chkLogging;
//
//    ServiceManager.ServiceComponent component;
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_schedule_service);
//
//        editInterval = (EditText) findViewById(R.id.editInterval);
//        chkLogging = (CheckBox) findViewById(R.id.chkLogging);
//
//        intentAddDevice = getIntent();
//        typeSensor = intentAddDevice.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);
//
//        //component = ServiceManager.getInstance(Sch)
//
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_schedule_service, menu);
//        return true;
//
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//        int id = item.getItemId();
//
//        if (id == R.id.action_test) {
//            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    public void onButtonStartClicked(View view) {
//        long interval;
//        try {
//            interval = Long.parseLong(editInterval.getText().toString());
//        } catch (Exception e) {
//            interval = 1000L;
//        }
//        // TODO: Bisogna passare il window reale da immissione in testo
//        ServiceManager.getInstance(ScheduleService.this).addServiceComponentActive(ServiceManager.getInstance(ScheduleService.this).getAvailableServiceComponentBySensorType(typeSensor));
//        ServiceManager.getInstance(ScheduleService.this).startScheduleService(typeSensor, chkLogging.isChecked(), interval, 1);
//        ServiceManager.getInstance(ScheduleService.this).addScheduleServiceToDB(typeSensor, chkLogging.isChecked(), interval, 1);
//        Toast.makeText(this, "Service added", Toast.LENGTH_LONG).show();
//        Intent intentMain = new Intent(this, MainActivity.class);
//        startActivity(intentMain);
//    }
//
//    public void onButtonDeleteClicked(View view) {
//
//
//        ServiceManager.getInstance(ScheduleService.this).stopScheduleService(typeSensor);
//        ServiceManager.getInstance(ScheduleService.this).removeServiceComponentActive(typeSensor);
//        Toast.makeText(this, "Service removed", Toast.LENGTH_LONG).show();
//        Intent intentMain = new Intent(this, MainActivity.class);
//        startActivity(intentMain);
//    }
//}