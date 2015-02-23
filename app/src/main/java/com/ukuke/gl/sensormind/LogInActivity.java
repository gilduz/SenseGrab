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
import android.widget.EditText;
import android.widget.Toast;

import com.ukuke.gl.sensormind.services.MQTTService;
import com.ukuke.gl.sensormind.support.SensormindAPI;

public class LogInActivity extends Activity {

    private static final String TAG = LogInActivity.class.getSimpleName();

    EditText editText_username;
    EditText editText_password;

    SharedPreferences prefs = null;
    boolean validLogIn = false;
    //private ImageView image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //image = (ImageView) findViewById(R.id.imageView);
        //image.setImageResource(R.drawable.sensormind_logo);
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

    public void onClickedImage(View view) {
        // Go to sensormind site
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.URL_BROWSER_SENSORMIND));
        startActivity(browserIntent);
    }

    public void onClickedRegisterButton(View view) {
        // Go to RegisterActivity
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void onClickedLogInButton(View view) {
        editText_username = (EditText) findViewById(R.id.editText_username);
        editText_password = (EditText) findViewById(R.id.editText_password);
        // Set variables in shared preferences, as default values while waiting for the result of asynk task
        prefs.edit().putBoolean("loggedIn",validLogIn).apply();
        prefs.edit().putString("username",editText_username.getText().toString()).apply();
        prefs.edit().putString("password",editText_password.getText().toString()).apply();
        // Perform login on asynk task
        new logIn_asynk().execute();
    }

    private class logIn_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            // Make http request login via Sensormind API
            SensormindAPI API = new SensormindAPI(editText_username.getText().toString(),editText_password.getText().toString());
            validLogIn = API.checkCredentials(editText_username.getText().toString(),editText_password.getText().toString());
            Log.d(TAG, "Request login for: " + editText_username.getText().toString());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            // if credentials are not correct put false in logged in, otherwise put true
            prefs.edit().putBoolean("loggedIn", validLogIn).apply();

            if (validLogIn) {
                // Credentials are correct
                Log.d(TAG,"Log in succeed!");
                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.getText().toString()).apply();
                prefs.edit().putString("password",editText_password.getText().toString()).apply();
                // try to create device feeds (if they already exist nothing happens)
                ServiceManager.getInstance(LogInActivity.this).createDeviceFeeds();
                launchMQTTService();
                // Back to main
                Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                // Credentials are not correct or something wrong happened
                Log.d(TAG,"Login failed");
                prefs.edit().putString("username","NULL").apply();
                prefs.edit().putString("password","NULL").apply();
                Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_LONG).show();
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
            scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Integer.parseInt(prefs.getString("syncFrequency","1800")) * 1000, scheduledIntent);
        }
        else {
            Log.d(TAG,"You need to login or register before send data via MQTT");
        }
    }


}
