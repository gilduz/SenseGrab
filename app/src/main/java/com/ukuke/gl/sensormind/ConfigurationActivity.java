package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
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
    private static final int MIN_MILLIS = 250;

    //Do not edit!
    private static final int MILLIS_IN_SEC = 1000;
    private static final int MILLIS_IN_MIN = 60*MILLIS_IN_SEC;
    private static final int MILLIS_IN_HOUR = 60*MILLIS_IN_MIN;
    private static final int MAX_SEEK_MILLIS = MILLIS_IN_SEC-STEP_MILLIS;
    private static final int MAX_SEEK_SEC = 59;
    private static final int MAX_SEEK_MIN = 59;
    private static final int MAX_SEEK_HOUR = 24;
    private static String TAG = ConfigurationActivity.class.getSimpleName();

    // EXTRAS VALUES
    public final static String CONFIGURATION_DB_ID = "configuration_db_id";
    private String DEFAULT_NAME = "Default_Name";

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
    RelativeLayout windowSetting;
    Menu menu;
    MenuItem load = null;
    MenuItem deactivate = null;

    // Initialize and set default value for seekBars
    long progressSamp = DEFAULT_INTERVAL;
    int progressWin = DEFAULT_WINDOW;

    // Variables
    private Intent intent;
    private int typeSensor;
    private boolean logging = true;
    private ServiceManager.ServiceComponent serviceComponent;
    private ServiceManager.ServiceComponent.Configuration configuration;
    private boolean isAModify;
    private int dbId;
    private int usedMinMillis;
    public int activeConfigurationId = -1;
    private int maxSeekSampMillis;

    // Others
    AlertDialog alertLoadList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        //-----------------------GRAPHIC ELEMENTS------------------------
        // seek bars and relative value views
        seekSamp = (SeekBar) findViewById(R.id.Conf_Sam_seekBar);
        textSamp = (TextView) findViewById(R.id.Conf_viewSeekbarValue);
        seekWin = (SeekBar) findViewById(R.id.Conf_Win_seekBar);
        textWin = (TextView) findViewById(R.id.Conf_viewWinSeekbarValue);

        // Relative layout
        windowSetting = (RelativeLayout) findViewById(R.id.Conf_windowSetting);

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
        //Radiogroup listener
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

        //-----------------------EXTRAS------------------------
        intent = getIntent();
        typeSensor = intent.getIntExtra(AddDeviceActivity.TYPE_SENSOR, Sensor.TYPE_LIGHT);
        isAModify = intent.getBooleanExtra(AddDeviceActivity.MODIFY_CONFIGURATION, false);// if it's not a modify set the default value false
        dbId = intent.getIntExtra(CONFIGURATION_DB_ID,-1);// if it's a new configuration set dbId with the default value -1

        //Set values from extras
        serviceComponent = ServiceManager.getInstance(ConfigurationActivity.this).getServiceComponentAvailableBySensorType(typeSensor);
        try {
            activeConfigurationId = serviceComponent.getActiveConfiguration().getDbId();
        } catch (Exception e) {}
        int relativeMinMicroS = serviceComponent.getMinDelay();
        if (relativeMinMicroS > 0){
            usedMinMillis = (int) Math.ceil((((double)relativeMinMicroS)/ 1000)/STEP_MILLIS)*STEP_MILLIS;
            Log.d(TAG,serviceComponent.getDysplayName()+" relativeMinMicros: "+relativeMinMicroS);
            Log.d(TAG,"Math.ceil((relativeMinMicroS)/ 1000): "+Math.ceil(((double)relativeMinMicroS)/ 1000));
            Log.d(TAG,"Math.ceil(((relativeMinMicroS)/ 1000)/STEP_MILLIS): "+Math.ceil((((double)relativeMinMicroS)/ 1000)/STEP_MILLIS));
            Log.d(TAG,"Math.ceil(((relativeMinMicroS)/ 1000)/STEP_MILLIS)*STEP_MILLIS: "+Math.ceil((((double)relativeMinMicroS)/ 1000)/STEP_MILLIS)*STEP_MILLIS);
            Log.d(TAG,serviceComponent.getDysplayName()+" usedMinMillis: "+usedMinMillis);
        } else usedMinMillis = MIN_MILLIS;
        maxSeekSampMillis = MAX_SEEK_MILLIS-usedMinMillis;

        setTitle(serviceComponent.getDysplayName());
        DEFAULT_NAME = serviceComponent.getDysplayName();

        // Hide window setting for not streaming sensors
        switch (typeSensor){
            // Not streaming sensors
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                windowSetting.setVisibility(View.GONE);
                break;
            default:
                break;
        }

        //TODO aggiungere il testo dentro a window per spiegare cos'è

        if (!isAModify) {
            //NEW CONFIGURATION
            buttonDelete.setVisibility(View.GONE);
            //configuration = new ServiceManager.ServiceComponent.Configuration();
            setDefaultValues();
        } else {
            //MODIFYING OLD CONFIGURATION
            setValuesFromConfigurationById(dbId);
        }

        // Set Listeners
        seekSamp.setOnSeekBarChangeListener(seekListener);
        seekWin.setOnSeekBarChangeListener(seekListener);
        radioGr.setOnCheckedChangeListener(radioListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
        // Hiding Load if necessary
        load = menu.findItem(R.id.Conf_load_conf);
        int size = serviceComponent.configurationList.size();
        if (size<1 || (size == 1 && isAModify)) {
            load.setVisible(false);
        }
        // Hiding Deactivate if necessary
        deactivate = menu.findItem(R.id.Conf_deactivate);
        if (dbId == activeConfigurationId && isAModify){
            deactivate.setVisible(true);
        } else deactivate.setVisible(false);
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
                alertLoadList();
                return true;
            case R.id.Conf_default_values:
                setDefaultValues();
                return true;
            case R.id.Conf_deactivate:
                ServiceManager.getInstance(ConfigurationActivity.this).addOrUpdateConfigurationServiceToDB(serviceComponent, serviceComponent.getActiveConfiguration(), false);
                ServiceManager.getInstance(ConfigurationActivity.this).stopScheduleService(serviceComponent);
                ServiceManager.getInstance(ConfigurationActivity.this).removeServiceComponentActive(typeSensor);
                serviceComponent.setActiveConfiguration(null);
                Toast.makeText(getApplicationContext(), "Service removed", Toast.LENGTH_LONG).show();
                activeConfigurationId=-1;
                deactivate.setVisible(false);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //---------------------------SAVE---------------------------------

    public void onButtonSaveClicked(View view) {

        long interval = DEFAULT_INTERVAL;

        int window = DEFAULT_WINDOW;

        try {
            interval = getMillis(seekSamp.getProgress(), radioGr.getCheckedRadioButtonId());
            window = seekWin.getProgress();
        } catch (Exception e) {}

        if (!isAModify) {
            // it's a new configuration, let's create it
            configuration = new ServiceManager.ServiceComponent.Configuration();
        } else {
            // it's an old configuration, let's remove it from the list before re-adding it
            serviceComponent.removeConfigurationByDbId(dbId);
        }

        //Set configuration values
        configuration.setInterval(interval);
        configuration.setWindow(window);
        configuration.setConfigurationName(confName.getText().toString());
        //configuration.setPath("test_1/v1/bm/Test"); //TODO Da aggiungere il path?
        configuration.setAttachGPS(gpsSwitch.isChecked());
        configuration.setPath(serviceComponent.getDefaultPath());
        configuration.setDbId(dbId);

        //Add configuration to the list
        serviceComponent.addConfiguration(configuration);

        //Alert for activation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to start sampling with this configuration?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //The user clicks Yes
                        //Deactivating old active configuration
                        if (dbId != activeConfigurationId && activeConfigurationId != -1) {
                            ServiceManager.getInstance(ConfigurationActivity.this).addOrUpdateConfigurationServiceToDB(serviceComponent, serviceComponent.getActiveConfiguration(), false);
                        }

                        //Activating current configuration
                        serviceComponent.setActiveConfiguration(configuration);
                        ServiceManager.getInstance(ConfigurationActivity.this).addServiceComponentActive(serviceComponent);
                        ServiceManager.getInstance(ConfigurationActivity.this).startScheduleService(serviceComponent);
                        ServiceManager.getInstance(ConfigurationActivity.this).addOrUpdateConfigurationServiceToDB(serviceComponent, configuration, true);

                        Toast.makeText(getApplicationContext(), "Service added", Toast.LENGTH_LONG).show();

                        backToMain();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // The user clicks No
                        ServiceManager.getInstance(ConfigurationActivity.this).addOrUpdateConfigurationServiceToDB(serviceComponent, configuration, false);

                        Toast.makeText(getApplicationContext(), "Configuration saved", Toast.LENGTH_LONG).show();

                        backToMain();
                    }
                });
        AlertDialog d = builder.create();
        d.setTitle("Saving configuration");
        d.show();
    }

    //---------------------------DELETE---------------------------------

    public void onButtonDeleteClicked(View view) {
        //Alert for confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this configuration? (this cannot be undone)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //The user click Yes
                        if (dbId == activeConfigurationId){
                            //I'm deleting the active configuration, so stop the service
                            ServiceManager.getInstance(ConfigurationActivity.this).stopScheduleService(serviceComponent);
                            ServiceManager.getInstance(ConfigurationActivity.this).removeServiceComponentActive(typeSensor);
                            serviceComponent.setActiveConfiguration(null);
                            Toast.makeText(getApplicationContext(), "Service removed", Toast.LENGTH_LONG).show();
                        }

                        //Removing from the list and from Db
                        serviceComponent.removeConfigurationByDbId(dbId);
                        ServiceManager.getInstance(ConfigurationActivity.this).removeConfigurationServiceToDB(configuration);

                        backToMain();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        AlertDialog d = builder.create();
        d.setTitle("Deleting configuration");
        d.show();

    }

    private void alertLoadList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ListView list = new ListView(getApplicationContext());
        list.setAdapter(new ConfigurationListAdapter());
        builder.setTitle("Load saved configuration");
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (alertLoadList.isShowing()) {
                    alertLoadList.dismiss();
                }
                isAModify = true;
                configuration = serviceComponent.configurationList.get(position);
                dbId = configuration.getDbId();
                setValuesFromConfigurationById(dbId);
                if (dbId == activeConfigurationId){ // In this case i have to show the menuitem as visible recreating the menu
                    deactivate.setVisible(true);
                }
                Toast.makeText(getApplicationContext(), "Loaded configuration: " +
                        configuration.getConfigurationName(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setView(list);
        builder.setNegativeButton("cancel",null);
        alertLoadList = builder.create();
        alertLoadList.show();
    }

    private class ConfigurationListAdapter extends ArrayAdapter<ServiceManager.ServiceComponent.Configuration> {
        public ConfigurationListAdapter() {
            super(getApplicationContext(), R.layout.alert_item_view, serviceComponent.configurationList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.alert_item_view, parent, false);
            }

            ServiceManager.ServiceComponent.Configuration currentConf = serviceComponent.configurationList.get(position);

            TextView myText = (TextView) itemView.findViewById(R.id.item_alertTextView);
            myText.setText(currentConf.getConfigurationName());

            return itemView;
            //return super.getView(position, convertView, parent);
        }
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

    private void setValuesFromConfigurationById (int DbId){
        if (DbId > -1 && serviceComponent.getConfigurationByDbId(DbId) != null) {
            configuration = serviceComponent.getConfigurationByDbId(DbId);
        } else configuration = serviceComponent.getActiveConfiguration();

        if (configuration!=null) {
            confName.setText(configuration.getConfigurationName());

            long millis = configuration.getInterval();
            //Log.d(TAG,"L'intervallo della configurazione attiva è: "+millis+" millisecondi");
            int rightRadio = getRightRadio(millis);
            int sampSeekValue = getProgressSeekSamp((int) millis, rightRadio);
            radioGr.check(rightRadio);
            seekSamp.setProgress(sampSeekValue);
            textSamp.setText(Long.toString(getTextSampValue(sampSeekValue, rightRadio)));
            seekWin.setProgress(configuration.getWindow());
            textWin.setText(Long.toString(configuration.getWindow()));

            confName.setText(configuration.getConfigurationName());
            gpsSwitch.setChecked(configuration.getAttachGPS());
        } else setDefaultValues();
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

    private void backToMain(){
        //Back to main
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
    }
}
