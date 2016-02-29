package com.bravson.socialalert.android.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UploadDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Upload.db";
    
    private static final String SQL_CREATE_ENTRIES =
    	    "CREATE TABLE " + UploadEntry.TABLE_NAME + " (" +
    	    		UploadEntry._ID + " INTEGER PRIMARY KEY NOT NULL," +
    	    		UploadEntry.COLUMN_NAME_TYPE + " INTEGER NOT NULL, " +
    	    		UploadEntry.COLUMN_NAME_TIMESTAMP + " NUMERIC NOT NULL, " +
    	    		UploadEntry.COLUMN_NAME_LONGITUDE + " REAL, " +
    	    		UploadEntry.COLUMN_NAME_LATITUDE + " REAL, " +
    	    		UploadEntry.COLUMN_NAME_URI + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_CATEGORY + " INTEGER, " +
    	    		UploadEntry.COLUMN_NAME_TITLE + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_DESCRIPTION + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_TAGS + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_COUNTRY + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_LOCALITY + " TEXT, " +
    	    		UploadEntry.COLUMN_NAME_ADDRESS + " TEXT " +
    	    " )";

  	private static final String SQL_DELETE_ENTRIES =
    	    "DROP TABLE IF EXISTS " + UploadEntry.TABLE_NAME;

    public UploadDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
