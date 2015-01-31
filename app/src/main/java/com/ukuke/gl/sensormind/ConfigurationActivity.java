package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigurationActivity extends Activity {

    // Database
    DbHelper db;

    // Dynamic views in activity_configuration.xml
    EditText confName; //Name
    SeekBar seekSamp; //sampling time
    SeekBar seekWin; //sampling window
    TextView textSamp;
    TextView textWin;
    RadioGroup radioGr;
    Switch gpsSwitch;

    // Initialize and set default value for seekBars
    int progressSamp = 5;
    int progressWin = 5;

    // Gildo
    private Intent intentAddDevice;
    private int typeSensor;
    private boolean logging = true;
    private ServiceManager.ServiceComponent serviceComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);


        intentAddDevice = getIntent();
        typeSensor = intentAddDevice.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);

        serviceComponent = ServiceManager.getInstance(ConfigurationActivity.this).getAvailableServiceComponentBySensorType(typeSensor);

        // seek bars and relative value views
        seekSamp = (SeekBar) findViewById(R.id.Conf_Sam_seekBar);
        textSamp = (TextView) findViewById(R.id.Conf_viewSeekbarValue);
        seekWin = (SeekBar) findViewById(R.id.Conf_Win_seekBar);
        textWin = (TextView) findViewById(R.id.Conf_viewWinSeekbarValue);

        // Name
        confName = (EditText) findViewById(R.id.Conf_InsertName);

        // Radio buttons
        radioGr = (RadioGroup) findViewById(R.id.Conf_radioGroup);

        // Gps switch
        gpsSwitch = (Switch) findViewById(R.id.Conf_gps);


        seekSamp.setMax(60);
        seekWin.setMax(60);

        // Set values from seekBars
        seekSamp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSamp.setText(String.valueOf(progress));
                progressSamp = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekWin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textWin.setText(String.valueOf(progress));
                progressWin = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //TODO Aggiungere condizione getExtras per differenziare se si arriva da add device o da main
        //TODO se si arriva da add device prendere il tipo sensore
        //TODO se si arriva da main è una modifica, cambiare testo save in update
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
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

    public void onButtonSaveClicked(View view) {
        long interval;
        int window = 1;
        try {
            interval = seekSamp.getProgress()*1000;
            window = 1;//seekSamp.getProgress();
        } catch (Exception e) {
            interval = 1000L;
        }
        // TODO: Non è stato fatto sec/min
        ServiceManager.ServiceComponent.Configuration configuration = new ServiceManager.ServiceComponent.Configuration();
        configuration.setInterval(interval);
        configuration.setWindow(window);
        configuration.setConfigurationName(confName.getText().toString());
        configuration.setPath("/Path/1");
        configuration.setAttachGPS(gpsSwitch.isActivated());

        ServiceManager.ServiceComponent component;
        component = ServiceManager.getInstance(ConfigurationActivity.this).getAvailableServiceComponentBySensorType(typeSensor);

        component.removeConfiguration(confName.getText().toString());
        component.addConfiguration(configuration);
        component.setActiveConfiguration(configuration);

        ServiceManager.getInstance(ConfigurationActivity.this).addServiceComponentActive(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).startScheduleService(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).addConfigurationServiceToDB(serviceComponent, configuration);

        Toast.makeText(this, "Service added", Toast.LENGTH_LONG).show();
        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);
    }

    public void onButtonDeleteClicked(View view) {
        ServiceManager.getInstance(ConfigurationActivity.this).stopScheduleService(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).removeServiceComponentActive(typeSensor);
        Toast.makeText(this, "Service removed", Toast.LENGTH_LONG).show();
        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);

    }

    //TODO aggiungere metodo saveOrUpdate() che differenzia a seconda di getextras (persare se settare una variabile invece che usare 2 volte getextras
    //TODO implementare scrittura o modifica su stringa database

    //TODO aggiungere metodo delete con alert per confermare, implementare cancellazione riga db

}
