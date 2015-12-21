package com.bravson.socialalert.android;

import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.bravson.socialalert.android.service.ApplicationPreferences_;
import com.bravson.socialalert.android.service.RpcBlockingCall;
import com.bravson.socialalert.common.domain.UserInfo;
import com.bravson.socialalert.common.facade.UserFacade;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Password;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
	
	@ViewById(R.id.login)
	Button login;
	
	@Pref
	ApplicationPreferences_ preferences;
	
	@App
	AndroidApp application;
	
	@Bean
	RpcBlockingCall rpc;
	
	@Override
	protected void onResume() {
		super.onResume();
		if (preferences.username().exists()) {
			emailAddress.setText(preferences.username().get());
			password.requestFocus();
		} else {
			emailAddress.requestFocus();
		}
	}

	@Click(R.id.login)
	void onLoginClick() {
		if (validate()) {
			asyncLogin(emailAddress.getText().toString(), password.getText().toString());
		}
	}
	
	@Click(R.id.forgetPassword)
	void onForgetPassword() {
		Toast.makeText(this, "Forget password", Toast.LENGTH_LONG).show();
	}
	
	@UiThread
	void asyncShowLoginSuccess(UserInfo info) {
		preferences.username().put(info.getEmail());
		application.setCurrentUser(info);
		startActivity(new Intent(this, TopMediaActivity_.class));
	}
	
	@UiThread
	void focusPassword() {
		password.requestFocus();
	}
	
	@Background
	void asyncLogin(String user, String pwd) {
		try {
			UserInfo info = rpc.with(UserFacade.class).login(user, pwd);
			asyncShowLoginSuccess(info);
		} catch (Exception e) {
			focusPassword();
		}
	}
}
