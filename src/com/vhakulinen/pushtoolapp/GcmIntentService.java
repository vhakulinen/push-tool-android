package com.vhakulinen.pushtoolapp;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
    public static final String RESPONSE_MESSAGE = "PingGcmInsetServiceMessage";
    public static final String RESPONSE_PING = "PingGcm";
;
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
        String responseMessage;
        try {
            responseMessage = BackendHelper.receiveDataFromBackend(
                    getApplicationContext()).getMessage();
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            return;
        }

        if (!responseMessage.equals("")) {
            PushDataSource db = new PushDataSource(getApplicationContext());
            JSONArray arr;
            try {
                arr = DataHelper.fromString(responseMessage);
            } catch (Exception e) {
                return;
            }
            db.open();
            for (int i = 0; i < arr.length(); i++) {
                PushData p;
                String sound;

                try {
                    JSONObject json = arr.getJSONObject(i);
                    p = DataHelper.fromJSONObject(json);
                    sound = json.getString("Sound");
                } catch (Exception e) {
                    Log.v(TAG, "FAIL: " + e.toString());
                    continue;
                }

                if (MainActivity.ON_BACKGROUD) {
                    sendNotification(p, Boolean.valueOf(sound));
                } else {
                    Log.i(TAG, "broadcasting");
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MainActivity.GcmIntentServiceReceiver.PROCESS_RESPONSE);
                    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    broadcastIntent.putExtra(RESPONSE_MESSAGE, GcmIntentService.RESPONSE_PING);
                    sendBroadcast(broadcastIntent);
                }

                db.savePushData(p);
            }
            db.close();
        }
    }

    private void sendNotification(PushData data, boolean sound) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder;

        mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setAutoCancel(true)
        .setContentTitle(data.getTitle())
        .setContentText(data.getBody());

        if (sound) {
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
        } else {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
