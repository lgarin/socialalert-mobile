package com.bravson.socialalert.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.domain.MediaType;
import com.bravson.socialalert.common.facade.MediaFacade;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

@EActivity(R.layout.media_preview)
public class MediaPreviewActivity extends Activity {

	@ViewById(R.id.profileView)
	ImageView profileView;
	
	@ViewById(R.id.usernameView)
	TextView usernameView;
	
	@ViewById(R.id.timestampView)
	TextView timestampView;
	
	@ViewById(R.id.titleView)
	TextView titleView;
	
	@ViewById(R.id.descriptionView)
	TextView descriptionView;
	
	@ViewById(R.id.likeCountView)
	TextView likeCountView;
	
	@ViewById(R.id.commentCountView)
	TextView commentCountView;
	
	@FragmentById(R.id.mediaFrame)
	MediaFrameFragment mediaFrame;
	
	@Bean
	RpcBlockingCall rpc;
	
	@StringRes
	String baseThumbnailUrl;
	
	@Extra("mediaUri")
	URI mediaUri;
	
	@Override
	protected void onResume() {
		super.onResume();
		clearData();
		if (mediaUri != null) {
			asyncLoadData(mediaUri);
		} else {
			finish();
		}
	}
	
	void clearData() {
		mediaFrame.clearFrame();
		profileView.setImageResource(R.drawable.user168);
		titleView.setText("");
		descriptionView.setText("");
		usernameView.setText("");
		timestampView.setText("");
		commentCountView.setText("");
		likeCountView.setText("");
	}

	@Background
	void asyncLoadData(URI mediaUri) {
		try {
			MediaInfo info = rpc.with(MediaFacade.class).viewMediaDetail(mediaUri);
			showInfo(info);
		} catch (Exception e) {
			finish();
		}
	}
	
	@UiThread
	void showInfo(MediaInfo info) {
		mediaFrame.showRemoteMedia(info.getType(), info.getMediaUri());
		usernameView.setText(info.getCreator());
		timestampView.setText(DateUtils.getRelativeTimeSpanString(info.getTimestamp().getMillis()));
		titleView.setText(info.getTitle());
		descriptionView.setText(info.getDescription());
		likeCountView.setText(String.format("%,d", info.getLikeCount()));
		commentCountView.setText(String.format("%,d", info.getCommentCount()));
	}
	
	@UiThread
	public void showProfileImage(Drawable bitmap) {
		profileView.setImageDrawable(bitmap);
	}
	
	@Background
	public void loadProfileImage(URI uri) {
		try {
			URL url = new URL(baseThumbnailUrl + "/" + uri);
			try (InputStream is = url.openStream()) {
				showProfileImage(RoundedBitmapDrawableFactory.create(getResources(), is));
			}
		} catch (IOException e) {
			//finish();
		}
	}
}