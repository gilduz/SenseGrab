package com.ukuke.gl.sensormind.support;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

/**
 * Created by gildoandreoni on 11/02/15.
 */


public class DeviceInfo {
    Context cn;

    public DeviceInfo(Context cn) {
        this.cn = cn;
    }

    public boolean isConnectedToWifi() {
        ConnectivityManager connManager = (ConnectivityManager) cn.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public boolean isPluggedIn() {
        Intent intent = cn.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }
}