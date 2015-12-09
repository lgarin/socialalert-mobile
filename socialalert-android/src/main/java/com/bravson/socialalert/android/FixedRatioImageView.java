package com.bravson.socialalert.android;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedRatioImageView extends ImageView {
	public FixedRatioImageView(Context context)
    {
        super(context);
    }

	public FixedRatioImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

	public FixedRatioImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth() * 3 / 4);
	}
}