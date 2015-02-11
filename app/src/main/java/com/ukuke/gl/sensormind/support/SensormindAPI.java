package com.ukuke.gl.sensormind.support;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

public class SensormindAPI {


    private static final String CREATE_FEED_SERVICE = "/service/v1/createfeed";
    private static final String CHECK_CREDENTIALS_SERVICE = "/service/v1/checkcredentials";
    private static final String REGISTER_NEW_ACCOUNT_SERVICE = "/service/v1/register";
    private static final String LIST_FEED_SERVICE = "/service/v1/listfeed";

    private static final String url = "http://137.204.213.190";
    private static final int port = 8888;
    private String user;
    private String password;
    private static final String TAG = SensormindAPI.class.getSimpleName();



    public SensormindAPI(String user, String password)
    {
        this.user = user;
        this.password = password;
    }

    public List<FeedJSON> getAllFeed()
    {
        List<FeedJSON> list = new ArrayList<>();
        boolean ret = false;
        String content = "username="+user+"&password="+password;
        HTMLResponse res = null;
        try
        {
            res = makeHTTPRequest("GET", LIST_FEED_SERVICE, content);
            JsonObject jsonObject = null;
            JSONArray array = null;

            JSONArray jsonMainArr = new JSONArray(res.getContent());

            int arrayLenght = jsonMainArr.length();

            for (int i = 0; i < jsonMainArr.length(); i++) {  // **line 2**
                FeedJSON feed = new FeedJSON();
                JSONObject childJSONObject = jsonMainArr.getJSONObject(i);

                if (childJSONObject.has("s_uid")) {
                    String s_uid = childJSONObject.getString("s_uid");
                    feed.setS_uid(s_uid); }
                if (childJSONObject.has("label")) {
                    String label = childJSONObject.getString("label");
                    feed.setLabel(label); }
                if (childJSONObject.has("description")) {
                    String description = childJSONObject.getString("description");
                    feed.setDescription(description); }
                if (childJSONObject.has("is_static_located")) {
                    boolean is_static_located = childJSONObject.getBoolean("is_static_located");
                    feed.setIs_static_located(is_static_located); }
                if (childJSONObject.has("measure_unit")) {
                    String measure_unit = childJSONObject.getString("measure_unit");
                    feed.setMeasure_unit(measure_unit); }
                if (childJSONObject.has("type_id")) {
                    int type_id = childJSONObject.getInt("type_id");
                    feed.setType_id(type_id); }
                if (childJSONObject.has("static_altitude")) {
                    Double static_altitude = childJSONObject.getDouble("static_altitude");
                    feed.setStatic_altitude(static_altitude); }
                if (childJSONObject.has("static_latitude")) {
                    Double static_latitude = childJSONObject.getDouble("static_latitude");
                    feed.setStatic_latitude(static_latitude); }
                if (childJSONObject.has("static_longitude")) {
                    Double static_longitude = childJSONObject.getDouble("static_longitude");
                    feed.setStatic_longitude(static_longitude); }

                list.add(feed);

            }

        } catch (Exception e) {
            Log.d(TAG,"ERR!: " + e);
        }
        //TODO: Gestire il success false
        return list;
    }


    public boolean registerNewAccount(String firstname, String lastname, String timezone, String email)
    {
        boolean ret = false;
        String content = "request={username:" + user + ",password:" + password + ",firstname:" + firstname + ",lastname:" + lastname + ",email:" + email + ",timezone:" + timezone + "}";
        HTMLResponse res = null;
        try
        {
            res = makeHTTPRequest("GET", REGISTER_NEW_ACCOUNT_SERVICE, content);
            JsonObject jsonObject = null;
            jsonObject = new Gson().fromJson(res.getContent(),JsonObject.class);
            String s = jsonObject.get("success").getAsString();
            if (s.equals("true"))
                ret = true;
        } catch (Exception e) { Log.d("API Sensormind: ", "ERR: " + e); }

        return ret;
    }

    public boolean checkCredentials(String user, String password)
    {
        boolean ret = false;
        String content = "username="+user+"&password="+password;
        HTMLResponse res = makeHTTPRequest("GET", CHECK_CREDENTIALS_SERVICE, content);
        try
        {
            JsonObject jsonObject = null;
            jsonObject = new Gson().fromJson(res.getContent(),JsonObject.class);
            String s = jsonObject.get("success").getAsString();
            if (s.equals("true")) {
                ret = true;
            }
        } catch (Exception e) {}
        return ret;
    }

    public boolean createFeed(String label, boolean is_static_located, String measure_unit, String s_uid, int type_id)
    {
        //TODO fare l'encoding dei parametri altrimenti la % dell'umidit� non � %25 � scoppia tutto
        boolean ret = false;
        int loc = is_static_located?1:0;
        String content = "username="+user+"&password="+password+"&request={label:\""+label+"\",s_uid:\""+s_uid+"\",is_static_located:"+loc+",measure_unit:\""+measure_unit+"\",type_id:"+type_id+"}";
        HTMLResponse res = makeHTTPRequest("GET", CREATE_FEED_SERVICE, content);
        if (res.getHTMLStatusCode() == 200)
        {
            try
            {
                JsonObject jsonObject = null;
                jsonObject = new Gson().fromJson(res.getContent(),JsonObject.class);
                String s = jsonObject.get("success").getAsString();
                if (s.equals("true")) {
                    ret = true;

                }
            } catch (Exception e) {}
        }
        return ret;
    }

    private  static HTMLResponse makeHTTPRequest(String method, String service, String content)
    {
        HTMLResponse res = new HTMLResponse();
        URL urlRequest;
        try {

            if (method.equals("POST"))
            {
                urlRequest = new URL(url + ":"+port+ service);
            }
            else
                urlRequest = new URL(url + ":"+port+ service + "?" + content);


            Log.d(TAG, urlRequest.toString());

            HttpURLConnection urlConnection = (HttpURLConnection) urlRequest.openConnection();

            urlConnection.setRequestMethod(method);
            if (method.equals("POST"))
            {
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setUseCaches(false);
            }
            //urlConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            //urlConnection.setRequestProperty("Connection","close");
            //urlConnection.setRequestProperty("Cache-Control","max-age=0");

            InputStream ins = null;
            if (method.equals("POST"))
            {
                OutputStream outputStream = urlConnection.getOutputStream();
                outputStream.write(content.getBytes("UTF-8"));
                outputStream.close();
            }

            res.setHTMLStatusCode(urlConnection.getResponseCode());

            if (urlConnection.getResponseCode() >= 200 && urlConnection.getResponseCode() <= 250)
                ins = urlConnection.getInputStream();
            else
                ins = urlConnection.getErrorStream();

            HashMap<String, String> HTMLHeader = new HashMap<String, String>();
            for (int i = 0;; i++)
            {
                String headerName = urlConnection.getHeaderFieldKey(i);
                String headerValue = urlConnection.getHeaderField(i);
                HTMLHeader.put(headerName, headerValue);
                if (headerName == null && headerValue == null)
                    break;
            }
            res.setHTMLHeader(HTMLHeader);

            if (ins != null)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(ins));
                String inputLine,resString="";
                while ((inputLine = in.readLine()) != null) resString += inputLine;
                in.close();
                res.setContent(resString);
            }

        } catch (Exception e) {
            Log.d(TAG, "Error in Make HttpRequest");
            e.printStackTrace();
        }
        return res;
    }
}
