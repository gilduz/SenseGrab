package com.ukuke.gl.sensormind.services;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ukuke.gl.sensormind.DataDbHelper;
import com.ukuke.gl.sensormind.MainActivity;
import com.ukuke.gl.sensormind.R;
import com.ukuke.gl.sensormind.support.DataSample;


public class MQTTService extends Service
{
    private static final String TAG = MQTTService.class.getSimpleName();

    private static boolean serviceRunning = false;
    private static int mid = 0;
    private static MQTTConnection connection = null;
    private final Messenger clientMessenger = new Messenger(new ClientHandler());
    SharedPreferences prefs = null;

    private boolean isLoggedIn;
    private String username = "NULL";
    private String password = "NULL";
    private String ipMqtt;
    private int portMqtt;

    @Override
    public void onCreate()
    {
        super.onCreate();
        connection = new MQTTConnection();

        //connection.start(); // Spostato da onstartcommand
        //dataDbHelper = new DataDbHelper(this);

        // Get shared preferences
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);


        ipMqtt = prefs.getString("ip_MQTT","123456");
        portMqtt = prefs.getInt("port_MQTT",123456);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!isRunning())
        {
            connection.start();
        }

        boolean isLoggedIn;


        //Log.d(TAG, "USERNAME" + prefs.getString("username", "NULL"));

        isLoggedIn = prefs.getBoolean("loggedIn", false);

        if (isLoggedIn) {
            username = prefs.getString("username", "NULL");
            password = prefs.getString("password", "NULL");
            //syncWithSensormind();

            Message msg = Message.obtain(null, MQTTConnection.MIO_PUBLISH_TUTTI);

            connection.makeRequest(msg);

        }
        else {
            Log.d(TAG, "No credentials stored in sharedpreferences");
        }
        return START_STICKY;
    }

    private void syncTEST_2() {

        DataDbHelper dataDbHelper = null;
        dataDbHelper = new DataDbHelper(this);
        List<DataSample> listData = new ArrayList<>();

        if (dataDbHelper.numberOfUnsentEntries() > 0) {
            listData = dataDbHelper.getAllUnsentDataSamples();
        }



        for (int i = 0; i < listData.size(); i++) {
            JSONObject obj = new JSONObject();
            DataSample sample = listData.get(i);

            try {
                obj.put("d", sample.getValue_1());

            } catch (Exception e) {}

            Bundle data = new Bundle();

            String message = obj.toString();
            //Log.d(TAG,"onStartCommand: " + message);

            data.putCharSequence(TOPIC, "/test_1/v1/bm/Test");
            data.putCharSequence(MESSAGE, message);
            Message msg = Message.obtain(null, PUBLISH);
            msg.setData(data);

            connection.makeRequest(msg);
        }


        if (listData.size()>0) {
            Log.d(TAG, "Sent to sensormind " + listData.size() + " samples");
            dataDbHelper.deleteAllDataSamples();
        }
    }

    private void syncWithSensormind() {

        // POI SPOSTALI DA QUI!!!
        DataDbHelper dataDbHelper = null;
        dataDbHelper = new DataDbHelper(this);

        List<DataSample> listData = new ArrayList<>();

        // Multi sample send
        try {
            //for (int j = 0; j < dataDbHelper.numberOfUnsentArrays() - 1; j++) {

            //}
            while (dataDbHelper.numberOfUnsentArrays() > 1) {
                DataSample sample;

                //Log.d(TAG, "Size List Data: " + listData.size());

                listData = dataDbHelper.getFirstUnsentArrayDataSamples();
                JSONArray array_1 = new JSONArray();
                JSONArray array_2 = new JSONArray();
                JSONArray array_3 = new JSONArray();

                // Split vector in 3 different arrays
                for (int i = 0; i < listData.size(); i++) {
                    sample = listData.get(i);
                    array_1.put(sample.getValue_1());
                    array_2.put(sample.getValue_2());
                    array_3.put(sample.getValue_3());
                }

                JSONObject obj_1 = new JSONObject();
                JSONObject obj_2 = new JSONObject();
                JSONObject obj_3 = new JSONObject();

                obj_1.put("d", array_1);
                obj_2.put("d", array_2);
                obj_3.put("d", array_3);

                JSONArray arrayCoord = new JSONArray();
                if ((listData.get(0).getLatitude() != null) && (listData.get(0).getLongitude() != null)) {
                    arrayCoord.put(listData.get(0).getLatitude());
                    arrayCoord.put(listData.get(0).getLongitude());
                    arrayCoord.put(0);
                    obj_1.put("l", arrayCoord);
                    obj_2.put("l", arrayCoord);
                    obj_3.put("l", arrayCoord);
                }

                if (listData.get(0).getTimestamp() != null) {
                    obj_1.put("t", listData.get(0).getTimestamp());
                    obj_2.put("t", listData.get(0).getTimestamp());
                    obj_3.put("t", listData.get(0).getTimestamp());
                }

                String path = "/" + username + "/v1/bm/" + listData.get(0).getFeedPath();

                // Invio 1
                Bundle data_1 = new Bundle();
                String message_1 = obj_1.toString();
                //Log.d(TAG,"onStartCommand: " + message);
                data_1.putCharSequence(TOPIC, path + "/1");
                data_1.putCharSequence(MESSAGE, message_1);
                Message msg_1 = Message.obtain(null, PUBLISH);
                msg_1.setData(data_1);
                connection.makeRequest(msg_1);

                // Invio 2
                Bundle data_2 = new Bundle();
                String message_2 = obj_2.toString();
                //Log.d(TAG,"onStartCommand: " + message);
                data_2.putCharSequence(TOPIC, path + "/2");
                data_2.putCharSequence(MESSAGE, message_2);
                Message msg_2 = Message.obtain(null, PUBLISH);
                msg_2.setData(data_2);
                connection.makeRequest(msg_2);

                // Invio 3
                Bundle data_3 = new Bundle();
                String message_3 = obj_3.toString();
                //Log.d(TAG,"onStartCommand: " + message);
                data_3.putCharSequence(TOPIC, path + "/3");
                data_3.putCharSequence(MESSAGE, message_3);
                Message msg_3 = Message.obtain(null, PUBLISH);
                msg_3.setData(data_3);
                connection.makeRequest(msg_3);

                // Scrivo su DB che i dati sono stati inviati

                Log.d(TAG, "Richiedo cancellazione di " + listData.size() + " samples al DB");
                dataDbHelper.setSentListOfDataSamples(listData);
                Log.d(TAG, "Richiesta cancellazione");
            }
        } catch (Exception e) { Log.e(TAG, "Error trying to send an array: " + e);}

        if (listData.size()>0) {
            Log.d(TAG, "Sent to sensormind " + listData.size() + " arrays");
            //dataDbHelper.setSentListOfDataSamples(listData);
        }

        // Single sample send
        try {
            if (dataDbHelper.numberOfUnsentEntries() > 0) {
                listData = dataDbHelper.getAllUnsentSingleDataSamples();
                for (int i = 0; i < listData.size(); i++) {

                    DataSample sample = listData.get(i);
                    JSONObject obj = new JSONObject();


                    obj.put("d", sample.getValue_1());

                    if ((sample.getLatitude() != null) && (sample.getLongitude() != null)) {
                        JSONArray array = new JSONArray();
                        array.put(sample.getLatitude());
                        array.put(sample.getLongitude());
                        array.put(0);
                        obj.put("l", array);
                    }
                    if (sample.getTimestamp() != null) {
                        obj.put("t", sample.getTimestamp());
                    }

                    Bundle data = new Bundle();

                    String message = obj.toString();
                    //Log.d(TAG, message);
                    //Log.d(TAG, sample.getFeedPath());

                    String path = "/" + username + "/v1/bm/" + sample.getFeedPath();
                    data.putCharSequence(TOPIC, path);
                    data.putCharSequence(MESSAGE, message);
                    Message msg = Message.obtain(null, PUBLISH);
                    msg.setData(data);

                    connection.makeRequest(msg);
                }
                if (listData.size()>0) {
                    Log.d(TAG, "Sent to sensormind " + listData.size() + " single samples");
                    dataDbHelper.setSentListOfDataSamples(listData);
                }
            }


        }catch (Exception e) {Log.d(TAG, "Errore nel publish singolo sample" + e);}



    }



    @Override
    public void onDestroy()
    {
        connection.end();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
		/*
		 * Return a reference to our client handler.
		 */
        return  clientMessenger.getBinder();
    }

    private synchronized static boolean isRunning()
    {
		 /*
		  * Only run one instance of the service.
		  */
        if (serviceRunning == false)
        {
            serviceRunning = true;
            return false;
        }
        else
        {
            return true;
        }
    }

    /*
     * These are the supported messages from bound clients
     */
    public static final int REGISTER = 0;
    public static final int SUBSCRIBE = 1;
    public static final int PUBLISH = 2;

    /*
     * Fixed strings for the supported messages.
     */
    public static final String TOPIC = "topic";
    public static final String MESSAGE = "message";
    public static final String STATUS = "status";
    public static final String CLASSNAME = "classname";
    public static final String INTENTNAME = "intentname";

    /*
     * This class handles messages sent to the service by
     * bound clients.
     */
    class ClientHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            boolean status = false;

            switch (msg.what)
            {
                //case SUBSCRIBE: // Not used now
                case PUBLISH:
           		 	/*
           		 	 * These two requests should be handled by
           		 	 * the connection thread, call makeRequest
           		 	 */
                    connection.makeRequest(msg);
                    break;
//                case REGISTER: // Not used now
//                {
//                    Bundle b = msg.getData();
//                    if (b != null)
//                    {
//                        Object target = b.getSerializable(CLASSNAME);
//                        if (target != null)
//                        {
//        				 /*
//        				  * This request can be handled in-line
//        				  * call the API
//        				  */
//                            connection.setPushCallback((Class<?>) target);
//                            status = true;
//                        }
//                        CharSequence cs = b.getCharSequence(INTENTNAME);
//                        if (cs != null)
//                        {
//                            String name = cs.toString().trim();
//                            if (name.isEmpty() == false)
//                            {
//            				 /*
//            				  * This request can be handled in-line
//            				  * call the API
//            				  */
//                                connection.setIntentName(name);
//                                status = true;
//                            }
//                        }
//                    }
//                    ReplytoClient(msg.replyTo, msg.what, status);
//                    break;
//                }
            }
        }
    }

    private void ReplytoClient(Messenger responseMessenger, int type, boolean status)
    {
		 /*
		  * A response can be sent back to a requester when
		  * the replyTo field is set in a Message, passed to this
		  * method as the first parameter.
		  */
        if (responseMessenger != null)
        {
            Bundle data = new Bundle();
            data.putBoolean(STATUS, status);
            Message reply = Message.obtain(null, type);
            reply.setData(data);

            try {
                responseMessenger.send(reply);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    enum CONNECT_STATE
    {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private class MQTTConnection extends Thread
    {
        private Class<?> launchActivity = null;
        private String intentName = null;
        private MsgHandler msgHandler = null;
        private static final int STOP = PUBLISH + 1;
        private static final int CONNECT = PUBLISH + 2;
        private static final int RESETTIMER = PUBLISH + 3;
        public static final int MIO_PUBLISH_TUTTI = 10;


        private CONNECT_STATE connState = CONNECT_STATE.DISCONNECTED;

        private void syncTEST() {

            DataDbHelper dataDbHelper = null;
            dataDbHelper = new DataDbHelper(getApplicationContext());

            List<DataSample> listData = new ArrayList<>();

            if (dataDbHelper.numberOfUnsentEntries() > 0) {
                listData = dataDbHelper.getAllUnsentSingleDataSamples();
            }



            for (int i = 0; i < listData.size(); i++) {
                JSONObject obj = new JSONObject();
                DataSample sample = listData.get(i);

                try {
                    obj.put("d", sample.getValue_1());

                } catch (Exception e) {}

                Bundle data = new Bundle();

                String message = obj.toString();
                //Log.d(TAG,"onStartCommand: " + message);

                String path = "/" + username + "/v1/bm/" + sample.getFeedPath();

                data.putCharSequence(TOPIC, path);
                data.putCharSequence(MESSAGE, message);
                Message msg = Message.obtain(null, PUBLISH);
                msg.setData(data);

                connection.makeRequest(msg);
            }

            if (listData.size()>0) {
                Log.d(TAG, "Sent to sensormind " + listData.size() + " samples");
                dataDbHelper.deleteAllDataSamples();
            }
        }

        MQTTConnection()
        {
            msgHandler = new MsgHandler();
            msgHandler.sendMessage(Message.obtain(null, CONNECT));
        }

        public void end()
        {
            msgHandler.sendMessage(Message.obtain(null, STOP));
        }

        public void makeRequest(Message msg)
        {
			/*
			 * It is expected that the caller only invokes
			 * this method with valid msg.what.
			 */
            msgHandler.sendMessage(Message.obtain(msg));
        }

        public void setPushCallback(Class<?> activityClass)
        {
            launchActivity = activityClass;
        }

        public void setIntentName(String name)
        {
            intentName = name;
        }

        private class MsgHandler extends Handler implements MqttCallback
        {
            private final String HOST = "137.204.213.190";//ipMqtt;
            private final int PORT = 1884;//portMqtt;
            private final String uri = "tcp://" + HOST + ":" + PORT;
            private final int MINTIMEOUT = 2000;
            private final int MAXTIMEOUT = 32000;
            private int timeout = MINTIMEOUT;
            private MqttClient client = null;
            private MqttConnectOptions options = new MqttConnectOptions();
            private Vector<String> topics = new Vector<String>();


            MsgHandler()
            {
                options.setCleanSession(true);
                try
                {

                    client = new MqttClient(uri, username, null);
                    //client = new MqttClient(uri, MqttClient.generateClientId(), null);
                    client.setCallback(this);

                }
                catch (MqttException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case STOP:
                    {
					/*
					 * Clean up, and terminate.
					 */
                        client.setCallback(null);
                        if (client.isConnected())
                        {
                            try {
                                client.disconnect();
                                client.close();
                            } catch (MqttException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        getLooper().quit();
                        break;
                    }
                    case CONNECT:
                    {
                        if (connState != CONNECT_STATE.CONNECTED)
                        {
                            try
                            {
                                //options.setUserName(username);
                                //options.setPassword(password.toCharArray());
                                options.setUserName(username);
                                options.setPassword(password.toCharArray());

                                client.connect(options);
                                connState = CONNECT_STATE.CONNECTED;
                                Log.d(TAG, "MQTT Connected with username: " + username + " and password: " + password );
                                timeout = MINTIMEOUT;
                            }
                            catch (MqttException e)
                            {
                                Log.d(TAG, "Connection attempt failed with reason code = " + e.getReasonCode() + e.getCause());
                                if (timeout < MAXTIMEOUT)
                                {
                                    timeout *= 2;
                                }
                                this.sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
                                return;
                            }

					    /*
					     * Re-subscribe to previously subscribed topics
					     */

                            Iterator<String> i = topics.iterator();
                            while (i.hasNext())
                            {
                                subscribe(i.next());
                            }
                        }
                        break;
                    }
                    case RESETTIMER:
                    {
                        timeout = MINTIMEOUT;
                        break;
                    }
                    case SUBSCRIBE:
                    {
                        boolean status = false;
                        Bundle b = msg.getData();
                        if (b != null)
                        {
                            CharSequence cs = b.getCharSequence(TOPIC);
                            if (cs != null)
                            {
                                String topic = cs.toString().trim();
                                if (topic.isEmpty() == false)
                                {
                                    status = subscribe(topic);
	        					/*
	        					 * Save this topic for re-subscription if needed.
	        					 */
                                    if (status)
                                    {
                                        topics.add(topic);
                                    }
                                }
                            }
                        }
                        ReplytoClient(msg.replyTo, msg.what, status);
                        break;
                    }
                    case PUBLISH:
                    {

                        boolean status = false;
                        Bundle b = msg.getData();
                        if (b != null)
                        {
                            CharSequence cs = b.getCharSequence(TOPIC);
                            if (cs != null)
                            {
                                String topic = cs.toString().trim();
                                if (topic.isEmpty() == false)
                                {
                                    cs = b.getCharSequence(MESSAGE);
                                    if (cs != null)
                                    {
                                        String message = cs.toString().trim();
                                        if (message.isEmpty() == false)
                                        {
                                            status = publish(topic, message);
                                        }
                                    }
                                }
                            }
                        }
                        ReplytoClient(msg.replyTo, msg.what, status);
                        break;
                    }
                    case MIO_PUBLISH_TUTTI:
                        syncWithSensormind();
                        //syncTEST_2();
                }
            }

            private boolean subscribe(String topic)
            {
                try
                {
                    client.subscribe(topic);
                }
                catch (MqttException e)
                {
                    Log.d(getClass().getCanonicalName(), "Subscribe failed with reason code = " + e.getReasonCode());
                    return false;
                }
                return true;
            }

            private boolean publish(String topic, String msg)
            {
                try
                {
                    MqttMessage message = new MqttMessage();

                    message.setPayload(msg.getBytes());
                    client.publish(topic, message);
                    Log.d(TAG,"PUBBLICATO: " + topic + " : " + message);
                }
                catch (MqttException e)
                {
                    Log.d(getClass().getCanonicalName(), "Publish failed with reason code = " + e.getReasonCode());
                    return false;
                }
                return true;
            }

            @Override
            public void connectionLost(Throwable arg0)
            {
                Log.d(getClass().getCanonicalName(), "connectionLost");
                connState = CONNECT_STATE.DISCONNECTED;
                sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken arg0)
            {
                //Log.d(TAG,"Delivery complete");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception
            {
                Log.d(getClass().getCanonicalName(), topic + ":" + message.toString());

                if (intentName != null)
                {
                    Intent intent = new Intent();
                    intent.setAction(intentName);
                    intent.putExtra(TOPIC, topic);
                    intent.putExtra(MESSAGE, message.toString());
                    sendBroadcast(intent);
                }


                Context context = getBaseContext();
                PendingIntent pendingIntent = null;

                if (launchActivity != null)
                {
                    Intent intent = new Intent(context, launchActivity);
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);

                    //build the pending intent that will start the appropriate activity
                    pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                }

                //build the notification
                Builder notificationCompat = new Builder(context);
                notificationCompat.setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setContentText( message.toString())
                        .setSmallIcon(R.drawable.ic_launcher);

                Notification notification = notificationCompat.build();
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(mid++, notification);

            }

        }

    }
}
