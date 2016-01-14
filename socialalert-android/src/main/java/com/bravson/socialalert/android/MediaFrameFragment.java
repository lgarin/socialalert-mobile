package com.bravson.socialalert.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import com.bravson.socialalert.common.domain.MediaType;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

@EFragment(R.layout.media_frame)
public class MediaFrameFragment extends Fragment {

	@ViewById(R.id.imageView)
	ImageView imageView;
	
	@ViewById(R.id.videoView)
	VideoView videoView;
	
	@StringRes
	String basePreviewUrl;
	
	void clearFrame() {
		imageView.setImageURI(null);
		videoView.setVideoURI(null);
	}
	
	void showRemoteMedia(MediaType type, URI mediaUri) {
		if (type == MediaType.VIDEO) {
			// TODO save as file first
			videoView.setVideoURI(Uri.parse(basePreviewUrl + "/" + mediaUri));
			videoView.setVisibility(View.VISIBLE);
			imageView.setVisibility(View.INVISIBLE);
		} else if (type == MediaType.PICTURE) {
			//imageView.setImageURI(mediaUri);
			loadRemoteImage(mediaUri);
			videoView.setVisibility(View.INVISIBLE);
			imageView.setVisibility(View.VISIBLE);
		} else {
			videoView.setVisibility(View.INVISIBLE);
			imageView.setVisibility(View.INVISIBLE);
		}
	}
	
	void showLocalMedia(MediaType type, File mediaFile) {
		if (type == MediaType.VIDEO) {
			videoView.setVideoURI(Uri.fromFile(mediaFile));
			videoView.setVisibility(View.VISIBLE);
			imageView.setVisibility(View.INVISIBLE);
		} else if (type == MediaType.PICTURE) {
			imageView.setImageURI(Uri.fromFile(mediaFile));
			videoView.setVisibility(View.INVISIBLE);
			imageView.setVisibility(View.VISIBLE);
		} else {
			videoView.setVisibility(View.INVISIBLE);
			imageView.setVisibility(View.INVISIBLE);
		}
	}
	
	@UiThread
	public void showImage(Bitmap bitmap) {
		imageView.setImageBitmap(bitmap);
	}
	
	@Background
	public void loadRemoteImage(URI uri) {
		try {
			URL url = new URL(basePreviewUrl + "/" + uri);
			try (InputStream is = url.openStream()) {
				showImage(BitmapFactory.decodeStream(is));
			}
		} catch (IOException e) {
			//finish();
		}
	}
}
