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
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.ViewById;

import com.bravson.socialalert.android.service.CameraService;
import com.bravson.socialalert.android.service.CameraStateCallback;
import com.bravson.socialalert.android.service.LocationService;
import com.bravson.socialalert.android.service.UploadDbService;
import com.bravson.socialalert.common.domain.MediaType;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaRecorder;
import android.support.annotation.UiThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.Toast;

@EActivity(R.layout.camera)
public class CameraActivity extends Activity {

	private static final int REQUEST_PERMISSION_CODE = 1;
	
	private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION};
	
	@ViewById(R.id.photoButton)
	ImageButton photoButton;
	
	@ViewById(R.id.videoButton)
	ImageButton videoButton;
	
	@ViewById(R.id.recordButton)
	ImageButton recordButton;
	
	@ViewById(R.id.cameraSurface)
	SurfaceView cameraSurface;
	
	@FragmentById(R.id.mediaCategory)
	MediaCategoryFragment mediaCategory;
	
	CameraCaptureSession captureSession;
	
	@Bean
	CameraService cameraService;
	
	@Bean
	LocationService locationService;
	
	@Bean
	UploadDbService uploadQueueService;
	
	ImageReader imageReader;
	
	MediaRecorder mediaRecorder;

	private volatile Location location;
	
	private volatile boolean videoMode;
	
	@UiThread
	void initServices() {
		for (String permission : REQUIRED_PERMISSIONS) {
			if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSION_CODE);
				return;
			}
		}

		locationService.requestLocationUpdate();
		
		try {
			cameraService.initCameraDevice(new CameraCallback());
		} catch (CameraAccessException e) {
			showErrorAndFinish(e);
		}
	}
	
	@UiThread
	void showErrorAndFinish(Exception e) {
		Toast.makeText(this, "No camera access", Toast.LENGTH_LONG).show();
		finish();
	}

	private void initImageReader() throws CameraAccessException {
		Size size = cameraService.getLargetImageSize();
		imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 2);
		imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
	}
	
	class ImageAvailableListener implements OnImageAvailableListener {
		
		public void onImageAvailable(ImageReader reader) {
			saveImage(reader.acquireNextImage(), getTemporaryFile(MediaType.PICTURE));
		}
	};
    
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
        
        startPost(MediaType.PICTURE);
    }
    
    @UiThread
    void startPost(MediaType mediaType) {
    	File file = getTemporaryFile(mediaType);
    	long fileId = uploadQueueService.queueFile(file, mediaType, location);
    	startService(new Intent(this, UploadService_.class).setAction(UploadService_.ACTION_QUEUE_UPLOAD).putExtra(UploadService_.FILE_ID_EXTRA, fileId));
    	finish();
    	startActivity(new Intent(this, ClaimMediaActivity_.class).putExtra(ClaimMediaActivity_.FILE_ID_EXTRA, fileId));
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	photoButton.setSelected(true);
    	initServices();
    }
	
	@Override
	protected void onPause() {
		closeCameraSession();
		
		cameraService.close();
		
		super.onPause();
	}

	void closeCameraSession() {
		if (imageReader != null) {
			imageReader.close();
			imageReader = null;
		}
		
		if (mediaRecorder != null) {
			mediaRecorder.release();
			mediaRecorder = null;
		}
		
		if (captureSession != null) {
			captureSession.close();
			captureSession = null;
		}
	}

	@UiThread
	void initCameraSession() {
		try {
			closeCameraSession();
			
			initCameraSurface();
			if (videoMode) {
				initMediaRecorder();
			} else {
				initImageReader();
			}
			ArrayList<Surface> surfaces = new ArrayList<>();
			surfaces.add(cameraSurface.getHolder().getSurface());
			if (imageReader != null) {
				surfaces.add(imageReader.getSurface());
			}
			if (mediaRecorder != null) {
				surfaces.add(mediaRecorder.getSurface());
			}
			cameraService.getCameraDevice().createCaptureSession(surfaces, new CameraActivity.CaptureCallback(), null);
		} catch (CameraAccessException e) {
			showErrorAndFinish(e);
		} catch (IOException e) {
			showErrorAndFinish(e);
		}
	}

	void initCameraSurface() {
		cameraSurface.getHolder().setFixedSize(cameraSurface.getWidth(), cameraSurface.getHeight());
	}
	
	private File getTemporaryFile(MediaType type) {
		switch (type){
		case PICTURE:
			return new File(getFilesDir(), "test.jpg");
		case VIDEO:
			return new File(getFilesDir(), "test.mp4");
		default:
			throw new IllegalArgumentException("Unsupported media type " + type);
		}
	}
	
	void initMediaRecorder() throws CameraAccessException, IOException {
		mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(getTemporaryFile(MediaType.VIDEO).getAbsolutePath());
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        Size size = cameraService.getLargetVideoSize();
        mediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOrientationHint(cameraService.getJpegOrientation());
        
        location = locationService.getCurrentLocation();
		if (location != null) {
			mediaRecorder.setLocation((float) location.getLatitude(), (float) location.getLongitude());
		}
		mediaRecorder.prepare();
    }

	private class CameraCallback implements CameraStateCallback {
		 @Override
		public void onError(CameraAccessException exception) {
			 showErrorAndFinish(exception);
		}
		
		 @Override
		public void onReady(CameraDevice camera) {
			 initCameraSession();
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
			videoButton.setEnabled(false);
			photoButton.setEnabled(false);
            try {
            	if (photoButton.isSelected()) {
            		capturePhoto();
            	} else if (recordButton.isSelected()){
            		recordButton.setSelected(false);
            		stopVideo();
            	} else {
            		startVideo();
            		recordButton.setSelected(true);
            	}
			} catch (CameraAccessException e) {
				showErrorAndFinish(e);
			}
		}
	}

	void capturePhoto() throws CameraAccessException {
		location = locationService.getCurrentLocation();
		CaptureRequest request = cameraService.createStillImageRequest(imageReader.getSurface(), location);
		captureSession.stopRepeating();
		captureSession.capture(request, null, null);
	}
	
	void startVideo() throws CameraAccessException {
		CaptureRequest request = cameraService.createRecordVideoRequest(cameraSurface.getHolder(), mediaRecorder.getSurface(), location);
		captureSession.stopRepeating();
		captureSession.setRepeatingRequest(request, null, null);
		mediaRecorder.start();
	}
	
	void stopVideo() {
		mediaRecorder.stop();
		startPost(MediaType.VIDEO);
	}
	
	private void changeCaptureMode(boolean newVideoMode) {
		if (videoMode == newVideoMode) {
			return;
		}
		photoButton.setSelected(!newVideoMode);
		videoButton.setSelected(newVideoMode);
		videoMode = newVideoMode;
		if (cameraService.isReady()) {
			initCameraSession();
		}
	}
	
	@Touch(R.id.photoButton)
	void onPhotoClick() {
		changeCaptureMode(false);
	}
	
	@Touch(R.id.videoButton)
	void onVideoClick() {
		changeCaptureMode(true);
	}
}
