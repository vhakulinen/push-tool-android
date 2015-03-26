package com.vhakulinen.pushtoolapp;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PushData {

    private String title;
    private String body;
    private String url;
    private long timestamp;

    private Format df = new SimpleDateFormat("MM/dd HH:mm");

    public PushData(String title, String body, String url, long timestamp) {
        this.title = title;
        this.body = body;
        this.url = url;
        // this.timestamp = timestamp;
        Date time = new Date();
        if (timestamp != 0) {
            time.setTime(timestamp);
        }
        this.timestamp = time.getTime();
    }

    public String getTitle() { return this.title; }
    public String getBody() { return this.body; }
    public String getUrl() { return this.url; }
    public long getTimestamp() { return this.timestamp; }

    public String getTime() {
        String dateString = df.format(this.timestamp);
        return dateString;
    }
}
