package com.bravson.socialalert.android;

import java.util.ArrayList;

import android.widget.ImageButton;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.IntegerRes;
import org.apache.commons.lang3.time.DateUtils;

import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.common.domain.MediaInfo;
import com.bravson.socialalert.common.domain.QueryResult;
import com.bravson.socialalert.common.facade.MediaFacade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

@EActivity(R.layout.top_media)
public class TopMediaActivity extends Activity {
	
	@Bean
	RpcBlockingCall rpc;

	@ViewById(R.id.cameraButton)
	ImageButton cameraButton;

	@ViewById(R.id.mapButton)
	ImageButton mapButton;

	@ViewById(R.id.plusButton)
	ImageButton plusButton;
	
	@ViewById(R.id.gridView)
	GridView gridView;
	
	@IntegerRes
	int mediaPageSize;
	
	@IntegerRes
	int mediaMaxAge;
	
	@IntegerRes
	int mediaMaxThumbnails;

	MediaThumbnailAdapter adapter;
	
	@AfterViews
    void bindAdapter() {
        adapter = new MediaThumbnailAdapter(this);
		gridView.setAdapter(adapter);
		mapButton.setVisibility(View.INVISIBLE);
		cameraButton.setVisibility(View.INVISIBLE);
    }
	
	@Click(R.id.cameraButton)
	void onCameraClick() {
		startActivity(new Intent(this, CameraActivity_.class));
	}
	
	@Click(R.id.mapButton)
	void onMapClick() {
		startActivity(new Intent(this, MapActivity_.class));
	}

	private static void switchVisibility(ImageButton button) {
		if (button.getVisibility() == View.INVISIBLE) {
			button.setVisibility(View.VISIBLE);
		} else {
			button.setVisibility(View.INVISIBLE);
		}
	}

	@Click(R.id.plusButton)
	void onPlusClick() {
		switchVisibility(mapButton);
		switchVisibility(cameraButton);
	}

	@Override
	protected void onResume() {
		super.onResume();
		gridView.setOnScrollListener(null);
		populateGrid(null);
		gridView.setOnScrollListener(new PagerScrollListener(mediaPageSize) {
			@Override
			public void load(int page) {
				loadPage(page);
			}
		});
	}
	
	@Background
	void loadPage(int page) {
		try {
			QueryResult<MediaInfo> result = rpc.with(MediaFacade.class).searchMedia(null, null, null, null, null, mediaMaxAge * DateUtils.MILLIS_PER_DAY, page, mediaPageSize);
			populateGrid(result);
		} catch (Exception e) {
			Toast.makeText(this, "Failed query", Toast.LENGTH_LONG).show();
			if (page == 0) {
				finish();
			} else {
				gridView.setOnScrollListener(null);
			}
		}
	}
	
	@UiThread
	void populateGrid(QueryResult<MediaInfo> result) {
		if (result != null) {
			adapter.addAll(result.getContent());
			if (result.getContent().isEmpty() || adapter.getCount() > mediaMaxThumbnails) {
				gridView.setOnScrollListener(null);
			}
		} else {
			adapter.clear();
		}
	}

    @ItemClick(R.id.gridView)
    void personListItemClicked(MediaInfo mediaInfo) {
        startActivity(new Intent(this, MediaPreviewActivity_.class).putExtra("mediaUri", mediaInfo.getMediaUri()));
    }
	
	public static class MediaThumbnailAdapter extends ArrayAdapter<MediaInfo> {
		
		public MediaThumbnailAdapter(Context context) {
			super(context, R.layout.media_thumbnail, new ArrayList<MediaInfo>());
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MediaThumbnailView itemView;
	        if (convertView == null) {
	        	itemView = MediaThumbnailView_.build(getContext());
	        } else {
	        	itemView = (MediaThumbnailView) convertView;
	        }

	        itemView.bind(getItem(position));

	        return itemView;
		}
	}
}
