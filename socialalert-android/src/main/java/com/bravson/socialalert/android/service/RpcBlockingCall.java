package com.bravson.socialalert.android.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;

import com.bravson.socialalert.android.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

@EBean
public class RpcBlockingCall extends RpcCall {

	@RootContext
	Activity activity;
	
	@StringRes(R.string.loadingDataMessage)
	String loadingDataMessage;
	
	private ProgressDialog progressDialog;

	@UiThread
	void asyncDismissProgressDialog() {
		dismissProgressDialog();
	}

	private void dismissProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}
	
	@UiThread
	void asyncShowProgressDialog() {
		progressDialog = ProgressDialog.show(context, activity.getTitle(), loadingDataMessage, true);
	}
	
	protected InvocationHandler createInvocationHandler(Class<?> serviceInterface) {
		final InvocationHandler baseHandler = super.createInvocationHandler(serviceInterface);
		return new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				asyncShowProgressDialog();
				try {
					return baseHandler.invoke(proxy, method, args);
				} finally {
					asyncDismissProgressDialog();
				}
			}
		};
	}
}
