package com.vhakulinen.pushtoolapp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class DataHelper {
    public final static String TAG = "DataHelper";
    public final static String STORAGE_FILE_NAME = "PushToolStorage";

    private static Format dateFormat = new SimpleDateFormat("MM/dd HH:mm");

    public static JSONArray fromString(String data) throws Exception {
        JSONArray jsonArray;
        try {
            data = data.replace("}{", "},{");
            jsonArray = new JSONArray("[ " + data + " ]");
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            throw new Exception("Failed to parse data to json");
        }
        return jsonArray;
    }

    public static String[][] fromJSONArray(JSONArray jsonArray) throws Exception {
        String[][] out = new String[jsonArray.length()][2];

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json;
            String title;
            String body;
            Date time = new Date();

            try {
                json = jsonArray.getJSONObject(i);
                title = json.getString("Title");
                body = json.getString("Body");

                try {
                    Long timestamp = json.getLong("UnixTimeStamp");
                    if (timestamp != 0) {
                        time.setTime(timestamp);
                    }
                } catch (Exception e) {
                }
            } catch (Exception e) {
                Log.v(TAG, e.toString());
                throw e;
            }

            String dateString = dateFormat.format(time);
            out[i] = new String[]{title, body, dateString};
        }

        return out;
    }

    public static void Save(Context context, String data) {
        // Update the timestamp to current time if it is 0
        JSONArray jsonData;
        String newData = "";
        try {
            jsonData = fromString(data);
            for (int i = 0; i < jsonData.length(); i++) {
                JSONObject json;
                Date time = new Date();

                try {
                    json = jsonData.getJSONObject(i);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    continue;
                }

                try {
                    Long timestamp = json.getLong("UnixTimeStamp");
                    if (timestamp == 0) {
                        json.put("UnixTimeStamp", time.getTime());
                        jsonData.put(i, json);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    continue;
                }
                newData += json.toString();
            }
        } catch (Exception e) {
            return;
        }

        Log.d(TAG, "Saving data");
        try {
            FileOutputStream out = context.openFileOutput(STORAGE_FILE_NAME,
                    Context.MODE_APPEND);
            out.write(newData.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "FileNotFoundException: " + e.toString());
        } catch (IOException e) {
            Log.i(TAG, "IOException: " + e.toString());
        }
    }

    public static String ReadAll(Context context) {
        try {
            FileInputStream in = context.openFileInput(STORAGE_FILE_NAME);
            int av = in.available();
            byte[] buf = new byte[av];
            in.read(buf, 0, av);
            return new String(buf);
        } catch (FileNotFoundException e) {
            Log.i(TAG, "FileNotFoundException: " + e.toString());
        } catch (IOException e) {
            Log.i(TAG, "IOException: " + e.toString());
        }
        return "";
    }
}
