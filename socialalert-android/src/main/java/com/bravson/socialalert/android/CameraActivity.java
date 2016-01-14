package com.bravson.socialalert.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

@EActivity(R.layout.camera)
public class CameraActivity extends Activity {

	private static final int REQUEST_CAMERA_PERMISSION = 1;
	
	@ViewById(R.id.cameraSurface)
	SurfaceView cameraSurface;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	@SystemService
	CameraManager cameraManager;
	
	CameraDevice cameraDevice;
	
	CameraCaptureSession captureSession;
	
	ImageReader imageReader;
	
	MediaRecorder mediaRecorder;
	
	private Semaphore cameraOpenCloseLock = new Semaphore(1);
	
	void initCameraDevice() {
		if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
	         requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
	         return;
	    }

		try {
			cameraManager.openCamera(findBestCameraId(), new CameraCallback(), null);
		} catch (CameraAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initImageReader(String cameraId) throws CameraAccessException {
		CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
		StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
		imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
		imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
	}
	
	class ImageAvailableListener implements OnImageAvailableListener {
		
		public void onImageAvailable(ImageReader reader) {
			saveImage(reader.acquireNextImage(), new File(getFilesDir(), "test.jpg"));
		}
	};
	
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    	if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    		initCameraDevice();
    	} else {
    		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    	}
    }
    
    private void writeBuffer(ByteBuffer buffer, File file) throws IOException {
    	FileOutputStream output = openFileOutput(file.getName(), MODE_PRIVATE);
    	try {
	    	while (buffer.hasRemaining()) {
	    		output.getChannel().write(buffer);
	    	}
    	} finally {
    		output.close();
    	}
    }
    
    @Background
    void saveImage(Image image, File file) {
    	ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        try {
        	writeBuffer(buffer, file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
        	image.close();
        }
        
        startPost(file);
    }
    
    @UiThread
    void startPost(File file) {
    	startActivity(new Intent(this, PostMediaActivity_.class).putExtra("imageFile", file));
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	initCameraDevice();
    }
	
	@Override
	protected void onPause() {
		if (imageReader != null) {
			imageReader.close();
			imageReader = null;
		}
		
		if (captureSession != null) {
			captureSession.close();
			captureSession = null;
		}
		
		if (cameraDevice != null) {
			closeCameraDevice();
		}
		
		super.onPause();
	}

	void closeCameraDevice() {
		try {
			cameraOpenCloseLock.acquire();
			cameraDevice.close();
			cameraDevice = null;
		} catch (InterruptedException e) {
		} finally {
			cameraOpenCloseLock.release();
		}
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
	
	void initCameraSession() {
		try {
			initCameraSurface();
			initImageReader(cameraDevice.getId());
			ArrayList<Surface> surfaces = new ArrayList<>();
			surfaces.add(cameraSurface.getHolder().getSurface());
			if (imageReader != null) {
				surfaces.add(imageReader.getSurface());
			}
			if (mediaRecorder != null) {
				surfaces.add(mediaRecorder.getSurface());
			}
			cameraDevice.createCaptureSession(surfaces, new CameraActivity.CaptureCallback(), null);
		} catch (CameraAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initCameraSurface() {
		cameraSurface.getHolder().setFixedSize(cameraSurface.getWidth(), cameraSurface.getHeight());
	}

	private class CameraCallback extends CameraDevice.StateCallback {
		@Override
		public void onDisconnected(CameraDevice camera) {
			cameraOpenCloseLock.release();
			camera.close();
			cameraDevice = null;
			cameraSurface.setVisibility(SurfaceView.INVISIBLE);
		}
		
		@Override
		public void onError(CameraDevice camera, int error) {
			cameraOpenCloseLock.release();
			camera.close();
			cameraDevice = null;
			cameraSurface.setVisibility(SurfaceView.INVISIBLE);
			// TODO show error
			finish();
		}
		
		@Override
		public void onOpened(CameraDevice camera) {
			cameraOpenCloseLock.release();
			cameraDevice = camera;
			initCameraSession();
		}
	}
	
	private class CaptureCallback extends CameraCaptureSession.StateCallback {

		@Override
		public void onConfigured(CameraCaptureSession session) {
			if (cameraDevice == null) {
				return;
			}
			
			try {
				captureSession = session;
				CaptureRequest.Builder requestBuilder = session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
				requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
				requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
				requestBuilder.addTarget(cameraSurface.getHolder().getSurface());
				
				session.setRepeatingRequest(requestBuilder.build(), null, null);
				
			} catch (CameraAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onConfigureFailed(CameraCaptureSession session) {
			// TODO Auto-generated method stub
			captureSession = null;
		}
	}
	
	static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
	
	@Click(R.id.recordButton)
	void onRecordClick() {
		if (captureSession != null) {
            try {
            	CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
    			captureBuilder.addTarget(imageReader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
				captureSession.stopRepeating();
				captureSession.capture(captureBuilder.build(), null, null);
			} catch (CameraAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*
	static class ImageCaptureCallback extends android.hardware.camera2.CameraCaptureSession.CaptureCallback {
		@Override
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
			
		}
	}
	*/
}
