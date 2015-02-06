package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
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

public class ConfigurationActivity extends Activity /*implements OnClickListener*/ {

    // CONSTANT VALUES
    //Editable
    private static final long DEFAULT_INTERVAL = 1000;
    private static final int DEFAULT_WINDOW = 2;
    private static final int STEP_MILLIS = 50;
    private static final String DEFAULT_NAME = "Default_Name";
    //Do not edit!
    private static final int MILLIS_IN_SEC = 1000;
    private static final int MILLIS_IN_MIN = 60*MILLIS_IN_SEC;
    private static final int MILLIS_IN_HOUR = 60*MILLIS_IN_MIN;
    private static final int MIN_MILLIS = 250;
    private int relativeMinMilliS;//related to each sensor, going close to this value forces streaming
    private int usedMinMillis;
    private static final int MAX_SEEK_MILLIS = MILLIS_IN_SEC-STEP_MILLIS;
    private int maxSeekSampMillis;
    private static final int MAX_SEEK_SEC = 59;
    private static final int MAX_SEEK_MIN = 59;
    private static final int MAX_SEEK_HOUR = 24;

    private static String TAG = ConfigurationActivity.class.getSimpleName();


    // Database
    //DbHelper dbHelper;

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
    long progressSamp = DEFAULT_INTERVAL;
    int progressWin = DEFAULT_WINDOW;

