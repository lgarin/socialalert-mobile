package com.bravson.socialalert.android.service;

import java.io.File;
import java.net.URI;

import com.bravson.socialalert.common.domain.MediaType;

import android.content.ContentValues;
import android.database.Cursor;
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
    public static final String COLUMN_NAME_COUNTRY = "country";
    public static final String COLUMN_NAME_LOCALITY = "locality";
    public static final String COLUMN_NAME_ADDRESS = "address";
    
	public static final String[] ALL_COLUMN_NAMES = { _ID, COLUMN_NAME_FILE, COLUMN_NAME_TYPE, COLUMN_NAME_TIMESTAMP,
			COLUMN_NAME_LONGITUDE, COLUMN_NAME_LATITUDE, COLUMN_NAME_URI, COLUMN_NAME_CATEGORY, COLUMN_NAME_TITLE,
			COLUMN_NAME_DESCRIPTION, COLUMN_NAME_TAGS, COLUMN_NAME_COUNTRY, COLUMN_NAME_LOCALITY, COLUMN_NAME_ADDRESS }; 
	
	public static UploadEntry map(Cursor cursor) {
		UploadEntry result = new UploadEntry();
		int index = 0;
		result.fileId = cursor.getLong(index++);
		result.file = new File(cursor.getString(index++));
		result.type = MediaType.valueOf(cursor.getString(index++));
		result.timestamp = cursor.getLong(index++);
		if (!cursor.isNull(index)) {
			result.longitude = cursor.getDouble(index++);
		}
		if (!cursor.isNull(index)) {
			result.latitude = cursor.getDouble(index++);
		}
		if (!cursor.isNull(index)) {
			result.mediaUri = URI.create(cursor.getString(index++));
		}
		if (!cursor.isNull(index)) {
			result.category = cursor.getInt(index++);
		}
		if (!cursor.isNull(index)) {
			result.title = cursor.getString(index++);
		}
		if (!cursor.isNull(index)) {
			result.description = cursor.getString(index++);
		}
		if (!cursor.isNull(index)) {
			result.tags = cursor.getString(index++);
		}
		if (!cursor.isNull(index)) {
			result.country = cursor.getString(index++);
		}
		if (!cursor.isNull(index)) {
			result.locality = cursor.getString(index++);
		}
		if (!cursor.isNull(index)) {
			result.address = cursor.getString(index++);
		}
		return result;
	}
	
	private Long fileId;
	
	private File file;
	
	private URI mediaUri;

	private MediaType type;

    private String title;
    
    private String description;
    
    private String tags;
    
    private Integer category;
	
	private Long timestamp;
	
	private Double longitude;
	
	private Double latitude;
	
	private String locality;
	
	private String country;
	
	private String address;

	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(UploadEntry._ID, fileId);
		values.put(UploadEntry.COLUMN_NAME_FILE, file.toString());
		values.put(UploadEntry.COLUMN_NAME_TIMESTAMP, timestamp);
		values.put(UploadEntry.COLUMN_NAME_TYPE, type.name());
		values.put(UploadEntry.COLUMN_NAME_LATITUDE, latitude);
		values.put(UploadEntry.COLUMN_NAME_LONGITUDE, longitude);
		values.put(UploadEntry.COLUMN_NAME_URI, mediaUri.toString());
		values.put(UploadEntry.COLUMN_NAME_CATEGORY, category);
		values.put(UploadEntry.COLUMN_NAME_TITLE, title);
		values.put(UploadEntry.COLUMN_NAME_DESCRIPTION, description);
		values.put(UploadEntry.COLUMN_NAME_TAGS, tags);
		values.put(UploadEntry.COLUMN_NAME_COUNTRY, country);
		values.put(UploadEntry.COLUMN_NAME_LOCALITY, locality);
		values.put(UploadEntry.COLUMN_NAME_ADDRESS, address);
		return values;
	}
	
	public long getFileId() {
		return fileId;
	}

	public void setFileId(long fileId) {
		this.fileId = fileId;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
    
    public URI getMediaUri() {
		return mediaUri;
	}

	public void setMediaUri(URI mediaUri) {
		this.mediaUri = mediaUri;
	}

	public MediaType getType() {
		return type;
	}

	public void setType(MediaType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public Integer getCategory() {
		return category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
}
