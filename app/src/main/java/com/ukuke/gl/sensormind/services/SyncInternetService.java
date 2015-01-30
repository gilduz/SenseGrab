package com.ukuke.gl.sensormind.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

/**
 * for a background service not linked to an activity it's important to use the command approach
 * instead of the Binder. For starting use the alarm manager
 */
public class SyncInternetService extends Service{

    private static final String TAG = SyncInternetService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // ignore this since not linked to an activity
        return null;
    }


    @Override
    public void onDestroy()
    {
        Toast.makeText(this, TAG + " stopped", Toast.LENGTH_LONG).show();
    }

}