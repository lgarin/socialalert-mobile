package com.bravson.socialalert.android.service;

import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.SupposeBackground;

import com.googlecode.jsonrpc4j.JsonRpcService;

@EBean(scope = Scope.Singleton)
public class JsonRpcServiceFactory {

	@Bean
	JsonRpcConnection serverConnection;
	
	private final HashMap<Class<?>, Object> serviceProxyMap = new HashMap<Class<?>, Object>();
	
	public JsonRpcServiceFactory() {
	}
	
	@SupposeBackground
	public <T> T get(Class<T> serviceInterface) {
		@SuppressWarnings("unchecked")
		T proxy = (T) serviceProxyMap.get(serviceInterface);
		if (proxy != null) {
			return proxy;
		}
		proxy = createProxy(serviceInterface);
		serviceProxyMap.put(serviceInterface, proxy);
		return proxy;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T createProxy(Class<T> serviceInterface) {
		JsonRpcService serviceDescription = serviceInterface.getAnnotation(JsonRpcService.class);
		return (T)Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] {serviceInterface}, new RpcInvocationHandler(serverConnection, serviceDescription));
	}
}
