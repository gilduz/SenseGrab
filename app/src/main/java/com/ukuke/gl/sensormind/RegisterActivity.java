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

public class RegisterActivity extends Activity {

    SensormindAPI API;
    private static final String TAG = RegisterActivity.class.getSimpleName();

    EditText editText_username;
    EditText editText_password;
    EditText editText_firstname;
    EditText editText_lastname;
    EditText editText_email;

    String username;

    SharedPreferences prefs = null;
    boolean validRegistration = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
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
        editText_username = (EditText) findViewById(R.id.editText_username);
        editText_password = (EditText) findViewById(R.id.editText_password);
        editText_firstname = (EditText) findViewById(R.id.editText_firstName);
        editText_lastname = (EditText) findViewById(R.id.editText_lastName);
        editText_email = (EditText) findViewById(R.id.editText_email);

        new logIn_asynk().execute();
    }

    private class logIn_asynk extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            SensormindAPI API = new SensormindAPI(editText_username.getText().toString(),editText_password.getText().toString());
            validRegistration = API.registerNewAccount(editText_firstname.getText().toString(), editText_lastname.getText().toString(), "55", editText_email.getText().toString());
            Log.d(TAG, "Request Registration for: " + editText_username.getText().toString());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            prefs.edit().putBoolean("loggedIn", validRegistration).commit();

            if (validRegistration) {
                Log.d(TAG,"Registration succeed!");
                Toast.makeText(getApplicationContext(), "Registration succeed!", Toast.LENGTH_LONG).show();
                prefs.edit().putString("username",editText_username.getText().toString()).apply();
                prefs.edit().putString("password",editText_password.getText().toString()).apply();

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
