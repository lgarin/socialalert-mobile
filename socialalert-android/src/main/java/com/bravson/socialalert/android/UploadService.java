package com.bravson.socialalert.android;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.api.support.app.AbstractIntentService;

import com.bravson.socialalert.android.service.UploadNotificationService;

@EIntentService
public class UploadService extends AbstractIntentService {

	@Bean
	UploadNotificationService notificationService;
	
	public UploadService() {
		super("UploadService");
	}

	@ServiceAction
	void queueUpload(long fileId) {
		notificationService.queueUpload(fileId);
	}

	@ServiceAction
	void triggerProcessing() {
		notificationService.triggerProcessing();
	}
}
