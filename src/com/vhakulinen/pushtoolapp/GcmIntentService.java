package com.vhakulinen.pushtoolapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
    public static final String RESPONSE_MESSAGE = "PingGcmInsetServiceMessage";

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    private static final String TAG = "GcmIntentService";

    public GcmIntentService() {
        super("GcmIntentService");
        Log.i(TAG, "Started");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.i(TAG, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.i(TAG, "Deleted messages on server: " + extras.toString());

            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                Log.i(TAG, "Received: " + extras.toString());
                Log.i(TAG, "Message: " + extras.getString("message"));

                if (extras.getString("message").equals("ping")) {
                    getDataFromBackend();
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void getDataFromBackend() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
        String token = prefs.getString(MainActivity.PROPERTY_TOKEN_ID, "");

        if (token.isEmpty()) {
            Log.i(TAG, "Token is empty, wont receive any data");
        }

        String urlParameters = String.format("token=%s", token);
        String responseMessage;

        try {
            URL url = new URL(MainActivity.BACKEND_POOL_ADDRESS);
            URLConnection conn = url.openConnection();
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            httpsConn.setRequestMethod("POST");
            httpsConn.setDoOutput(true); 
            httpsConn.setDoInput(true);
     
            // Send post request
            httpsConn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
     
            // TODO: If responseCode is not 200, exit the loop and ask
            // user to re-retrieve the token
            int responseCode = httpsConn.getResponseCode();
            Log.v("PUSH","\nSending 'POST' request to URL : " + url);
            Log.v("PUSH", "Post parameters : " + urlParameters);
            Log.v("PUSH", "Response Code : " + responseCode);
     
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(httpsConn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
     
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
     
            //print result
            Log.v("PUSH", response.toString());
            responseMessage = response.toString();

        } catch (MalformedURLException e) {
            Log.v("MalformedURLException", e.toString());
            return;
        } catch (IOException e) {
            Log.v("IOException", e.toString());
            return;
        }

        if (!responseMessage.equals("")) {
            DataHelper.Save(this, responseMessage);
            if (MainActivity.ON_BACKGROUD) {
                String[][] data;
                try {
                    data = DataHelper.fromJSONArray(DataHelper.fromString(responseMessage));
                } catch (Exception e) {
                    return;
                }
                for (String[] d : data) {
                    sendNotification(d);
                }
            } else {
                Log.i(TAG, "broadcasting");
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.GcmIntentServiceReceiver.PROCESS_RESPONSE);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra(RESPONSE_MESSAGE, responseMessage);
                sendBroadcast(broadcastIntent);
            }
        }
    }

    private void sendNotification(String[] data) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder;

        mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        // .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentTitle(data[0])
        .setContentText(data[1]);

        if (Boolean.valueOf(data[3])) {
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
        } else {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
