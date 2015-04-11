package com.vhakulinen.pushtoolapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PushDataSource {

    private SQLiteDatabase database;
    private PushDatabaseHelper dbHelper;
    private String[]allColumns = { PushDatabaseHelper.COLUMN_ID,
        PushDatabaseHelper.COLUMN_TITLE, PushDatabaseHelper.COLUMN_BODY,
        PushDatabaseHelper.COLUMN_URL, PushDatabaseHelper.COLUMN_TIMESTAMP};

    public PushDataSource(Context context) {
        dbHelper = new PushDatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void savePushData(PushData d) {
        ContentValues values = new ContentValues();
        values.put(PushDatabaseHelper.COLUMN_TITLE, d.getTitle());
        values.put(PushDatabaseHelper.COLUMN_BODY, d.getBody());
        values.put(PushDatabaseHelper.COLUMN_URL, d.getUrl());
        values.put(PushDatabaseHelper.COLUMN_TIMESTAMP, d.getTimestamp());
        database.insert(PushDatabaseHelper.TABLE_DATA, null, values);
    }

    public List<PushData> getAllData() {
        List<PushData> data = new ArrayList<PushData>();

        Cursor cursor = database.query(PushDatabaseHelper.TABLE_DATA,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data.add(cursorToPushData(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return data;
    }

    public List<PushData> getNextXFrom(int startIndex, int count) {
        // Inverst everything so that gets the newest
        List<PushData> data = new ArrayList<PushData>();

        Cursor cursor = database.query(PushDatabaseHelper.TABLE_DATA,
                allColumns, null, null, null, null, null);

        cursor.moveToLast();
        // +1 because cursor.getPosition is zero based
        int invertedIndex = cursor.getPosition() - startIndex + 1;

        while (!cursor.isBeforeFirst() && count > 0) {
            if (cursor.getPosition() < invertedIndex) {
                data.add(cursorToPushData(cursor));
                count--;
            }
            cursor.moveToPrevious();
        }
        cursor.close();

        Collections.reverse(data);
        return data;
    }

    private PushData cursorToPushData(Cursor c) {
        PushData d = new PushData(
                c.getString(c.getColumnIndex(PushDatabaseHelper.COLUMN_TITLE)),
                c.getString(c.getColumnIndex(PushDatabaseHelper.COLUMN_BODY)),
                c.getString(c.getColumnIndex(PushDatabaseHelper.COLUMN_URL)),
                c.getLong(c.getColumnIndex(PushDatabaseHelper.COLUMN_TIMESTAMP)));
        return d;
    }
}
