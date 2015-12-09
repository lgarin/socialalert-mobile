package com.bravson.socialalert.android;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public abstract class PagerScrollListener implements OnScrollListener {
    private final int bufferItemCount;
    private int currentPage = -1;
    private int itemCount;
    private boolean isLoading;

    public PagerScrollListener(int bufferItemCount) {
        this.bufferItemCount = bufferItemCount;
    }

    public abstract void loadMore(int page, int totalItemsCount);

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Do Nothing
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
    	if (!isLoading && totalItemCount == 0) {
        	currentPage = -1;
            itemCount = totalItemCount;
            isLoading = true;
            loadMore(0, totalItemCount);
        } else if (isLoading && (totalItemCount > itemCount)) {
        	itemCount = totalItemCount;
            isLoading = false;
            currentPage++;
        } else if (!isLoading && (totalItemCount - visibleItemCount)<=(firstVisibleItem + bufferItemCount)) {
        	isLoading = true;
            loadMore(currentPage + 1, totalItemCount);
        }
    }
}
