package com.bravson.socialalert.android.service;

import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultLong;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.androidannotations.annotations.sharedpreferences.SharedPref.Scope;
import org.apache.commons.lang3.time.DateUtils;

@SharedPref(Scope.UNIQUE)
public interface ApplicationPreferences {

 	String username();

	String password();
	
	@DefaultLong(value=3650 * DateUtils.MILLIS_PER_DAY)
	long mediaMaxAge();
	
	@DefaultInt(value=21)
	int mediaPageSize();
}
