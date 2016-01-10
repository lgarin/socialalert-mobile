package com.bravson.socialalert.android;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff.Mode;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

@EActivity(R.layout.camera)
public class CameraActivity extends Activity {

	private static final int REQUEST_CAMERA_PERMISSION = 1;
	
	@ViewById(R.id.cameraSurface)
	SurfaceView cameraSurface;
	
	@ViewById(R.id.category1)
	ImageButton category1Button;
	
	@ViewById(R.id.category2)
	ImageButton category2Button;
	
	@ViewById(R.id.category3)
	ImageButton category3Button;
	
	@ViewById(R.id.category4)
	ImageButton category4Button;
	
	@ViewById(R.id.category5)
	ImageButton category5Button;
	
	@ViewById(R.id.category6)
	ImageButton category6Button;
	
	@ViewById(R.id.category7)
	ImageButton category7Button;
	
	@ViewById(R.id.category8)
	ImageButton category8Button;
	
	
	@SystemService
	CameraManager cameraManager;
	
	CameraDevice cameraDevice;
	
	CameraCaptureSession captureSession;
	
	@AfterViews
	void initCategoryButtons() {
		for (final ImageButton button : getCategoryButtons()) {
			button.setOnClickListener(new CategoryButtonClickListener());
		}
	}
	
	private class CategoryButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (!v.isSelected()) {
				deselectOtherButtons(v);
				v.setSelected(true);
			}
		}

		private void deselectOtherButtons(View v) {
			for (ImageButton b : getCategoryButtons()) {
				if (b != v) {
					b.setSelected(false);
				}
			}
		}
	}
	
	private List<ImageButton> getCategoryButtons() {
		return Arrays.asList(category1Button, category2Button, category3Button, category4Button, category5Button, category6Button, category7Button, category8Button);
	}
	
	@AfterViews
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
	
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    	if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    		initCameraDevice();
    	} else {
    		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    	}
    }
	
	@Override
	protected void onPause() {
		if (captureSession != null) {
			captureSession.close();
		}
		if (cameraDevice != null) {
			cameraDevice.close();
			cameraDevice = null;
		}
		super.onPause();
	}
	
	private String findBestCameraId() throws CameraAccessException {
		String firstCameraId = null;
		for (String cameraId : cameraManager.getCameraIdList()) {
			if (firstCameraId == null) {
				firstCameraId = cameraId;
			}
			CameraCharacteristics characterstics = cameraManager.getCameraCharacteristics(cameraId);
			if (characterstics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE).booleanValue()) {
				return cameraId;
			}
		}
		return firstCameraId;
	}
	
	private class CameraCallback extends CameraDevice.StateCallback {
		@Override
		public void onDisconnected(CameraDevice camera) {
			camera.close();
			cameraDevice = null;
			captureSession = null;
			cameraSurface.setVisibility(SurfaceView.INVISIBLE);
		}
		
		@Override
		public void onError(CameraDevice camera, int error) {
			camera.close();
			cameraDevice = null;
			captureSession = null;
			cameraSurface.setVisibility(SurfaceView.INVISIBLE);
			// TODO show error
			finish();
		}
		
		@Override
		public void onOpened(CameraDevice camera) {
			cameraDevice = camera;
			try {
				captureSession = null;
				cameraSurface.getHolder().setFixedSize(cameraSurface.getWidth(), cameraSurface.getHeight());
				camera.createCaptureSession(Collections.singletonList(cameraSurface.getHolder().getSurface()), new CameraActivity.CaptureCallback(), null);
			} catch (CameraAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
}
