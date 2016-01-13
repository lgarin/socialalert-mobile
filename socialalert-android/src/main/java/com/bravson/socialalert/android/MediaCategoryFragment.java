package com.bravson.socialalert.android;

import java.util.Arrays;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

@EFragment(R.layout.media_category)
public class MediaCategoryFragment extends Fragment {

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
	
	@AfterViews
	void initCategoryButtons() {
		for (ImageButton button : getCategoryButtons()) {
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
	
	public Integer getSelectedCategory() {
		int index = 0;
		for (ImageButton button : getCategoryButtons()) {
			if (button.isSelected()) {
				return index;
			}
			index++;
		}
		return null;
	}
}
