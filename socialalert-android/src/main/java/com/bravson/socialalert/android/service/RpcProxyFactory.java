package com.bravson.socialalert.android.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import com.googlecode.jsonrpc4j.JsonRpcService;

@EBean
public class RpcProxyFactory {

	@Bean
	JsonRpcConnection serverConnection;
	
	private final HashMap<Class<?>, Object> serviceProxyMap = new HashMap<Class<?>, Object>();
	
	protected final <T> T getProxy(Class<T> serviceInterface) {
		@SuppressWarnings("unchecked")
		T proxy = (T) serviceProxyMap.get(serviceInterface);
		if (proxy != null) {
			return proxy;
		}
		proxy = createProxy(serviceInterface, createInvocationHandler(serviceInterface));
		serviceProxyMap.put(serviceInterface, proxy);
		return proxy;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T createProxy(Class<T> serviceInterface, InvocationHandler invoicationHandler) {
		return (T)Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] {serviceInterface}, invoicationHandler);
	}
	
	protected InvocationHandler createInvocationHandler(Class<?> serviceInterface) {
		return new RpcInvocationHandler(serverConnection, serviceInterface.getAnnotation(JsonRpcService.class));
	}
}
