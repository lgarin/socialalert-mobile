package com.bravson.socialalert.android.service;

import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.androidannotations.annotations.sharedpreferences.SharedPref.Scope;

@SharedPref(Scope.UNIQUE)
public interface ApplicationPreferences {

 	String username();

	String password();
}
