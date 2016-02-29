package com.bravson.socialalert.android;

import java.io.IOException;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import com.bravson.socialalert.android.service.LocationService;
import com.bravson.socialalert.android.service.UploadEntry;
import com.bravson.socialalert.android.service.UploadQueueService;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;

import android.content.Intent;
import android.location.Address;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

@EActivity(R.layout.post_media)
public class PostMediaActivity extends ValidatedActivity {

	@ViewById(R.id.titleView)
	@NotEmpty
	@Order(1)
	EditText titleView;
	
	@ViewById(R.id.descriptionView)
	EditText descriptionView;
	
	@ViewById(R.id.tagsView)
	EditText tagsView;
	
	@ViewById(R.id.publishButton)
	Button publishButton;
	
	@ViewById(R.id.addressView)
	TextView addressView;
	
	@Extra("fileId")
	Long fileId;
	
	@FragmentById(R.id.mediaFrame)
	MediaFrameFragment mediaFrame;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	@Bean
	LocationService locationService;
	
	@Bean
	UploadQueueService uploadQueueService;
	
	private UploadEntry entry;
	
	private volatile Address address;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (fileId != null) {
			entry = uploadQueueService.findUpload(fileId);
		} else {
			entry = null;
		}
		
		if (entry == null) {
			finish();
		} else  {
			mediaFrame.showLocalMedia(entry.getType(), entry.getFile(this));
			if (entry.getLongitude() != null && entry.getLatitude() != null) {
				asyncSearchLocality(entry.getLatitude(), entry.getLongitude());
			}
		}
	}
	
	@Background
	void asyncSearchLocality(double latitude, double longitude) {
		try {
			address = locationService.getAddress(latitude, longitude);
			if (address != null) {
				asyncDisplayAddress(address);
			}
		} catch (IOException e) {
			// ignore
		}
	}
	
	@UiThread
	void asyncDisplayAddress(Address address) {
		addressView.setText(address.getLocality() + " - " + address.getCountryName());
	}

	@Click(R.id.publishButton)
	void onPublish() {
		if (validate()) {
			uploadQueueService.updateClaimAttributes(entry.getFileId(), titleView.getText(), descriptionView.getText(), mediaCategory.getSelectedCategory(), tagsView.getText(), address);
			startService(new Intent(this, UploadService_.class).setAction(UploadService_.ACTION_START_UPLOAD).putExtra(UploadService_.FILE_ID_EXTRA, entry.getFileId()));
			finish();
		}
	}
}
