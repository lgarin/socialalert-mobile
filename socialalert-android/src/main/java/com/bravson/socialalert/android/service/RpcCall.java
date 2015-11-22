package com.bravson.socialalert.android.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SupposeBackground;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;

import android.app.Activity;
import android.widget.Toast;

import com.bravson.socialalert.android.R;
import com.googlecode.jsonrpc4j.JsonRpcClientException;

@EBean
public class RpcCall extends RpcProxyFactory {

	@RootContext
	Activity activity;
	
	@StringRes(R.string.unknownErrorMessage)
	String unknownErrorMessage;
	
	@StringRes(R.string.loadingDataMessage)
	String loadingDataMessage;
	
	@UiThread
	void asyncShowCallError(Exception exception) {
		Toast.makeText(activity, unknownErrorMessage, Toast.LENGTH_LONG).show();
	}
	
	@UiThread
	void asyncShowCallError(JsonRpcClientException exception) {
		// TODO handle specific error codes
		Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_LONG).show();
	}
	
	protected InvocationHandler createInvocationHandler(Class<?> serviceInterface) {
		final InvocationHandler baseHandler = super.createInvocationHandler(serviceInterface);
		return new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					return baseHandler.invoke(proxy, method, args);
				} catch (JsonRpcClientException e) {
					asyncShowCallError(e);
					throw e;
				} catch (Exception e) {
					asyncShowCallError(e);
					throw e;
				}
			}
		};
	}
	
	@SupposeBackground
	public <T> T with(Class<T> serviceInterface) {
		return getProxy(serviceInterface);
	}
}
