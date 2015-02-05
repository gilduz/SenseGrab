package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigurationActivity extends Activity {

    // Database
    DbHelper dbHelper;

    // Dynamic views in activity_configuration.xml
    EditText confName; //Name
    SeekBar seekSamp; //sampling time
    SeekBar seekWin; //sampling window
    TextView textSamp;
    TextView textWin;
    RadioGroup radioGr;
    Switch gpsSwitch;
    Button buttonDelete;
    Button buttonSave;

    // Initialize and set default value for seekBars
    int progressSamp = 5;
    int progressWin = 5;

    // Gildo
    private Intent intentAddDevice;
    private int typeSensor;
    private boolean logging = true;
    private ServiceManager.ServiceComponent serviceComponent;
    private ServiceManager.ServiceComponent.Configuration configuration;
    private boolean isAModify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);


        intentAddDevice = getIntent();
        typeSensor = intentAddDevice.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);
        isAModify = intentAddDevice.getBooleanExtra(AddDeviceActivity.MODIFY_CONFIGURATION, false);

        serviceComponent = ServiceManager.getInstance(ConfigurationActivity.this).getServiceComponentAvailableBySensorType(typeSensor);

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

        // Buttons
        buttonDelete = (Button) findViewById(R.id.Conf_delete);
        buttonSave = (Button) findViewById(R.id.Conf_save);

        //TODO Aggiungere nascondino window, spiegare che minchia è window, settare i valori di default e i valori massimi per ogni tipo di sensore. Puzzi un po'

        if (!isAModify) {
            buttonDelete.setVisibility(View.GONE);
            confName.setText("Default_Name");
            configuration = new ServiceManager.ServiceComponent.Configuration();


        }
        else {
            configuration = serviceComponent.getActiveConfiguration(); // TODO Da cambiare per gestire config generiche
            confName.setText(serviceComponent.getActiveConfiguration().getConfigurationName());

            int sampSeekValue = 0;
            // MILLISECONDS
            if (serviceComponent.getActiveConfiguration().getInterval() < 1000) {
                sampSeekValue = (int) serviceComponent.getActiveConfiguration().getInterval();
                radioGr.check(R.id.Conf_radioMill);


            }
            // SECONDS
            else if ((serviceComponent.getActiveConfiguration().getInterval() >= 1000) && (serviceComponent.getActiveConfiguration().getInterval() < 60000)) {
                sampSeekValue = (int) serviceComponent.getActiveConfiguration().getInterval() / 1000;
                radioGr.check(R.id.Conf_radioSec);
            }
            // MINUTES
            else if ((serviceComponent.getActiveConfiguration().getInterval() >= 60000) && (serviceComponent.getActiveConfiguration().getInterval() < 36000000)) {
                sampSeekValue = (int) serviceComponent.getActiveConfiguration().getInterval() / 60000;
                radioGr.check(R.id.Conf_radioMin);
            }
            // HOURS
            else if (serviceComponent.getActiveConfiguration().getInterval() > 3600000) {
                sampSeekValue = (int) serviceComponent.getActiveConfiguration().getInterval() / 3600000;
                radioGr.check(R.id.Conf_radioHour);
            }

            seekSamp.setProgress(sampSeekValue);
            textSamp.setText(Integer.toString(sampSeekValue));
            seekWin.setProgress(serviceComponent.getActiveConfiguration().getWindow());
            textWin.setText(Long.toString(serviceComponent.getActiveConfiguration().getWindow()));

            confName.setText(serviceComponent.getActiveConfiguration().getConfigurationName());
            gpsSwitch.setChecked(serviceComponent.getActiveConfiguration().getAttachGPS());
        }



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

        long interval = 1000;

        int window = 1;// = 1;

        try {

            if (radioGr.getCheckedRadioButtonId() == R.id.Conf_radioMill) {
                interval = seekSamp.getProgress();
            }
            else if (radioGr.getCheckedRadioButtonId() == R.id.Conf_radioSec) {
                interval = seekSamp.getProgress() * 1000;
            }
            else if (radioGr.getCheckedRadioButtonId() == R.id.Conf_radioMin) {
                interval = seekSamp.getProgress() * 60000;
            }
            else if (radioGr.getCheckedRadioButtonId() == R.id.Conf_radioHour) {
                interval = seekSamp.getProgress() * 3600000;
            }

            window = seekWin.getProgress();
        } catch (Exception e) {
            interval = 1000L;
        }
        // TODO: Non è stato fatto sec/min

        configuration.setInterval(interval);
        configuration.setWindow(window);
        configuration.setConfigurationName("Default_Name");
        //configuration.setPath("test_1/v1/bm/Test"); //TODO Da aggiungere il path
        configuration.setAttachGPS(gpsSwitch.isChecked());

        configuration.setPath(serviceComponent.getDefaultPath());
        serviceComponent.removeConfiguration(confName.getText().toString());
        serviceComponent.addConfiguration(configuration);
        serviceComponent.setActiveConfiguration(configuration);

        ServiceManager.getInstance(ConfigurationActivity.this).addServiceComponentActive(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).startScheduleService(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).addConfigurationServiceToDB(serviceComponent, configuration, true); // TODO Gestire l'attivo o no


        Toast.makeText(this, "Service added", Toast.LENGTH_LONG).show();
        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);
    }

    public void onButtonDeleteClicked(View view) {
        ServiceManager.getInstance(ConfigurationActivity.this).stopScheduleService(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).removeServiceComponentActive(typeSensor);
        //TODO: Sistema qui sotto!
        configuration.setConfigurationName(serviceComponent.getDysplayName());
        ServiceManager.getInstance(ConfigurationActivity.this).removeConfigurationServiceToDB(configuration);
        Toast.makeText(this, "Service removed", Toast.LENGTH_LONG).show();
        Intent intentMain = new Intent(this, MainActivity.class);
        startActivity(intentMain);

    }

    //TODO aggiungere metodo saveOrUpdate() che differenzia a seconda di getextras (persare se settare una variabile invece che usare 2 volte getextras
    //TODO implementare scrittura o modifica su stringa database

    //TODO aggiungere metodo delete con alert per confermare, implementare cancellazione riga dbHelper

}
