// TODO LEO! Nel xml mi sistemi il fatto che l'immagine sale con la tastiera invece di rimanere ancorata al fondo?! thanks

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
import android.support.v7.app.ActionBarActivity;
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

import com.ukuke.gl.sensormind.R;
import com.ukuke.gl.sensormind.services.MQTTService;
import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.support.SensormindAPI;

import java.security.Provider;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class RegisterActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = RegisterActivity.class.getSimpleName();
    SensormindAPI API;
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

        // Timezone
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.timezone_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
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

    public void onClickedRegisterButton(View view) {
        if (editText_password.getText().toString().compareTo(editText_password_bis.getText().toString()) == 0) {
            new logIn_asynk().execute();
        }
        else {
            Toast.makeText(getApplicationContext(), "Please check the password", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickedImage(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.URL_BROWSER_SENSORMIND));
        startActivity(browserIntent);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        timezone = Integer.toString(pos+1);
        if (prefs.getBoolean("HEAVY_LOG",false)) {
            Log.d(TAG, "Timezone selected: " + timezone);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    private class logIn_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            SensormindAPI API = new SensormindAPI(editText_username.getText().toString(),editText_password.getText().toString());
            validRegistration = API.registerNewAccount(editText_firstname.getText().toString(), editText_lastname.getText().toString(), timezone, editText_email.getText().toString());
            Log.d(TAG, "Request Registration for: " + editText_username.getText().toString());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            prefs.edit().putBoolean("loggedIn", validRegistration).apply();

            if (validRegistration) {
                Log.d(TAG,"Registration successful");
                Toast.makeText(getApplicationContext(), "Registration succeed!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.getText().toString()).apply();
                prefs.edit().putString("password",editText_password.getText().toString()).apply();
                prefs.edit().commit();
                ServiceManager.getInstance(RegisterActivity.this).createDeviceFeeds();
                launchMQTTService();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
            else {
                Log.d(TAG,"Registration failed");
                Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_LONG).show();
            }
            prefs.edit().commit();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private void launchMQTTService() {
        if (prefs.getBoolean("loggedIn",false)) {
            Log.d(TAG, "Activate Mqtt service");
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
