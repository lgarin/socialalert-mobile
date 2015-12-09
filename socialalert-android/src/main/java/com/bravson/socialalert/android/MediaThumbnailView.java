package com.bravson.socialalert.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import com.bravson.socialalert.common.domain.MediaInfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

@EViewGroup(R.layout.media_thumbnail)
public class MediaThumbnailView extends RelativeLayout {

	@StringRes
	String baseThumbnailUrl;
	
	@ViewById(R.id.imageView)
	ImageView imageView;
	
	@ViewById(R.id.thumbnailTitle)
	TextView thumbnailTitle;
	
	public MediaThumbnailView(Context context) {
		super(context);
	}
	
	public void bind(MediaInfo mediaInfo) {
		thumbnailTitle.setText(mediaInfo.getTitle());
		loadImage(mediaInfo.getMediaUri());
	}
	
	@UiThread
	public void showImage(Bitmap bitmap) {
		imageView.setImageBitmap(bitmap);
	}
	
	@Background
	public void loadImage(URI uri) {
		try {
			URL url = new URL(baseThumbnailUrl + "/" + uri);
			try (InputStream is = url.openStream()) {
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				showImage(bitmap);
			}
		} catch (IOException e) {
			// TODO
		}
	}
}
