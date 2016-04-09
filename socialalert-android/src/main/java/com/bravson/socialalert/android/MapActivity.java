package com.bravson.socialalert.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;
import android.widget.ImageView;
import com.bravson.socialalert.android.service.LocationService;
import com.bravson.socialalert.android.service.ProgressListener;
import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.android.service.RpcCall;
import com.bravson.socialalert.common.domain.GeoStatistic;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.domain.MediaType;
import com.bravson.socialalert.common.domain.QueryResult;
import com.bravson.socialalert.common.facade.MediaFacade;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import org.androidannotations.annotations.*;

import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import org.androidannotations.annotations.res.StringRes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Set;

@EActivity(R.layout.map)
public class MapActivity extends Activity implements OnMapReadyCallback, OnMyLocationButtonClickListener {
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

	@Bean
	LocationService locationService;

	@FragmentById(R.id.mapView)
	MapFragment mapView;

	GoogleMap map;
	ClusterManager<MediaItem> clusterManager;

	@Bean
	RpcCall rpc;

	@StringRes
	String baseThumbnailUrl;
	
	@AfterViews
	void initMap() {
		mapView.getMapAsync(this);
	}

	 @Override
    public void onMapReady(GoogleMap map) {
		this.map = map;

		 map.setOnMyLocationButtonClickListener(this);
		 //map.setOnCameraChangeListener(this);

		 clusterManager = new ClusterManager<MediaItem>(this, map);
		 clusterManager.setRenderer(new MediaItemRenderer());

		 map.setOnCameraChangeListener(clusterManager);
		 map.setOnMarkerClickListener(clusterManager);


		 enableMyLocation();
    }





	private double computeDistance(LatLngBounds latLngBounds) {
		float[] distance = new float[1];

		Location.distanceBetween(
				latLngBounds.southwest.latitude,
				latLngBounds.southwest.longitude,
				latLngBounds.northeast.latitude,
				latLngBounds.northeast.longitude,
				distance
		);

		return distance[0];
	}

	@Background
	void loadMapData(double latitude, double longitude, double radius) {
		try {
			QueryResult<MediaInfo> result = rpc.with(MediaFacade.class).searchMedia(MediaType.PICTURE, latitude, longitude, radius, null, 1000L * DateTimeConstants.MILLIS_PER_DAY, 0, 100);
			for (MediaInfo info : result.getContent()) {
				saveImage(info.getMediaUri());
			}
			showItems(result.getContent());
		} catch (Exception e) {
			Log.e("MAP", e.getMessage());
		}
	}

	@UiThread
	void showItems(List<MediaInfo> items) {

		clusterManager.clearItems();
		for (MediaInfo info : items) {
			clusterManager.addItem(new MediaItem(info));
		}
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

	private static class MediaItem implements ClusterItem {
		private MediaInfo mediaInfo;
		private LatLng latLng;

		public MediaItem(MediaInfo info) {
			this.mediaInfo = info;
			if (info.getLatitude() != null && info.getLongitude() != null) {
				this.latLng = new LatLng(info.getLatitude(), info.getLongitude());
			}
		}

		@Override
		public LatLng getPosition() {
			return latLng;
		}
	}


	public void saveImage(URI uri) {
		File file = new File(getFilesDir(), uri.getPath());
		if (file.exists()) {
			return;
		}
		file.getParentFile().mkdirs();
		try {
			URL url = new URL(baseThumbnailUrl + "/" + uri);
			try (InputStream is = url.openStream();
				OutputStream os = new FileOutputStream(file)) {
				copy(is, os);
			}
		} catch (Exception e) {
			Log.e("IMAGE", e.getMessage());
		}
	}

	private static long copy(InputStream var0, OutputStream var1) throws IOException {
		long var2 = 0L;

		int var5;
		for(byte[] var4 = new byte[8192]; (var5 = var0.read(var4)) > 0; var2 += (long)var5) {
			var1.write(var4, 0, var5);
		}

		return var2;
	}


	private class MediaItemRenderer extends DefaultClusterRenderer<MediaItem> implements GoogleMap.OnCameraChangeListener {
		private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
		private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());

		public MediaItemRenderer() {
			super(getApplicationContext(), map, clusterManager);
		}

		@Override
		public void onCameraChange(CameraPosition cameraPosition) {
			VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
			loadMapData(cameraPosition.target.latitude, cameraPosition.target.longitude, computeDistance(visibleRegion.latLngBounds) / 2000.0);
		}

		@Override
		protected void onBeforeClusterItemRendered(MediaItem media, MarkerOptions markerOptions) {
			markerOptions.title(media.mediaInfo.getTitle());
			try {
				File imageFile = new File(getFilesDir(), media.mediaInfo.getMediaUri().getPath());
				if (imageFile.canRead()) {
					markerOptions.icon(BitmapDescriptorFactory.fromPath(imageFile.getPath()));
				} else {
					markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camera_pin_pop_icon));
				}
			} catch (Exception e) {
				Log.e("MARKER", e.getMessage());
			}
		}

		@Override
		protected void onBeforeClusterRendered(Cluster<MediaItem> cluster, MarkerOptions markerOptions) {
			super.onBeforeClusterRendered(cluster, markerOptions);
		}

		@Override
		protected boolean shouldRenderAsCluster(Cluster cluster) {
			// Always render clusters.
			return cluster.getSize() > 1;
		}
	}
}