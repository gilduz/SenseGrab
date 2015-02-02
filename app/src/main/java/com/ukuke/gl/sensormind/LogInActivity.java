package com.ukuke.gl.sensormind;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ukuke.gl.sensormind.R;
import com.ukuke.gl.sensormind.services.SensorBackgroundService;
import com.ukuke.gl.sensormind.support.SensormindAPI;

public class LogInActivity extends Activity {

    SensormindAPI API;
    private static final String TAG = LogInActivity.class.getSimpleName();

    EditText editText_username;
    EditText editText_password;

    SharedPreferences prefs = null;
    boolean validLogIn = true; //TODO: Da implementare


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);
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
        //prefs.edit().putBoolean("CICCIO",true).apply();
        prefs.edit().commit();
        Log.d(TAG,"VALORE: " + prefs.contains("username"));
        Log.d(TAG,"VALORE: " + prefs.contains("password"));
        //new logIn_asynk().execute();
    }

    private class logIn_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            boolean result;
            // result = API.registerNewAccount("Test_2_Name", "Test_2_Surname", "55", "test_2@test.com");
            Log.d(TAG, "Request login for: " + editText_username.getText().toString());
            //TODO: Per loggarsi come si fa? non serve... Serve un sistema per vedere se l'username esiste
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            //TODO: Controllare il result della connessione e impostare logged o no

            prefs.edit().putBoolean("loggedIn", validLogIn).apply();

            if (validLogIn) {
                Log.d(TAG,"Registration succeed!");
                Toast.makeText(getApplicationContext(), "Registration succeed!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.toString()).apply();
                prefs.edit().putString("password",editText_password.toString()).apply();


            }
            else {
                Log.d(TAG,"Registration failed!");
                Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_LONG).show();
            }
            prefs.edit().commit();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
