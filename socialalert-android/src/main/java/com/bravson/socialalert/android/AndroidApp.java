package com.bravson.socialalert.android;

import org.androidannotations.annotations.EApplication;

import com.bravson.socialalert.common.domain.UserInfo;

import android.app.Application;

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
