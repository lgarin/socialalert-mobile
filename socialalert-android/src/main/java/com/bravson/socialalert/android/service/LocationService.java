package com.bravson.socialalert.android.service;

import java.io.IOException;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

@EBean
public class LocationService {

	@SystemService
	LocationManager locationManager;
	
	@RootContext
	Activity activity;
	
	private volatile Location updatedLocation;
	
	public Location getCurrentLocation() {
		if (updatedLocation != null) {
			return updatedLocation;
		}
		return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	
	public void requestLocationUpdate() {
		locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationUpdateListener(), null);
	}
	
	public Address getAddress(Location location) throws IOException {
		List<Address> addressList = new Geocoder(activity).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
		if (addressList.isEmpty()) {
			return null;
		}
		return addressList.get(0);
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
