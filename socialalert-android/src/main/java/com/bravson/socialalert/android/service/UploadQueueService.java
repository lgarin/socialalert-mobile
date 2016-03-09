package com.bravson.socialalert.android.service;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import com.bravson.socialalert.common.domain.MediaType;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
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
	
	private Long findOldestTimestamp() {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = db.query(UploadEntry.TABLE_NAME, UploadEntry.ALL_COLUMN_NAMES, null, null, null, null, UploadEntry.COLUMN_NAME_TIMESTAMP);
			if (c.moveToNext()) {
				UploadEntry entry = UploadEntry.map(c);
				return entry.getTimestamp();
			}
			return null;
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
	
	public long queueFile(File file, MediaType type, Location location) {
		long timestamp = System.currentTimeMillis();
		
		try (SQLiteDatabase db = uploadDbHelper.getWritableDatabase()) {
			UploadEntry entry = new UploadEntry();
			entry.setTimestamp(timestamp);
			entry.setType(type);
			if (location != null) {
				entry.setLongitude(location.getLongitude());
				entry.setLatitude(location.getLatitude());
			}
			long fileId = db.insertOrThrow(UploadEntry.TABLE_NAME, null, entry.toValues());
			entry.setFileId(fileId);
			file.renameTo(entry.getFile(context));
			file.setLastModified(timestamp);
			return fileId;
		}
	}
	
	public List<UploadEntry> getPendingUploads() {
		ArrayList<UploadEntry> result = new ArrayList<>();
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = db.query(UploadEntry.TABLE_NAME, UploadEntry.ALL_COLUMN_NAMES, null, null, null, null, UploadEntry.COLUMN_NAME_TIMESTAMP);
			while (c.moveToNext()) {
				result.add(UploadEntry.map(c));
			}
		}
		return result;
	}
	
	public UploadEntry findUpload(long fileId) {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = findById(db, fileId);
			if (c.moveToNext()) {
				return UploadEntry.map(c);
			}
		}
		return null;
	}

	private Cursor findById(SQLiteDatabase db, long fileId) {
		return db.query(UploadEntry.TABLE_NAME, UploadEntry.ALL_COLUMN_NAMES, UploadEntry._ID + " = ?", new String[] { String.valueOf(fileId) }, null, null, null);
	}
	
	public void updateClaimAttributes(long fileId, CharSequence title, CharSequence description, Integer categoryId, CharSequence tags, Address address) {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = findById(db, fileId);
			if (c.moveToNext()) {
				UploadEntry entry = UploadEntry.map(c);
				entry.setTitle(title.toString());
				entry.setDescription(description.toString());
				entry.setCategory(categoryId);
				entry.setTags(tags.toString());
				if (address != null) {
					entry.setCountry(address.getCountryCode());
					entry.setLocality(address.getLocality());
					entry.setAddress(address.toString());
				}
				db.replace(UploadEntry.TABLE_NAME, null, entry.toValues());
			}
		}
	}

	public UploadEntry updateMediaUri(long fileId, URI mediaUri) {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			Cursor c = findById(db, fileId);
			if (c.moveToNext()) {
				UploadEntry entry = UploadEntry.map(c);
				entry.setMediaUri(mediaUri);
				db.replace(UploadEntry.TABLE_NAME, null, entry.toValues());
				return entry;
			}
			return null;
		}
	}
	
	public void deleteFile(long fileId) {
		try (SQLiteDatabase db = uploadDbHelper.getReadableDatabase()) {
			db.delete(UploadEntry.TABLE_NAME, UploadEntry._ID + " = ?", new String[] { String.valueOf(fileId) });
		}
	}
}
