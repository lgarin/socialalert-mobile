package com.bravson.socialalert.android;

import org.androidannotations.annotations.EApplication;

import android.app.Application;

import com.bravson.socialalert.common.domain.UserInfo;

@EApplication
public class AndroidApp extends Application {

	private UserInfo currentUser;
	
	public UserInfo getCurrentUser() {
		return currentUser;
	}
	
	public void setCurrentUser(UserInfo currentUser) {
		this.currentUser = currentUser;
	}
}
