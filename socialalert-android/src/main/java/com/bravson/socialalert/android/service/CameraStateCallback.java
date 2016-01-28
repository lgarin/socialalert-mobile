package com.bravson.socialalert.android.service;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;

public interface CameraStateCallback {
	
	void onReady(CameraDevice camera);
	
	void onError(CameraAccessException exception);

}
