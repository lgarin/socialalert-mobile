package com.bravson.socialalert.android.service;

import java.io.File;
import java.io.FileFilter;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import com.bravson.socialalert.common.domain.MediaType;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

@EBean
public class UploadQueueService {

	private UploadDbHelper uploadDbHelper;
	
	@RootContext
	Context context;
	
	@AfterInject
	void init() {
		uploadDbHelper = new UploadDbHelper(context);
	}
	
	private static String getFilename(long rowId) {
		return "upload" + rowId + ".tmp";
	}
	
	private Long findOldestTimestamp() {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = db.query(UploadEntry.TABLE_NAME, new String[] {UploadEntry.COLUMN_NAME_TIMESTAMP}, null, null, null, null, UploadEntry.COLUMN_NAME_TIMESTAMP);
			if (c.isAfterLast()) {
				return null;
			}
			return c.getLong(0);
		}
	}
	
	private void deleteOldFiles(final long minTimestamp) {
		
		File[] oldFiles = context.getFilesDir().listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.lastModified() < minTimestamp;
			}
		});
		
		for (File file : oldFiles) {
			file.delete();
		}
	}
	
	public void purgeOldFiles() {
		Long minTimestamp = findOldestTimestamp();
		if (minTimestamp != null) {
			deleteOldFiles(minTimestamp);
		}
	}
	
	public void queueFile(File file, MediaType type, Location location) {
		long timestamp = System.currentTimeMillis();
		
		try (SQLiteDatabase db = uploadDbHelper.getWritableDatabase()) {
			
			// Create a new map of values, where column names are the keys
			ContentValues values = new ContentValues();
			values.put(UploadEntry.COLUMN_NAME_FILE, file.toString());
			values.put(UploadEntry.COLUMN_NAME_TIMESTAMP, timestamp);
			values.put(UploadEntry.COLUMN_NAME_TYPE, type.ordinal());
			if (location != null) {
				values.put(UploadEntry.COLUMN_NAME_LATITUDE, location.getLatitude());
				values.put(UploadEntry.COLUMN_NAME_LONGITUDE, location.getLongitude());
			}
	
			// Insert the new row, returning the primary key value of the new row
			long rowId = db.insertOrThrow(UploadEntry.TABLE_NAME, null, values);
			file.renameTo(new File(context.getFilesDir(), getFilename(rowId)));
			file.setLastModified(timestamp);
		}
	}
}
