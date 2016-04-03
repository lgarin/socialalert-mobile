package com.bravson.socialalert.android;

import android.location.Location;
import com.bravson.socialalert.android.service.LocationService;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import org.androidannotations.annotations.*;

import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

@EActivity(R.layout.map)
public class MapActivity extends Activity implements OnMapReadyCallback, OnMyLocationButtonClickListener {
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

	@Bean
	LocationService locationService;

	@FragmentById(R.id.mapView)
	MapFragment mapView;

	GoogleMap map;
	
	@AfterViews
	void initMap() {
		mapView.getMapAsync(this);
	}

	 @Override
    public void onMapReady(GoogleMap map) {
		this.map = map;

		map.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
    }
	 
	 
	 private void enableMyLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        	requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else if (map != null) {
			locationService.requestLocationUpdate();
        	map.setMyLocationEnabled(true);
        }
	 }
	 
	@Override
	public boolean onMyLocationButtonClick() {
		Location location = locationService.getCurrentLocation();
		if (location != null) {
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10.0f);
			map.moveCamera(update);
			return true;
		}

		return false;
	}
}