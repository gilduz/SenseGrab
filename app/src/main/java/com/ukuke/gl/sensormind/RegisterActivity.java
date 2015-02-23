package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ukuke.gl.sensormind.services.MQTTService;
import com.ukuke.gl.sensormind.support.SensormindAPI;

public class RegisterActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = RegisterActivity.class.getSimpleName();
    EditText editText_username;
    EditText editText_password;
    EditText editText_firstname;
    EditText editText_lastname;
    EditText editText_email;
    EditText editText_password_bis;
    String timezone = "55";

    SharedPreferences prefs = null;
    boolean validRegistration = false;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        editText_username = (EditText) findViewById(R.id.editText_username);
        editText_password = (EditText) findViewById(R.id.editText_password);
        editText_password_bis = (EditText) findViewById(R.id.editText_password_bis);
        editText_firstname = (EditText) findViewById(R.id.editText_firstName);
        editText_lastname = (EditText) findViewById(R.id.editText_lastName);
        editText_email = (EditText) findViewById(R.id.editText_email);

        // Timezone: set the spinner from array resource
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.timezone_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        // Set the listener, in this case this activity implements the listener
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Actually there isn't a menu in this activity
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //Back to main
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void onClickedRegisterButton(View view) {
        // Check if password is repeated correctly
        if (editText_password.getText().toString().compareTo(editText_password_bis.getText().toString()) == 0) {
            // Perform registration on asynk task
            new register_asynk().execute();
        }
        else {
            Toast.makeText(getApplicationContext(), "Please check the password", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickedImage(View view) {
        // Go to sensormind web site
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.URL_BROWSER_SENSORMIND));
        startActivity(browserIntent);
    }

    // Listener method for timezone spinner
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        timezone = Integer.toString(pos+1);
        if (prefs.getBoolean("HEAVY_LOG",false)) {
            Log.d(TAG, "Timezone selected: " + timezone);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    private class register_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            SensormindAPI API = new SensormindAPI(editText_username.getText().toString(),editText_password.getText().toString());
            validRegistration = API.registerNewAccount(editText_firstname.getText().toString(), editText_lastname.getText().toString(), timezone, editText_email.getText().toString());
            Log.d(TAG, "Request Registration for: " + editText_username.getText().toString());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            // if registration fails put false in logged in, otherwise put true
            prefs.edit().putBoolean("loggedIn", validRegistration).apply();

            if (validRegistration) {
                Log.d(TAG,"Registration successful");
                // Registration performed
                Toast.makeText(getApplicationContext(), "Registration succeed!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.getText().toString()).apply();
                prefs.edit().putString("password",editText_password.getText().toString()).apply();
                // try to create device feeds (if they already exist nothing happens)
                ServiceManager.getInstance(RegisterActivity.this).createDeviceFeeds();
                launchMQTTService();
                // Back to main
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
            else {
                Log.d(TAG,"Registration failed");
                Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private void launchMQTTService() {
        if (prefs.getBoolean("loggedIn",false)) {
            Log.d(TAG, "Activate Mqtt service");
            // Set repeating alarm for data sync getting the sync interval from shared preferences
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MQTTService.class);

            PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Integer.parseInt(prefs.getString("syncFrequency","1800")) * 1000, scheduledIntent);
        }
        else {
            Log.d(TAG,"You need to login or register before send data via MQTT");
        }
    }
}
