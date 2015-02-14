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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.ukuke.gl.sensormind.DataDbHelper;
import com.ukuke.gl.sensormind.R;
import com.ukuke.gl.sensormind.support.DataSample;
import com.ukuke.gl.sensormind.support.DeviceInfo;


public class MQTTService extends Service {
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
    private DeviceInfo deviceInfo = null;

    private synchronized static boolean isRunning() {
		 /*
		  * Only run one instance of the service.
		  */
        if (serviceRunning == false) {
            serviceRunning = true;
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connection = new MQTTConnection();

        // Get shared preferences
        prefs = getSharedPreferences("com.ukuke.gl.sensormind", MODE_PRIVATE);
        ipMqtt = prefs.getString("ip_MQTT", "137.204.213.190");
        portMqtt = prefs.getInt("port_MQTT", 1884);
        username = prefs.getString("username", "NULL");
        password = prefs.getString("password", "NULL");
        deviceInfo = new DeviceInfo(MQTTService.this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (prefs.getBoolean("enableGrabbing", false)) {
            if (!isRunning()) {
                connection.start();
            }
        }
//        else {
//            Message msg = Message.obtain(null, MQTTConnection.STOP);
//            connection.makeRequest(msg);
//        }
        boolean isLoggedIn = prefs.getBoolean("loggedIn", false);

        if (isLoggedIn) {
            username = prefs.getString("username", "NULL");
            password = prefs.getString("password", "NULL");
            new uploadToSensormind_asynk().execute(); }
        else {
            Log.d(TAG, "No credentials stored in shared preferences");
        }
        return START_STICKY;
    }

    public boolean isAllowedToSync() {
        boolean ret = true;

        if (prefs.getBoolean("syncOnlyOnWifi", false))  {
            if (!deviceInfo.isConnectedToWifi()) {
                Log.d(TAG, "Mqtt not allowed to sync, connect to wifi or change preferences in settings");
                return false;
            }
        }

        if (prefs.getBoolean("syncOnlyIfPluggedIn", false))  {
            if (!deviceInfo.isPluggedIn()) {
                Log.d(TAG, "Mqtt not allowed to sync, plug in your device or change preferences in settings");
                return false;
            }
        }

        return ret;
    }

    @Override
    public void onDestroy() {
        connection.end();
    }

    @Override
    public IBinder onBind(Intent intent) {
		/*
		 * Return a reference to our client handler.
		 */
        return clientMessenger.getBinder();
    }

    private void ReplytoClient(Messenger responseMessenger, int type, boolean status) {
		 /*
		  * A response can be sent back to a requester when
		  * the replyTo field is set in a Message, passed to this
		  * method as the first parameter.
		  */
        if (responseMessenger != null) {
            Bundle data = new Bundle();
            data.putBoolean(STATUS, status);
            Message reply = Message.obtain(null, type);
            reply.setData(data);

            try {
                responseMessenger.send(reply);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    enum CONNECT_STATE {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    /*
     * This class handles messages sent to the service by
     * bound clients.
     */
    class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            boolean status = false;

            switch (msg.what) {
                //case SUBSCRIBE: // Not used now
                case PUBLISH:
           		 	/*
           		 	 * These two requests should be handled by
           		 	 * the connection thread, call makeRequest
           		 	 */
                    connection.makeRequest(msg);
                    break;
                case REGISTER: // Not used now
                {
                    Bundle b = msg.getData();
                    if (b != null)
                    {
                        Object target = b.getSerializable(CLASSNAME);
                        if (target != null)
                        {
        				 /*
        				  * This request can be handled in-line
        				  * call the API
        				  */
                            connection.setPushCallback((Class<?>) target);
                            status = true;
                        }
                        CharSequence cs = b.getCharSequence(INTENTNAME);
                        if (cs != null)
                        {
                            String name = cs.toString().trim();
                            if (name.isEmpty() == false)
                            {
            				 /*
            				  * This request can be handled in-line
            				  * call the API
            				  */
                                connection.setIntentName(name);
                                status = true;
                            }
                        }
                    }
                    ReplytoClient(msg.replyTo, msg.what, status);
                    break;
                }
            }
        }
    }

    private class MQTTConnection extends Thread {
        public static final int MIO_PUBLISH_TUTTI = 10;
        private Class<?> launchActivity = null;
        private String intentName = null;
        private MsgHandler msgHandler = null;
        private CONNECT_STATE connState = CONNECT_STATE.DISCONNECTED;        private static final int STOP = PUBLISH + 1;
        MQTTConnection() {
            msgHandler = new MsgHandler();
            msgHandler.sendMessage(Message.obtain(null, CONNECT));
        }

        public void end() {
            msgHandler.sendMessage(Message.obtain(null, STOP));
        }        private static final int CONNECT = PUBLISH + 2;

        public void makeRequest(Message msg) {
			/*
			 * It is expected that the caller only invokes
			 * this method with valid msg.what.
			 */
            msgHandler.sendMessage(Message.obtain(msg));
        }

        public void setPushCallback(Class<?> activityClass) {
            launchActivity = activityClass;
        }        private static final int RESETTIMER = PUBLISH + 3;

        public void setIntentName(String name) {
            intentName = name;
        }

        private class MsgHandler extends Handler implements MqttCallback {
            private final String HOST = "137.204.213.190";//ipMqtt;
            private final int PORT = 1884;//portMqtt;
            private final String uri = "tcp://" + HOST + ":" + PORT;
            private final int MINTIMEOUT = 2000;
            private final int MAXTIMEOUT = 32000;
            private int timeout = MINTIMEOUT;
            private MqttClient client = null;
            private MqttConnectOptions options = new MqttConnectOptions();
            private Vector<String> topics = new Vector<String>();


            MsgHandler() {
                options.setCleanSession(true);
                try {

                    //client = new MqttClient(uri, username, null);
                    client = new MqttClient(uri, MqttClient.generateClientId(),null);
                    //client = new MqttClient(uri, "1i", null);
                    client.setCallback(this);

                } catch (MqttException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case STOP: {
					/*
					 * Clean up, and terminate.
					 */
                        client.setCallback(null);
                        if (client.isConnected()) {
                            try {
                                client.disconnect();
                                client.close();
                                Log.i(TAG, "MQTT Disconnected");

                                //Toast.makeText(getApplicationContext(), "MQTT Disconnected", Toast.LENGTH_LONG).show();
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                        //getLooper().quit(); //DISATTIVATO SENNO CRASJHA!!!
                        break;
                    }
                    case CONNECT: {
                        if (connState != CONNECT_STATE.CONNECTED) {
                            try {
                                options.setUserName(username);
                                options.setPassword(password.toCharArray());

                                client.connect(options);
                                connState = CONNECT_STATE.CONNECTED;
                                //Toast.makeText(getApplicationContext(), "MQTT Connected", Toast.LENGTH_LONG).show();
                                Log.i(TAG, "MQTT Connected with username: " + username + " and password: " + password);
                                timeout = MINTIMEOUT;
                            } catch (MqttException e) {
                                Log.d(TAG, "Connection for " + username + " attempt failed with reason code = " + e.getReasonCode() + e.getCause());
                                if (timeout < MAXTIMEOUT) {
                                    timeout *= 2;
                                }
                                this.sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
                                return;
                            }

					    /*
					     * Re-subscribe to previously subscribed topics
					     */

                            Iterator<String> i = topics.iterator();
                            while (i.hasNext()) {
                                subscribe(i.next());
                            }
                        }
                        break;
                    }
                    case RESETTIMER: {
                        timeout = MINTIMEOUT;
                        break;
                    }
                    case SUBSCRIBE: {
                        boolean status = false;
                        Bundle b = msg.getData();
                        if (b != null) {
                            CharSequence cs = b.getCharSequence(TOPIC);
                            if (cs != null) {
                                String topic = cs.toString().trim();
                                if (topic.isEmpty() == false) {
                                    status = subscribe(topic);
	        					/*
	        					 * Save this topic for re-subscription if needed.
	        					 */
                                    if (status) {
                                        topics.add(topic);
                                    }
                                }
                            }
                        }
                        ReplytoClient(msg.replyTo, msg.what, status);
                        break;
                    }
                    case PUBLISH: {

                        boolean status = false;
                        Bundle b = msg.getData();
                        if (b != null) {
                            CharSequence cs = b.getCharSequence(TOPIC);
                            if (cs != null) {
                                String topic = cs.toString().trim();
                                if (topic.isEmpty() == false) {
                                    cs = b.getCharSequence(MESSAGE);
                                    if (cs != null) {
                                        String message = cs.toString().trim();
                                        if (message.isEmpty() == false) {
                                            status = publish(topic, message);
                                        }
                                    }
                                }
                            }
                        }
                        ReplytoClient(msg.replyTo, msg.what, status);
                        break;
                    }
                }
            }

            private boolean subscribe(String topic) {
                try {
                    client.subscribe(topic);
                } catch (MqttException e) {
                    Log.d(getClass().getCanonicalName(), "Subscribe failed with reason code = " + e.getReasonCode());
                    return false;
                }
                return true;
            }

            private boolean publish(String topic, String msg) {
                try {
                    MqttMessage message = new MqttMessage();

                    message.setPayload(msg.getBytes());
                    client.publish(topic, message);
                    Log.v(TAG, "Published topic: " + topic);// + " : " + message);
                } catch (MqttException e) {
                    Log.e(TAG, "Publish failed with reason code = " + e.getReasonCode());
                    return false;
                }
                return true;
            }

            @Override
            public void connectionLost(Throwable arg0) {
                Log.d(TAG, "Mqtt connection lost");
                connState = CONNECT_STATE.DISCONNECTED;
                sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken arg0) {
                //Log.d(TAG,"Delivery complete");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, topic + ":" + message.toString());

                if (intentName != null) {
                    Intent intent = new Intent();
                    intent.setAction(intentName);
                    intent.putExtra(TOPIC, topic);
                    intent.putExtra(MESSAGE, message.toString());
                    sendBroadcast(intent);
                }


                Context context = getBaseContext();
                PendingIntent pendingIntent = null;

                if (launchActivity != null) {
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
                        .setContentText(message.toString())
                        .setSmallIcon(R.drawable.ic_launcher);

                Notification notification = notificationCompat.build();
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(mid++, notification);

            }


        }
        public void run() {
            //    Log.d(TAG, "SONO NEL RUN!");
        }
    }

    private class uploadToSensormind_asynk extends AsyncTask<String, Void, String> {

        private boolean res;

        @Override
        protected String doInBackground(String... params) {

            syncWithSensormind();

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //Log.i(TAG, "Sensormind Sync completed");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        //@Deprecated
        private boolean publishMessage(String path, String message) {
            boolean res = false;
            try {
                MqttClient myC = connection.msgHandler.client;
                MqttMessage mess = new MqttMessage();
                mess.setPayload(message.getBytes());
                myC.publish(path, mess);
                res = true;
                Log.d(TAG, "Published topic: " + path);// + " : " + message);
                //Log.d(TAG, "PUBBLICATO: " + path + " : " + message);
            } catch (Exception e) {
                Log.d(TAG, "Errore in publishMessage: " + e);
            }
            return res;
        }

        private boolean publishMessage_DISACTIVATED(String path, String message) {
            boolean res = false;
            try {

                //path = "CIAO";
                Bundle data = new Bundle();
                data.putCharSequence(TOPIC, path);
                data.putCharSequence(MESSAGE, message);
                Message msg = Message.obtain(null, PUBLISH);
                msg.setData(data);
                connection.makeRequest(msg);
                res = true;
            } catch (Exception e) {
                Log.d(TAG, "Errore in publishMessage: " + e);
            }
            return res;
        }

        public void syncWithSensormind() {
            DataDbHelper dataDbHelper;
            dataDbHelper = new DataDbHelper(getApplicationContext());
            // TODO Questo isAllowedToSync dovrebbe essere gestito in modo più furbo anche connettendosi o disconnettendosi dal server
            if ((connection.connState == CONNECT_STATE.CONNECTED) && isAllowedToSync()) {
                int numPublishedMessages = 0;
                int numArrayPublished = 0;

                List<DataSample> listData;

                // Multi sample send
                try {
                    while (dataDbHelper.numberOfUnsentArrays() > 1) {
                        DataSample sample;

                        //Log.d(TAG, "Size List Data: " + listData.size());

                        listData = dataDbHelper.getFirstUnsentArrayDataSamples();
                        if (listData.size() <= 0) {
                            break;
                        } // TODO: Se non metto questo mi va in exception perchè ognitanto listData è vuota. perchè?

                        numArrayPublished++;
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
                        if (connection.connState != CONNECT_STATE.CONNECTED) {
                            break;
                        }
                        boolean sent_1 = publishMessage(path + "/1", obj_1.toString());
                        numPublishedMessages++;
                        boolean sent_2 = publishMessage(path + "/2", obj_2.toString());
                        numPublishedMessages++;
                        boolean sent_3 = publishMessage(path + "/3", obj_3.toString());
                        numPublishedMessages++;

                        if (sent_1 && sent_2 && sent_3) {
                            dataDbHelper.setSentListOfDataSamples(listData);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in array publish : " + e);
                }

                if (numArrayPublished > 0) {
                    Log.i(TAG, "Sent to Mqtt " + numArrayPublished + " arrays");
                }

                // Single sample send
                try {
                    if (dataDbHelper.numberOfUnsentEntries() > 0) {
                        List<DataSample> listDataSent = new ArrayList<>();
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

                            String path = "/" + username + "/v1/bm/" + sample.getFeedPath();
                            data.putCharSequence(TOPIC, path);
                            data.putCharSequence(MESSAGE, message);
                            Message msg = Message.obtain(null, PUBLISH);
                            msg.setData(data);
                            if (connection.connState != CONNECT_STATE.CONNECTED) {
                                break;
                            }
                            boolean sent = publishMessage(path, message);
                            numPublishedMessages++;
                            if (sent) { // Se riesce ad inviarlo aggiungilo alla lista di sent
                                listDataSent.add(sample);
                            }
                        }
                        if (listDataSent.size() > 0) {
                            Log.i(TAG, "Sent to Mqtt " + listDataSent.size() + " single samples");
                            dataDbHelper.setSentListOfDataSamples(listDataSent);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error in single sample publish: " + e);
                }
                Log.i(TAG, "Sensormind sync completed: " + numPublishedMessages + " messages published");
            } else {
                Log.i(TAG, "Nothing published, i'm not connected to MQTT");
            }
        }
    }
}
