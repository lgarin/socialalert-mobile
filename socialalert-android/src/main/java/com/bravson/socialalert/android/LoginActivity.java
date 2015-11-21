package com.bravson.socialalert.android;

import java.io.IOException;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import com.bravson.socialalert.android.service.JsonRpcServiceFactory;
import com.bravson.socialalert.common.domain.UserInfo;
import com.bravson.socialalert.common.facade.UserFacade;

import android.app.Activity;
import android.widget.TextView;

@EActivity(R.layout.login)
public class LoginActivity extends Activity {

	int counter;
	
	@ViewById(R.id.textView1)
	TextView textView1;
	
	@Bean
	JsonRpcServiceFactory serviceFactory;
	
	@Click(R.id.button1)
	void onClick() {
		counter++;
		asyncLogin();
		textView1.setText("Click " + counter);
	}
	
	@UiThread
	void showError(Exception exception) {
		textView1.setText(exception.getMessage());
	}
	
	@Background
	void asyncLogin() {
		try {
			UserInfo info = serviceFactory.get(UserFacade.class).login("lcuien", "123");
		} catch (Exception e) {
			showError(e);
		}
	}
}
