package com.bravson.socialalert.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import com.bravson.socialalert.android.service.CameraService;
import com.bravson.socialalert.android.service.CameraStateCallback;
import com.bravson.socialalert.android.service.LocationService;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Toast;

@EActivity(R.layout.camera)
public class CameraActivity extends Activity {

	private static final int REQUEST_PERMISSION_CODE = 1;
	
	@ViewById(R.id.cameraSurface)
	SurfaceView cameraSurface;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	CameraCaptureSession captureSession;
	
	@Bean
	CameraService cameraService;
	
	@Bean
	LocationService locationService;
	
	ImageReader imageReader;
	
	MediaRecorder mediaRecorder;

	private volatile Location location;
	
	@UiThread
	void initServices() {
		if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
	         requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
	         return;
	    }

		locationService.requestLocationUpdate();
		
		try {
			cameraService.initCameraDevice(new CameraCallback());
		} catch (CameraAccessException e) {
			showErrorAndFinish(e);
		}
	}
	
	@UiThread
	void showErrorAndFinish(CameraAccessException e) {
		Toast.makeText(this, "No camera access", Toast.LENGTH_LONG).show();
		finish();
	}

	private void initImageReader(String cameraId) throws CameraAccessException {
		Size largest = cameraService.getLargetPictureSize();
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
    	if (requestCode == REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
    		initServices();
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
    	startActivity(new Intent(this, PostMediaActivity_.class).putExtra("imageFile", file).putExtra("location", location));
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	initServices();
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
		
		cameraService.close();
		
		super.onPause();
	}


	void initCameraSession(CameraDevice cameraDevice) {
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
			showErrorAndFinish(e);
		}
	}

	void initCameraSurface() {
		cameraSurface.getHolder().setFixedSize(cameraSurface.getWidth(), cameraSurface.getHeight());
	}

	private class CameraCallback implements CameraStateCallback {
		 @Override
		public void onError(CameraAccessException exception) {
			 showErrorAndFinish(exception);
		}
		
		 @Override
		public void onReady(CameraDevice camera) {
			 initCameraSession(camera);
		}
	}
	
	private class CaptureCallback extends CameraCaptureSession.StateCallback {

		@Override
		public void onConfigured(CameraCaptureSession session) {
			if (!cameraService.isReady()) {
				return;
			}
			
			try {
				captureSession = session;
				CaptureRequest request = cameraService.createPreviewCaptureRequest(cameraSurface.getHolder());
				session.setRepeatingRequest(request, null, null);
			} catch (CameraAccessException e) {
				showErrorAndFinish(e);
			}
		}

		@Override
		public void onConfigureFailed(CameraCaptureSession session) {
			captureSession = null;
		}
	}
	
	@Click(R.id.recordButton)
	void onRecordClick() {
		if (captureSession != null) {
            try {
                location = locationService.getCurrentLocation();
				CaptureRequest request = cameraService.createStillImageRequest(imageReader.getSurface(), location);
				captureSession.stopRepeating();
				captureSession.capture(request, null, null);
			} catch (CameraAccessException e) {
				showErrorAndFinish(e);
			}
		}
	}
}
