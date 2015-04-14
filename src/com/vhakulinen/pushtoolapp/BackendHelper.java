package com.vhakulinen.pushtoolapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class BackendHelper {

    public static final String DEFAULT_BACKEND_ADDRESS = "https://lotsof.coffee/p/";

    public static final String BACKEND_ADDRESS = DEFAULT_BACKEND_ADDRESS;

    public static final String BACKEND_POOL_ADDRESS = BACKEND_ADDRESS + "pool/";
    public static final String BACKED_RETRIEVE_ADDRESS = BACKEND_ADDRESS + "retrieve/";
    public static final String BACKED_GCM_REGISTER = BACKEND_ADDRESS + "gcm/";
    public static String TAG = "BackendHelper";

    private static Response doPostRequest(String uri, String params) throws Exception {
        String responseMessage;
        int responseCode;

        URL url = new URL(uri);
        URLConnection conn = url.openConnection();
        HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
        httpsConn.setRequestMethod("POST");
        httpsConn.setDoOutput(true); 
        httpsConn.setDoInput(true);
 
        // Send post request
        httpsConn.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();
 
        responseCode = httpsConn.getResponseCode();
 
        BufferedReader in = new BufferedReader(
                new InputStreamReader(httpsConn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
 
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
 
        responseMessage = response.toString();

        return new Response(responseCode, responseMessage);
    }

    public static Response receiveDataFromBackend(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
        String token = prefs.getString(MainActivity.PROPERTY_TOKEN_ID, "");

        if (token.isEmpty()) {
            Log.i(TAG, "Token is empty, wont receive any data");
            throw new Exception("Token is empty!");
        }

        String urlParameters = String.format("token=%s", token);
        return doPostRequest(BACKEND_POOL_ADDRESS, urlParameters);
    }

    public static Response getTokenFromBackend(String email, String password) throws Exception {
        String urlParams = String.format("email=%s&password=%s", email, password);
        return doPostRequest(BackendHelper.BACKED_RETRIEVE_ADDRESS, urlParams);
    }

    public static Response registerGCMToBackend(String token, String regid) throws Exception {
        String urlParams = String.format("gcmid=%s&token=%s", regid, token);
        return doPostRequest(BackendHelper.BACKED_GCM_REGISTER, urlParams);
    }
}
