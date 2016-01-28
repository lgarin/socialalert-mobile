package com.bravson.socialalert.android.service;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

@EBean
public class LocationService {

	@SystemService
	LocationManager locationManager;
	
	private volatile Location updatedLocation;
	
	public Location getCurrentLocation() {
		if (updatedLocation != null) {
			return updatedLocation;
		}
		return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	
	@Background
	public void asyncUpdateLocation() {
		locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationUpdateListener(), null);
	}
	
	class LocationUpdateListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			updatedLocation = location;
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
	}
}
