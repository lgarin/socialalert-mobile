package com.bravson.socialalert.android;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

import org.androidannotations.annotations.AfterInject;
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
import com.bravson.socialalert.android.service.UploadNotificationState;
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
	
	static final NavigableMap<Long, UploadNotification> notificationMap = new ConcurrentSkipListMap<>();
	
	public UploadService() {
		super("UploadService");
	}
	
	@AfterInject
	void loadPendingUploads() {
		//currentUpload.set(null);
		//notificationMap.clear();
		
		uploadQueueService.purgeOldFiles();
		for (UploadEntry upload : uploadQueueService.getPendingUploads()) {
			startProcessing(upload);
		}
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		uploadQueueService.purgeOldFiles();
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
	
	@ServiceAction
	void startUpload(long fileId) {
		UploadEntry upload = uploadQueueService.findUpload(fileId);
		if (upload == null) {
			return;
		}
		
		startProcessing(upload);
	}
	
	private long getNextUploadFileId() {
		if (notificationMap.isEmpty()) {
			return 0;
		}
		return notificationMap.firstKey();
	}

	void startProcessing(UploadEntry upload) {
		if (upload.isCompleted()) {
			claimMedia(upload);
		} else if (!upload.isUploaded() && upload.getFileId() == getNextUploadFileId()) {
			asyncUpload(upload);
		}
	}
	
	@Background
	void asyncUpload(UploadEntry upload) {
		showNotification(upload, UploadNotificationState.UPLOADING);
		try {
			URI mediaUri = URI.create(uploadConnection.upload(upload.getFile(this), upload.getContentType(), new UploadProgressListener(upload)));
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
		startNextUpload(upload);
	}
	
	private UploadNotification getNotification(UploadEntry entry) {
		UploadNotification result = notificationMap.get(entry.getFileId());
		if (result == null) {
			result = new UploadNotification(this);
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
		notificationMap.remove(entry.getFileId());
		startNextUpload(entry);
	}

	void startNextUpload(UploadEntry previousUpload) {
		if (!notificationMap.isEmpty()) {
			Long fileId = notificationMap.firstKey();
			startUpload(fileId);
		}
	}

	@UiThread
	void asyncShowSuccess(UploadEntry entry) {
		UploadNotification notification = getNotification(entry);
		notification.setState(UploadNotificationState.COMPLETED);
		notificationManager.notify(notification.getNoticationId(), notification.buildUpdateNotification(entry));
		notificationMap.remove(entry.getFileId());
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
