package com.bravson.socialalert.android;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.StringUtils;

import com.bravson.socialalert.android.service.LocationService;
import com.bravson.socialalert.android.service.MediaUploadConnection;
import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.common.domain.GeoAddress;
import com.bravson.socialalert.common.domain.MediaCategory;
import com.bravson.socialalert.common.domain.MediaConstants;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.domain.MediaType;
import com.bravson.socialalert.common.facade.MediaFacade;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
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
	
	@Extra("imageFile")
	File imageFile;
	
	@Extra("videoFile")
	File videoFile;
	
	@Extra("location")
	Location location;
	
	@FragmentById(R.id.mediaFrame)
	MediaFrameFragment mediaFrame;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	@Bean
	MediaUploadConnection uploadConnection;
	
	@Bean
	LocationService locationService;
	
	@Bean
	RpcBlockingCall rpc;
	
	private URI mediaUri;
	
	private volatile Address address;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (location != null) {
			asyncSearchLocality(location);
		}
		
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
	void asyncSearchLocality(Location location) {
		try {
			address = locationService.getAddress(location);
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
				asyncClaimPicture(mediaUri, titleView.getText().toString(), descriptionView.getText().toString(), mediaCategory.getSelectedCategory(), tagsView.getText().toString());
			} else if (videoFile != null) {
				// TODO
			}
		}
	}
	
	private GeoAddress buildGeoAddress() {
		if (address == null) {
			return null;
		}
		GeoAddress result = new GeoAddress();
		result.setCountry(address.getCountryCode());
		result.setLocality(address.getLocality());
		result.setFormattedAddress(address.toString());
		result.setLatitude(address.getLatitude());
		result.setLongitude(address.getLongitude());
		return result;
	}
	
	@Background
	void asyncClaimPicture(URI mediaUri, String title, String description, Integer categoryIndex, String tags) {
		try {
			ArrayList<MediaCategory> categories = new ArrayList<MediaCategory>();
			if (categoryIndex != null) {
				categories.add(MediaCategory.values()[categoryIndex]);
			}
			MediaInfo info = rpc.with(MediaFacade.class).claimPicture(mediaUri, title, buildGeoAddress(), categories, Arrays.asList(StringUtils.split(tags)));
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
