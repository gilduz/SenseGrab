package com.ukuke.gl.sensormind.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.ActivityRecognitionResultCreator;
import com.google.android.gms.location.DetectedActivity;

//public static final int IN_VEHICLE
//        The device is in a vehicle, such as a car.
//        Constant Value: 0 (0x00000000)
//public static final int ON_BICYCLE
//        The device is on a bicycle.
//        Constant Value: 1 (0x00000001)
//public static final int ON_FOOT
//        The device is on a user who is walking or running.
//        Constant Value: 2 (0x00000002)
//public static final int RUNNING
//        The device is on a user who is running. This is a sub-activity of ON_FOOT.
//        Constant Value: 8 (0x00000008)
//public static final int STILL
//        The device is still (not moving).
//        Constant Value: 3 (0x00000003)
//public static final int TILTING
//        The device angle relative to gravity changed significantly. This often occurs when a device is picked up from a desk or a user who is sitting stands up.
//        Constant Value: 5 (0x00000005)
//public static final int UNKNOWN
//        Unable to detect the current activity.
//        Constant Value: 4 (0x00000004)
//public static final int WALKING
//        The device is on a user who is walking. This is a sub-activity of ON_FOOT.
//        Constant Value: 7 (0x00000007)



public class ActivityRecognitionIntentService extends IntentService {


    private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();
    public static final String KEY_ACTIVITY_IN_VEHICLE =         "activity_in_vehicle";
    public static final String KEY_ACTIVITY_ON_BICYCLE =         "activity_on_bicycle";
    public static final String KEY_ACTIVITY_ON_FOOT =            "activity_on_foot";
    public static final String KEY_ACTIVITY_RUNNING =            "activity_running";
    public static final String KEY_ACTIVITY_STILL =              "activity_still";
    public static final String KEY_ACTIVITY_TILTING =            "activity_tilting";
    public static final String KEY_ACTIVITY_UNKNOWN =            "activity_unknown";
    public static final String KEY_ACTIVITY_WALKING =            "activity_walking";
    public static final String KEY_MOST_PROBABLE_ACTIVITY =            "most_probable_activity";

    public static final String KEY_BROADCAST_RESULT = "broadcast_result";

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            int activity_in_vehicle = result.getActivityConfidence(DetectedActivity.IN_VEHICLE);
            int activity_on_bicycle = result.getActivityConfidence(DetectedActivity.ON_BICYCLE);
            int activity_on_foot = result.getActivityConfidence(DetectedActivity.ON_FOOT);
            int activity_running = result.getActivityConfidence(DetectedActivity.RUNNING);
            int activity_still = result.getActivityConfidence(DetectedActivity.STILL);
            int activity_tilting = result.getActivityConfidence(DetectedActivity.TILTING);
            int activity_unknown = result.getActivityConfidence(DetectedActivity.UNKNOWN);
            int activity_walking = result.getActivityConfidence(DetectedActivity.WALKING);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            Intent intent_2 = new Intent();
            intent_2.putExtra(KEY_ACTIVITY_IN_VEHICLE, activity_in_vehicle);
            intent_2.putExtra(KEY_ACTIVITY_ON_BICYCLE, activity_on_bicycle);
            intent_2.putExtra(KEY_ACTIVITY_ON_FOOT, activity_on_foot);
            intent_2.putExtra(KEY_ACTIVITY_RUNNING, activity_running);
            intent_2.putExtra(KEY_ACTIVITY_STILL, activity_still);
            intent_2.putExtra(KEY_ACTIVITY_TILTING, activity_tilting);
            intent_2.putExtra(KEY_ACTIVITY_UNKNOWN, activity_unknown);
            intent_2.putExtra(KEY_ACTIVITY_WALKING, activity_walking);
            intent_2.putExtra(KEY_MOST_PROBABLE_ACTIVITY, mostProbableActivity.getType());


            intent_2.setAction(KEY_BROADCAST_RESULT);
            intent_2.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(intent_2);
//            Bundle args = intent_2.getExtras();
//            ResultReceiver resultReceiver = args.getParcelable("receiver");
//            resultReceiver.send(100, args);
        }
        else {
            Log.d(TAG, "No activity recognition data" );
        }
    }
}
