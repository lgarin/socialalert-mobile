package com.bravson.socialalert.android;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public abstract class PagerScrollListener implements OnScrollListener {
    private final int pageSize;
    private int itemCount;
    private boolean isLoading;

    public PagerScrollListener(int pageSize) {
        this.pageSize = pageSize;
    }

    public abstract void load(int page);

    @Override
    public final void onScrollStateChanged(AbsListView view, int scrollState) {
        // Do Nothing
    }

    @Override
    public final void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
    	if (!isLoading && totalItemCount == 0) {
            itemCount = totalItemCount;
            isLoading = true;
            load(0);
        } else if (isLoading && (totalItemCount > itemCount)) {
        	itemCount = totalItemCount;
            isLoading = false;
        } else if (!isLoading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + pageSize / 2)) {
        	isLoading = true;
            load((itemCount + pageSize - 1) / pageSize);
        }
    }
}
