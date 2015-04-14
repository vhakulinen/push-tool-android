package com.vhakulinen.pushtoolapp;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class DataHelper {
    public final static String TAG = "DataHelper";
    public final static String STORAGE_FILE_NAME = "PushToolStorage";

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

    public static PushData fromJSONObject(JSONObject json) throws Exception {
        String title = json.getString("Title");
        String body = json.getString("Body");
        String url = json.getString("Url");
        long timestamp;

        try {
            timestamp = json.getLong("UnixTimeStamp");
        } catch (Exception e) {
            timestamp = 0;
        }

        return new PushData(title, body, url, timestamp);
    }
}
