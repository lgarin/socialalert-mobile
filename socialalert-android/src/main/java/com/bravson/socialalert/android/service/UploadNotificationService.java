package com.bravson.socialalert.android.service;

import java.net.URI;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;

import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.facade.MediaFacade;

import android.app.NotificationManager;
import android.content.Context;

@EBean(scope=Scope.Singleton)
public class UploadNotificationService {
	
	@RootContext
	Context context;

	@Bean
	MediaUploadConnection uploadConnection;
	
	@Bean
	UploadDbService uploadQueueService;
	
	@Bean
	RpcCall rpc;
	
	@SystemService
	NotificationManager notificationManager;
	
	final NavigableMap<Long, UploadNotification> notificationMap = new ConcurrentSkipListMap<>();

	private UploadNotificationState getInitialState(UploadEntry upload) {
		if (upload.isCompleted()) {
			return UploadNotificationState.COMPLETED;
		} else if (upload.isUploaded()) {
			return UploadNotificationState.INPUT_REQUIRED;
		}
		return UploadNotificationState.PENDING;
	}
	
	@AfterInject
	void loadPendingUploads() {
		uploadQueueService.purgeOldFiles();
		for (UploadEntry upload : uploadQueueService.getPendingUploads()) {
			showNotification(upload, getInitialState(upload));
		}
	}
	
	
	private class UploadProgressListener implements ProgressListener {

		private UploadNotification notification;
		
		public UploadProgressListener(UploadEntry upload) {
			notification = getNotification(upload);
		}
		
		@Override
		public void onProgress(int maxProgress, int currentProgress) {
			notificationManager.notify(notification.getNoticationId(), notification.buildUploadNotification(maxProgress, currentProgress));
		}
		
	}
	
	public void queueUpload(long fileId) {
		UploadEntry upload = uploadQueueService.findUpload(fileId);
		if (upload == null) {
			return;
		}
		showNotification(upload, getInitialState(upload));
		triggerProcessing();
	}
	
	private Long getNextFileId() {
		for (Map.Entry<Long, UploadNotification> entry : notificationMap.entrySet()) {
			if (entry.getValue().getState() == UploadNotificationState.CLAIMING) {
				return null;
			} else if (entry.getValue().getState() == UploadNotificationState.UPLOADING) {
				return null;
			}
		}
		for (Map.Entry<Long, UploadNotification> entry : notificationMap.entrySet()) {
			if (entry.getValue().getState() == UploadNotificationState.PENDING) {
				return entry.getKey();
			}
		}
		return null;
	}

	public void triggerProcessing() {
		Long fileId = getNextFileId();
		if (fileId == null) {
			return;
		}
		UploadEntry upload = uploadQueueService.findUpload(fileId);
		if (upload == null) {
			notificationMap.remove(fileId);
			triggerProcessing();
		} else {
			startProcessing(upload);
		}
		
	}

	private void startProcessing(UploadEntry upload) {
		if (upload.isCompleted()) {
			claimMedia(upload);
		} else if (!upload.isUploaded()) {
			asyncUpload(upload);
		}
	}
	
	@Background
	void asyncUpload(UploadEntry upload) {
		showNotification(upload, UploadNotificationState.UPLOADING);
		try {
			URI mediaUri = URI.create(uploadConnection.upload(upload.getFile(context), upload.getContentType(), new UploadProgressListener(upload)));
			upload = uploadQueueService.updateMediaUri(upload.getFileId(), mediaUri);
			if (upload.isEnriched()) {
				claimMedia(upload);
			} else {
				showNotification(upload, UploadNotificationState.INPUT_REQUIRED);
				startNextUpload(upload);
			}
		} catch (Exception e) {
			asyncShowError(upload, e);
		}
	}
	
	@UiThread
	void showNotification(UploadEntry upload, UploadNotificationState state) {
		UploadNotification notification = getNotification(upload);
		notification.setState(state);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(upload));
	}
	
	private UploadNotification getNotification(UploadEntry entry) {
		UploadNotification result = notificationMap.get(entry.getFileId());
		if (result == null) {
			result = new UploadNotification(context);
			notificationMap.put(entry.getFileId(), result);
		}
		return result;
	}

	void claimMedia(UploadEntry upload) {
		showNotification(upload, UploadNotificationState.CLAIMING);
		switch (upload.getType()) {
		case PICTURE:
			asyncClaimPicture(upload);
			break;
		case VIDEO:
			asyncClaimVideo(upload);
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
		notification.setState(UploadNotificationState.ERROR);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(entry));
		startNextUpload(entry);
	}

	void startNextUpload(UploadEntry previousUpload) {
		if (!notificationMap.isEmpty()) {
			Long fileId = notificationMap.firstKey();
			queueUpload(fileId);
		}
	}

	@UiThread
	void asyncShowSuccess(UploadEntry entry) {
		UploadNotification notification = getNotification(entry);
		notification.setState(UploadNotificationState.COMPLETED);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(entry));
		startNextUpload(entry);
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
