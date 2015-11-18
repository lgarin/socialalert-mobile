package com.bravson.socialalert.android;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.widget.TextView;

@EActivity(R.layout.login)
public class LoginActivity extends Activity {

	int counter;
	
	@ViewById(R.id.textView1)
	TextView textView1;
	
	@Click(R.id.button1)
	void onClick() {
		counter++;
		textView1.setText("Click " + counter);
	}
}
