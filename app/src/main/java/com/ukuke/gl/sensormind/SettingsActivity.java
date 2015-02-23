package com.ukuke.gl.sensormind;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.ukuke.gl.sensormind.services.MQTTService;

/**
 * Settings activity created modifying standard code
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = true; // same settings for smartphones and tablets
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Sync preferencies
        addPreferencesFromResource(R.xml.pref_data_sync);

        // Sync preferencies
        addPreferencesFromResource(R.xml.pref_data_db);

        // Set value on summary text under preference title for db freq and sync freq
        bindPreferenceSummaryToValue(findPreference("dbFrequency"));
        bindPreferenceSummaryToValue(findPreference("syncFrequency"));
    }

    /**
     * {@inheritDoc}
     */
    // UNUSED
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    // UNUSED
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    // UNUSED
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Listener for restarting alarms with new values whenever a key changes
        switch (key) {
            case "syncFrequency":
                //Log.d(TAG,"New value: "+key+" "+sharedPreferences.getString(key,"300"));
                // Launch MQTT Service with new sync frequency value and set transfer to db interval
                launchMQTTService(sharedPreferences);
                int syncFreq = Integer.parseInt(sharedPreferences.getString("syncFrequency","300"));
                int dbTransf = 300;
                if (syncFreq <= 301) {
                    dbTransf = syncFreq - 2; //set the interval as a bit less of the other interval
                }
                ServiceManager.getInstance(this).setTransferToDbInterval(dbTransf);
                break;
            case "dbFrequency" :
                //Log.d(TAG,"New value: "+key+" "+sharedPreferences.getString(key,"1800"));
                // Set interval for deletind old data from db
                ServiceManager.getInstance(this)
                        .setDeleteOldDataInterval(Integer.parseInt(sharedPreferences.getString(key,"1800")));
                sharedPreferences.edit().putLong("last_delete",System.currentTimeMillis()).apply();
                //Log.d(TAG,"New value: "+key+" "+Integer.parseInt(sharedPreferences.getString(key,"1800")));
                break;
            default:
                break;
        }

    }

    private void launchMQTTService(SharedPreferences prefs) {
        if (prefs.getBoolean("loggedIn",false)) {
            Log.d(TAG, "Activate Mqtt service");
            // Set repeating alarm for data sync getting the sync interval from shared preferences
            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MQTTService.class);
            //Log.d(TAG,"New value: syncFrequency "+Integer.parseInt(prefs.getString("syncFrequency","300")));
            PendingIntent scheduledIntent = PendingIntent.getService(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Integer.parseInt(prefs.getString("syncFrequency","300")) * 1000, scheduledIntent);
        }
        else {
            Log.d(TAG,"You need to login or register before send data via MQTT");
        }
    }

    // FRAGMENT SETTING ARE NOT USED, IF YOU WANT TO USE THESE FOR TABLETS YOU NEED TO MODIFY THEM
    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }
    }

    *//**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     *//*
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

    *//**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     *//*
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }*/
}
