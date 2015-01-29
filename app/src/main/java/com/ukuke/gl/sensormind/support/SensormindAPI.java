package com.ukuke.gl.sensormind.support;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class SensormindAPI {


    private static final String CREATE_FEED_SERVICE = "/service/v1/createfeed";
    //aggiungere la registrazione
    private static final String REGISTER_NEW_ACCOUNT = "/service/v1/register";
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

    // esempio:
    // createFeed("sensore-temperature",true,"C","telefono42/sensori/temperatura",1);


    public boolean registerNewAccount(String firstname, String lastname, String timezone, String email)
    {
        boolean ret = false;

        String content = "request={username:" + user + ",password:" + password + ",firstname:" + firstname + ",lastname:" + lastname + ",email:" + email + ",timezone:" + timezone + "}";

        Log.d ("RISPOSTA SERVER: ", "Sto per richiedere msg");

        HTMLResponse res = null;
        try
        {
            res = makeHTTPRequest("GET", REGISTER_NEW_ACCOUNT, content);

            JsonObject jsonObject = null;
            jsonObject = new Gson().fromJson(res.getContent(),JsonObject.class);
            String s = jsonObject.get("success").getAsString();
            if (s.equals("true"))
                ret = true;
        } catch (Exception e) { Log.d("API Sensormind: ", "ERR: " + e); }


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
                Log.d("CREATE FEED","Creato?: " + ret);
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
