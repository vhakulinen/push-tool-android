package com.vhakulinen.pushtoolapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PushDatabaseHelper extends SQLiteOpenHelper {

  public static final String TABLE_DATA = "pushdata";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_TITLE = "data";
  public static final String COLUMN_BODY = "body";
  public static final String COLUMN_URL = "url";
  public static final String COLUMN_TIMESTAMP = "timestamp";

  private static final String DATABASE_NAME = "pushdata.db";
  private static final int DATABASE_VERSION = 1;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table "
      + TABLE_DATA + "(" + COLUMN_ID
      + " integer primary key autoincrement, "
      + COLUMN_TITLE + " text not null,"
      + COLUMN_BODY + " text,"
      + COLUMN_URL + " text,"
      + COLUMN_TIMESTAMP + " bigint" + ");";

  public PushDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
    onCreate(db);
  }

} 
