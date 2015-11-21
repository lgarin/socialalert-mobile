package com.bravson.socialalert.android;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.widget.EditText;
import android.widget.Toast;

import com.bravson.socialalert.android.service.ApplicationPreferences_;
import com.bravson.socialalert.android.service.JsonRpcServiceFactory;
import com.bravson.socialalert.common.domain.UserInfo;
import com.bravson.socialalert.common.facade.UserFacade;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Password;

@EActivity(R.layout.login)
public class LoginActivity extends ValidatedActivity {

	@ViewById(R.id.emailAddress)
	@NotEmpty
	@Email
	@Order(1)
	EditText emailAddress;
	
	@ViewById(R.id.password)
	@Password(min = 3)
	@Order(2)
	EditText password;
	
	@Pref
	ApplicationPreferences_ preferences;
	
	@Bean
	JsonRpcServiceFactory serviceFactory;
	
	@Override
	protected void onResume() {
		super.onResume();
		if (preferences.username().exists()) {
			emailAddress.setText(preferences.username().get());
			password.requestFocus();
		}
	}
	
	@Click(R.id.login)
	void onClick() {
		if (validate()) {
			asyncLogin(emailAddress.getText().toString(), password.getText().toString());
		}
	}
	
	@UiThread
	void showError(Exception exception) {
		Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();;
	}
	
	@Background
	void asyncLogin(String user, String pwd) {
		try {
			UserInfo info = serviceFactory.get(UserFacade.class).login(user, pwd);
			preferences.username().put(user);
		} catch (Exception e) {
			showError(e);
		}
	}
}
