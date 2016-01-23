package com.bravson.socialalert.android;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.StringUtils;

import com.bravson.socialalert.android.service.MediaUploadConnection;
import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.common.domain.MediaCategory;
import com.bravson.socialalert.common.domain.MediaConstants;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.domain.MediaType;
import com.bravson.socialalert.common.domain.UserInfo;
import com.bravson.socialalert.common.facade.MediaFacade;
import com.bravson.socialalert.common.facade.UserFacade;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

@EActivity(R.layout.post_media)
public class PostMediaActivity extends ValidatedActivity {

	@ViewById(R.id.titleView)
	@NotEmpty
	@Order(1)
	EditText title;
	
	@ViewById(R.id.descriptionView)
	EditText description;
	
	@ViewById(R.id.tagsView)
	EditText tags;
	
	@ViewById(R.id.publishButton)
	Button publishButton;
	
	@Extra("imageFile")
	File imageFile;
	
	@Extra("videoFile")
	File videoFile;
	
	@FragmentById(R.id.mediaFrame)
	MediaFrameFragment mediaFrame;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	@Bean
	MediaUploadConnection uploadConnection;
	
	@Bean
	RpcBlockingCall rpc;
	
	private URI mediaUri;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (imageFile != null) {
			mediaFrame.showLocalMedia(MediaType.PICTURE, imageFile);
			startUpload(imageFile, MediaConstants.JPG_MEDIA_TYPE);
		} else if (videoFile != null) {
			mediaFrame.showLocalMedia(MediaType.VIDEO, videoFile);
			startUpload(videoFile, MediaConstants.MP4_MEDIA_TYPE);
		}
		publishButton.setEnabled(false);
	}
	
	@Background
	void startUpload(File file, String mediaType) {
		try {
			String mediaUri = uploadConnection.upload(file, mediaType);
			if (mediaUri != null) {
				enablePublish(URI.create(mediaUri));
			} else {
				// TODO
			}
		} catch (Exception e) {
			// TODO
		}
	}
	
	@UiThread
	void enablePublish(URI mediaUri) {
		publishButton.setEnabled(true);
		this.mediaUri = mediaUri;
	}

	@Click(R.id.publishButton)
	void onPublish() {
		if (validate() && mediaUri != null) {
			if (imageFile != null) {
				asyncClaimPicture(mediaUri, title.getText().toString(), description.getText().toString(), mediaCategory.getSelectedCategory(), tags.getText().toString());
			} else if (videoFile != null) {
				// TODO
			}
		}
	}
	
	@Background
	void asyncClaimPicture(URI mediaUri, String title, String description, Integer categoryIndex, String tags) {
		try {
			ArrayList<MediaCategory> categories = new ArrayList<MediaCategory>();
			if (categoryIndex != null) {
				categories.add(MediaCategory.values()[categoryIndex]);
			}
			MediaInfo info = rpc.with(MediaFacade.class).claimPicture(mediaUri, title, null, categories, Arrays.asList(StringUtils.split(tags)));
			asyncShowSuccess(info);
		} catch (Exception e) {
			//TODO
		}
	}

	@UiThread
	void asyncShowSuccess(MediaInfo info) {
		finish();
		startActivity(new Intent(this, MediaPreviewActivity_.class).putExtra("mediaUri", info.getMediaUri()));
	}
}
