package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ukuke.gl.sensormind.R;
import com.ukuke.gl.sensormind.services.MQTTService;
import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.support.SensormindAPI;

import java.util.List;

public class LogInActivity extends Activity {

    SensormindAPI API;
    private static final String TAG = LogInActivity.class.getSimpleName();

    EditText editText_username;
    EditText editText_password;

    SharedPreferences prefs = null;
    boolean validLogIn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }


    public void onClickedLogInButton(View view) {
        editText_username = (EditText) findViewById(R.id.editText_username);
        editText_password = (EditText) findViewById(R.id.editText_password);
        prefs.edit().putBoolean("loggedIn",validLogIn).apply();
        prefs.edit().putString("username",editText_username.getText().toString()).apply();//editText_username.getText().toString());
        prefs.edit().putString("password",editText_password.getText().toString()).apply();
        prefs.edit().commit();

        new logIn_asynk().execute();
    }



    private class logIn_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            boolean result;
            SensormindAPI API = new SensormindAPI(editText_username.getText().toString(),editText_password.getText().toString());
            validLogIn = API.checkCredentials(editText_username.getText().toString(),editText_password.getText().toString());
            Log.d(TAG, "Request login for: " + editText_username.getText().toString());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            prefs.edit().putBoolean("loggedIn", validLogIn).apply();

            if (validLogIn) {
                Log.d(TAG,"Log in succeed!");
                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.getText().toString()).apply();
                prefs.edit().putString("password",editText_password.getText().toString()).apply();
                prefs.edit().commit();
                ServiceManager.getInstance(LogInActivity.this).createDeviceFeeds();
                launchMQTTService();
                Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                Log.d(TAG,"Login failed");
                prefs.edit().putString("username","NULL").apply();
                prefs.edit().putString("password","NULL").apply();
                Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_LONG).show();
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
