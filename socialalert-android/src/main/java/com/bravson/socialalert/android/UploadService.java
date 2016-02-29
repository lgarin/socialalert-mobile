package com.bravson.socialalert.android;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.api.support.app.AbstractIntentService;

import com.bravson.socialalert.android.service.MediaUploadConnection;
import com.bravson.socialalert.android.service.ProgressListener;
import com.bravson.socialalert.android.service.RpcCall;
import com.bravson.socialalert.android.service.UploadEntry;
import com.bravson.socialalert.android.service.UploadNotification;
import com.bravson.socialalert.android.service.UploadQueueService;
import com.bravson.socialalert.common.domain.GeoAddress;
import com.bravson.socialalert.common.domain.MediaCategory;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.facade.MediaFacade;

import android.app.NotificationManager;

@EIntentService
public class UploadService extends AbstractIntentService {

	@Bean
	MediaUploadConnection uploadConnection;
	
	@Bean
	UploadQueueService uploadQueueService;
	
	@Bean
	RpcCall rpc;
	
	@SystemService
	NotificationManager notificationManager;
	
	@StringRes(R.string.uploadContentTitle)
	String uploadContentTitle;
	
	@StringRes(R.string.uploadContentProgress)
	String uploadContentProgress;
	
	static final Map<Long, UploadNotification> notificationMap = new ConcurrentHashMap<>();
	
	public UploadService() {
		super("UploadService");
	}
	
	private class UploadProgressListener implements ProgressListener {

		private UploadNotification notification;
		
		public UploadProgressListener(UploadEntry upload) {
			this.notification = getNotification(upload);
		}
		
		@Override
		public void onProgress(int maxProgress, int currentProgress) {
			notificationManager.notify(notification.getNoticationId(), notification.buildUploadNotification(maxProgress, currentProgress));
		}
		
	}
	
	@ServiceAction
	void startUpload(long fileId) {
		UploadEntry upload = uploadQueueService.findUpload(fileId);
		if (upload == null) {
			return;
		}
		
		UploadNotification notifcation = getNotification(upload);
		if (upload.isCompleted()) {
			claimMedia(upload);
		} else if (!upload.isUploaded() && notifcation.beginUpload()) {
			showNotification(upload);
			asyncUpload(upload);
		}
	}
	
	@Background
	void asyncUpload(UploadEntry upload) {
		try {
			URI mediaUri = URI.create(uploadConnection.upload(upload.getFile(this), upload.getContentType(), new UploadProgressListener(upload)));
			upload = uploadQueueService.updateMediaUri(upload.getFileId(), mediaUri);
			showNotification(upload);
			claimMedia(upload);
		} catch (Exception e) {
			asyncShowError(upload, e);
		}
	}
	
	@UiThread
	void showNotification(UploadEntry upload) {
		UploadNotification notification = getNotification(upload);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(upload));
	}
	
	private UploadNotification getNotification(UploadEntry entry) {
		UploadNotification result = notificationMap.get(entry.getFileId());
		if (result == null) {
			result = new UploadNotification(this);
			notificationMap.put(entry.getFileId(), result);
		}
		return result;
	}

	void claimMedia(UploadEntry entry) {
		switch (entry.getType()) {
		case PICTURE:
			asyncClaimPicture(entry);
			break;
		case VIDEO:
			asyncClaimVideo(entry);
			break;
		}
	}

	@Background
	void asyncClaimPicture(UploadEntry entry) {
		try {
			MediaInfo info = rpc.with(MediaFacade.class).claimPicture(entry.getMediaUri(), entry.getTitle(), entry.buildGeoAddress(), entry.getCategoryList(), entry.getTagList());
			uploadQueueService.deleteFile(entry.getFileId());
			asyncShowSuccess(entry);
		} catch (Exception e) {
			asyncShowError(entry, e);
		}
	}
	
	@UiThread
	void asyncShowError(UploadEntry entry, Exception e) {
		UploadNotification notification = getNotification(entry);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(entry));
		notification.resetUpload();
	}

	@UiThread
	void asyncShowSuccess(UploadEntry entry) {
		// TODO Auto-generated method stub
		UploadNotification notification = getNotification(entry);
		notificationManager.cancel(notification.getNoticationId());
		notificationMap.remove(entry.getFileId());
	}

	@Background
	void asyncClaimVideo(UploadEntry entry) {
		try {
			MediaInfo info = rpc.with(MediaFacade.class).claimVideo(entry.getMediaUri(), entry.getTitle(), entry.buildGeoAddress(), entry.getCategoryList(), entry.getTagList());
			uploadQueueService.deleteFile(entry.getFileId());
			asyncShowSuccess(entry);
		} catch (Exception e) {
			asyncShowError(entry, e);
		}
	}
}
