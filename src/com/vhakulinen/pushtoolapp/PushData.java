package com.vhakulinen.pushtoolapp;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PushData {

    private int id;
    private String title;
    private String body;
    private String url;
    private long timestamp;

    private Format df = new SimpleDateFormat("MM/dd HH:mm");

    public void constructor(String title, String body, String url, long timestamp, int id) {
        this.title = title;
        this.body = body;
        this.url = url;
        // this.timestamp = timestamp;
        Date time = new Date();
        if (timestamp != 0) {
            time.setTime(timestamp);
        }
        this.timestamp = time.getTime();
        this.id = id;
    }

    public PushData(String title, String body, String url, long timestamp) {
        this.constructor(title, body, url, timestamp, -1);
    }

    public PushData(String title, String body, String url, long timestamp, int id) {
        this.constructor(title, body, url, timestamp, id);
    }

    public String getTitle() { return this.title; }
    public String getBody() { return this.body; }
    public String getUrl() { return this.url; }
    public long getTimestamp() { return this.timestamp; }

    public String getTime() {
        String dateString = df.format(this.timestamp);
        return dateString;
    }

    public int getId() {
        return this.id;
    }
}
