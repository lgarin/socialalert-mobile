package com.bravson.socialalert.android.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.bravson.socialalert.android.MediaPreviewActivity_;
import com.bravson.socialalert.android.PostMediaActivity_;
import com.bravson.socialalert.android.R;
import com.bravson.socialalert.android.UploadService_;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class UploadNotification {

	private final static AtomicInteger ID_COUNTER = new AtomicInteger(); 
	
	private final int noticationId;
	
	private Context context;
	private Notification.Builder builder;
	
	private UploadNotificationState state;
	
	public UploadNotification(Context context) {
		this.context = context;
		this.builder = new Notification.Builder(context);
		noticationId = ID_COUNTER.incrementAndGet();
		state = UploadNotificationState.PENDING;
	}
	
	public int getNoticationId() {
		return noticationId;
	}
	
	private int getStatusStringId() {
		switch (state) {
		case PENDING:
			return R.string.uploadContentPending;
		case UPLOADING:
			return R.string.uploadContentProgress;
		case INPUT_REQUIRED:
			return R.string.uploadContentBlocked;
		case CLAIMING:
			return R.string.uploadContentCompleted; // TODO
		case COMPLETED:
			return R.string.uploadContentCompleted;
		case ERROR:
			return R.string.uploadContentFailed;
		default:
			throw new IllegalStateException("Unexpected state " + state);
		}
	}
	
	public synchronized Notification buildUpdateNotification(UploadEntry upload) {
		String uploadContentTitle = context.getString(R.string.uploadContentTitle);
		String uploadContentProgress = context.getString(getStatusStringId());
		builder.setWhen(upload.getTimestamp()).setOngoing(!upload.isCompleted());
		builder.setContentTitle(uploadContentTitle).setContentText(uploadContentProgress).setSmallIcon(R.drawable.alarm63);
		if (!upload.isEnriched()) {
			Intent intent = new Intent(context, PostMediaActivity_.class).putExtra(PostMediaActivity_.FILE_ID_EXTRA, upload.getFileId());
			builder.setContentIntent(PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		} else if (state == UploadNotificationState.COMPLETED) {
			Intent intent = new Intent(context, MediaPreviewActivity_.class).putExtra(MediaPreviewActivity_.MEDIA_URI_EXTRA, upload.getMediaUri());
			builder.setContentIntent(PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		} else if (state == UploadNotificationState.ERROR) {
			Intent intent = new Intent(context, UploadService_.class).putExtra(UploadService_.FILE_ID_EXTRA, upload.getFileId());
			builder.setContentIntent(PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		}
		return builder.build();
	}
	
	public synchronized Notification buildUploadNotification(int maxProgress, int currentProgress) {
		builder.setProgress(maxProgress, currentProgress, false);
		return builder.build();
	}
	
	public synchronized void setState(UploadNotificationState newState) {
		state = newState;
	}
	
	public synchronized UploadNotificationState getState() {
		return state;
	}
}