    // Gildo
    private Intent intent;
    private int typeSensor;
    private boolean logging = true;
    private ServiceManager.ServiceComponent serviceComponent;
    private ServiceManager.ServiceComponent.Configuration configuration;
    private boolean isAModify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        //-----------------------EXTRAS------------------------
        intent = getIntent();
        typeSensor = intent.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);
        isAModify = intent.getBooleanExtra(AddDeviceActivity.MODIFY_CONFIGURATION, false);

        serviceComponent = ServiceManager.getInstance(ConfigurationActivity.this).getServiceComponentAvailableBySensorType(typeSensor);
        relativeMinMilliS = serviceComponent.getMinDelay()/1000;
        if (relativeMinMilliS >STEP_MILLIS){
            usedMinMillis = (int) Math.ceil((relativeMinMilliS /1000)/STEP_MILLIS);
        } else usedMinMillis = MIN_MILLIS;
        maxSeekSampMillis = MAX_SEEK_MILLIS-usedMinMillis;

        setTitle(serviceComponent.getDysplayName());

        //-----------------------GRAPHIC ELEMENTS------------------------
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

        //---------------------LISTENERS----------------------
        //Seekbars listener
        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                switch (seekBar.getId()) {
                    case R.id.Conf_Sam_seekBar: {
                        textSamp.setText(String.valueOf(getTextSampValue(progress, radioGr.getCheckedRadioButtonId())));
                        progressSamp = progress;
                        break;
                    }
                    case R.id.Conf_Win_seekBar: {
                        textWin.setText(String.valueOf(progress));
                        progressWin = progress;
                        break;
                    }
                }

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        RadioGroup.OnCheckedChangeListener radioListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.Conf_radioMill:
                        seekSamp.setMax(maxSeekSampMillis/STEP_MILLIS);
                        break;
                    case R.id.Conf_radioSec:
                        seekSamp.setMax(MAX_SEEK_SEC-1);
                        break;
                    case R.id.Conf_radioMin:
                        seekSamp.setMax(MAX_SEEK_MIN-1);
                        break;
                    case R.id.Conf_radioHour:
                        seekSamp.setMax(MAX_SEEK_HOUR-1);
                        break;
                    default:break;
                }
            }
        };



        //TODO Aggiungere nascondino window, spiegare che minchia è window, settare i valori di default e i valori massimi per ogni tipo di sensore. Puzzi un po'

        if (!isAModify) {
            //NEW CONFIGURATION
            buttonDelete.setVisibility(View.GONE);
            //configuration = new ServiceManager.ServiceComponent.Configuration();
            setDefaultValues();


        } else {
            //MODIFYING OLD CONFIGURATION
            configuration = serviceComponent.getActiveConfiguration(); // TODO Passare DbId tramite extras per gestire molteplici configurazioni
            confName.setText(serviceComponent.getActiveConfiguration().getConfigurationName());

            long millis = serviceComponent.getActiveConfiguration().getInterval();
            Log.d(TAG,"L'intervallo della configurazione attiva è: "+millis+" millisecondi");
            int rightRadio = getRightRadio(millis);
            int sampSeekValue = getProgressSeekSamp((int)millis,rightRadio);
            radioGr.check(rightRadio);
            seekSamp.setProgress(sampSeekValue);
            textSamp.setText(Long.toString(getTextSampValue(sampSeekValue,rightRadio)));
            seekWin.setProgress(serviceComponent.getActiveConfiguration().getWindow());
            textWin.setText(Long.toString(serviceComponent.getActiveConfiguration().getWindow()));

            confName.setText(serviceComponent.getActiveConfiguration().getConfigurationName());
            gpsSwitch.setChecked(serviceComponent.getActiveConfiguration().getAttachGPS());
        }


        // Set values from seekBars
        seekSamp.setOnSeekBarChangeListener(seekListener);
        seekWin.setOnSeekBarChangeListener(seekListener);
        radioGr.setOnCheckedChangeListener(radioListener);




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
        switch (id) {
            case R.id.Conf_load_conf:
                return true;
            case R.id.Conf_default_values:
                setDefaultValues();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*@Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Conf_radioMill:{

            }
        }
    }*/

    public void onButtonSaveClicked(View view) {

        long interval = DEFAULT_INTERVAL;

        int window = DEFAULT_WINDOW;

        try {

            interval = getMillis(seekSamp.getProgress(), radioGr.getCheckedRadioButtonId());
            window = seekWin.getProgress();
        } catch (Exception e) {
            interval = DEFAULT_INTERVAL;
        }
        // TODO: Non è stato fatto sec/min

        if (!isAModify) {
            configuration = new ServiceManager.ServiceComponent.Configuration();
        }

        configuration.setInterval(interval);
        configuration.setWindow(window);
        configuration.setConfigurationName(confName.getText().toString());
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
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intentMain);
    }

    public void onButtonDeleteClicked(View view) {
        ServiceManager.getInstance(ConfigurationActivity.this).stopScheduleService(serviceComponent);
        ServiceManager.getInstance(ConfigurationActivity.this).removeServiceComponentActive(typeSensor);
        //TODO: Sistema qui sotto!
        configuration.setConfigurationName(serviceComponent.getDysplayName());
        ServiceManager.getInstance(ConfigurationActivity.this).removeConfigurationServiceToDB(configuration);
        Toast.makeText(this, "Service removed", Toast.LENGTH_LONG).show();
        Intent intent=new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    private void setDefaultValues(){
        confName.setText(DEFAULT_NAME);
        radioGr.check(R.id.Conf_radioSec);
        progressSamp = getProgressSeekSamp((int)DEFAULT_INTERVAL,R.id.Conf_radioSec);
        seekSamp.setMax(MAX_SEEK_SEC-1);
        seekSamp.setProgress((int)progressSamp);
        textSamp.setText(String.valueOf(getTextSampValue((int) progressSamp, radioGr.getCheckedRadioButtonId())));
        seekWin.setProgress(DEFAULT_WINDOW);
        textWin.setText(Integer.toString(DEFAULT_WINDOW));
    }

    private int getProgressSeekSamp(int millis, int radioId){
        switch (radioId){
            case R.id.Conf_radioMill:
                seekSamp.setMax(maxSeekSampMillis/STEP_MILLIS);
                return (millis-usedMinMillis)/STEP_MILLIS;
            case R.id.Conf_radioSec:
                seekSamp.setMax(MAX_SEEK_SEC-1);
                return (millis/MILLIS_IN_SEC)-1;
            case R.id.Conf_radioMin:
                seekSamp.setMax(MAX_SEEK_MIN-1);
                return (millis/MILLIS_IN_MIN)-1;
            case R.id.Conf_radioHour:
                seekSamp.setMax(MAX_SEEK_HOUR-1);
                return (millis/MILLIS_IN_HOUR)-1;
        }
        return 0;
    }

    private int getRightRadio(long millis){
        // MILLISECONDS
        if (millis < MILLIS_IN_SEC) {
            return R.id.Conf_radioMill;
        }
        // SECONDS
        else if ((millis >= MILLIS_IN_SEC) && (millis < MILLIS_IN_MIN)) {
            return R.id.Conf_radioSec;
        }
        // MINUTES
        else if ((millis >= MILLIS_IN_MIN) && (millis < MILLIS_IN_HOUR)) {
            return R.id.Conf_radioMin;
        }
        // HOURS
        else if (millis > MILLIS_IN_HOUR) {
            return R.id.Conf_radioHour;
        }
        //Default
        return R.id.Conf_radioSec;
    }

    private long getMillis(int progress, int radioId){
        switch (radioId){
            case R.id.Conf_radioMill:
                return progress*STEP_MILLIS+usedMinMillis;
            case R.id.Conf_radioSec:
                return (progress+1)*MILLIS_IN_SEC;
            case R.id.Conf_radioMin:
                return (progress+1)*MILLIS_IN_MIN;
            case R.id.Conf_radioHour:
                return (progress+1)*MILLIS_IN_HOUR;
        }
        return DEFAULT_INTERVAL;
    }

    private long getTextSampValue(int progress, int radioId){
        switch (radioId){
            case R.id.Conf_radioMill:
                return progress*STEP_MILLIS+usedMinMillis;
            case R.id.Conf_radioSec:
                return (progress+1);
            case R.id.Conf_radioMin:
                return (progress+1);
            case R.id.Conf_radioHour:
                return (progress+1);
        }
        return 1;
    }



    //TODO aggiungere metodo saveOrUpdate() che differenzia a seconda di getextras (persare se settare una variabile invece che usare 2 volte getextras
    //TODO implementare scrittura o modifica su stringa database

    //TODO aggiungere metodo delete con alert per confermare, implementare cancellazione riga dbHelper

}
