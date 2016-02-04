package com.bravson.socialalert.android.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

@EBean
public class CameraService implements AutoCloseable {

	@SystemService
	CameraManager cameraManager;
	
	@RootContext
	Activity activity;
	
	private final AtomicBoolean closing = new AtomicBoolean();
	private final AtomicReference<CameraDevice> currentCameraDevice = new AtomicReference<>();
	
	public void initCameraDevice(CameraStateCallback callback) throws CameraAccessException {

		if (currentCameraDevice.get() != null) {
			throw new CameraAccessException(CameraAccessException.MAX_CAMERAS_IN_USE);
		}
		String cameraId = findBestCameraId();
		if (cameraId == null) {
			throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
		}
		cameraManager.openCamera(cameraId, new CameraCallback(callback), null);
	}
	
	private String findBestCameraId() throws CameraAccessException {
		String firstCameraId = null;
		for (String cameraId : cameraManager.getCameraIdList()) {
			if (firstCameraId == null) {
				firstCameraId = cameraId;
			}
			CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
			Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
			Boolean flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (facing.intValue() == CameraCharacteristics.LENS_FACING_FRONT && flash.booleanValue()) {
				return cameraId;
			}
		}
		return firstCameraId;
	}
	
	private class CameraCallback extends CameraDevice.StateCallback {
		CameraStateCallback callback;
		
		public CameraCallback(CameraStateCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void onDisconnected(CameraDevice camera) {
			closing.set(true);
			camera.close();
			callback.onError(new CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED));
		}
		
		@Override
		public void onError(CameraDevice camera, int error) {
			closing.set(true);
			camera.close();
			callback.onError(new CameraAccessException(error));
		}
		
		@Override
		public void onOpened(CameraDevice camera) {
			if (closing.get()) {
				camera.close();
			} else {
				currentCameraDevice.set(camera);
				callback.onReady(camera);
			}
		}
		
		@Override
		public void onClosed(CameraDevice camera) {
			closing.set(false);
			currentCameraDevice.set(null);
		}
	}
	
	@Override
	public void close() {
		if (closing.getAndSet(true)) {
			return;
		}
		
		CameraDevice cameraDevice = currentCameraDevice.get();
		if (cameraDevice != null) {
			cameraDevice.close();
		}
	}
	
	public CaptureRequest createStillImageRequest(Surface target, Location location) throws CameraAccessException {
		CameraDevice cameraDevice = getCameraDevice();
		
		CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		requestBuilder.addTarget(target);
		requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
		requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());
        if (location != null) {
        	requestBuilder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
        }
        
		return requestBuilder.build();
	}
	
	public CaptureRequest createRecordVideoRequest(SurfaceHolder previewTarget, Surface target, Location location) throws CameraAccessException {
		CameraDevice cameraDevice = getCameraDevice();
		
		CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
		requestBuilder.addTarget(target);
		requestBuilder.addTarget(previewTarget.getSurface());
		requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
		requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());
        if (location != null) {
        	requestBuilder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
        }
        
		return requestBuilder.build();
	}
	
	public CaptureRequest createPreviewCaptureRequest(SurfaceHolder target) throws CameraAccessException {
		CameraDevice cameraDevice = getCameraDevice();
		CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
		requestBuilder.addTarget(target.getSurface());
		
		return requestBuilder.build();
	}
	
	public int getJpegOrientation() throws CameraAccessException {
		CameraCharacteristics c = cameraManager.getCameraCharacteristics(getCameraDevice().getId());
		int deviceOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();
		
		if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
			return 0;
		
		int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

		// Round device orientation to a multiple of 90
		deviceOrientation = (deviceOrientation + 45) / 90 * 90;

		// Reverse device orientation for front-facing cameras
		int lensFacing = c.get(CameraCharacteristics.LENS_FACING);
		if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT)
			deviceOrientation = -deviceOrientation;

		// Calculate desired JPEG orientation relative to camera orientation to
		// make the image upright relative to the device orientation
		return (sensorOrientation + deviceOrientation + 360) % 360;
	}

	public boolean isReady() {
		return currentCameraDevice.get() != null && !closing.get();
	}
	
	public Size getLargetImageSize() throws CameraAccessException {
		CameraDevice cameraDevice = getCameraDevice();
		CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
		StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		return Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
	}
	
	public Size getLargetVideoSize() throws CameraAccessException {
		CameraDevice cameraDevice = getCameraDevice();
		CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
		StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		return Collections.max(Arrays.asList(map.getHighSpeedVideoSizes()), new CompareSizesByArea());
	}

	public CameraDevice getCameraDevice() throws CameraAccessException {
		CameraDevice cameraDevice = currentCameraDevice.get();
		if (cameraDevice == null) {
			throw new CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED);
		}
		return cameraDevice;
	}
	
	static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
