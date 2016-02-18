package com.bravson.socialalert.android.service;

import android.database.sqlite.*;
import android.provider.BaseColumns;

public final class UploadEntry implements BaseColumns  {

	public static final String TABLE_NAME = "entry";
    public static final String COLUMN_NAME_FILE = "file";
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_LONGITUDE = "longitude";
    public static final String COLUMN_NAME_LATITUDE = "latitude";
    public static final String COLUMN_NAME_URI = "uri";
    public static final String COLUMN_NAME_CATEGORY = "category";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_TAGS = "tags";
    

}
