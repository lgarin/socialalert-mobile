package com.bravson.socialalert.android;

import java.io.File;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;

import com.bravson.socialalert.common.domain.MediaType;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;

import android.widget.Button;
import android.widget.EditText;

@EActivity(R.layout.post_media)
public class PostMediaActivity extends ValidatedActivity {

	@ViewById(R.id.titleView)
	@NotEmpty
	@Order(1)
	EditText title;
	
	@ViewById(R.id.publishButton)
	Button publishButton;
	
	@Extra("imageFile")
	File imageFile;
	
	@Extra("videoFile")
	File videoFile;
	
	@FragmentById(R.id.mediaFrame)
	MediaFrameFragment mediaFrame;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (imageFile != null) {
			mediaFrame.showLocalMedia(MediaType.PICTURE, imageFile);
			startUpload(imageFile);
		} else if (videoFile != null) {
			mediaFrame.showLocalMedia(MediaType.VIDEO, videoFile);
			startUpload(videoFile);
		}
		publishButton.setEnabled(false);
	}
	
	@Background
	void startUpload(File file) {
		
	}
	
	@Click(R.id.publishButton)
	void onPublish() {
		
	}
}
