package com.bravson.socialalert.android.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.googlecode.jsonrpc4j.JsonRpcService;
import com.googlecode.jsonrpc4j.ReflectionUtil;

class RpcInvocationHandler implements InvocationHandler {
	
	private final JsonRpcService serviceDescription;
	private final JsonRpcConnection serverConnection;
	
	public RpcInvocationHandler(JsonRpcConnection serverConnection, JsonRpcService serviceDescription) {
		this.serverConnection = serverConnection;
		this.serviceDescription = serviceDescription;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Object.class) {
			return proxyObjectMethods(method, proxy, args);
		}
		Object arguments = ReflectionUtil.parseArguments(method, args, serviceDescription.useNamedParams());
		return serverConnection.invoke(method.getName(), arguments, method.getGenericReturnType(), serviceDescription.value());
	}
	
	private static Object proxyObjectMethods(Method method, Object proxyObject, Object[] args) {
		String name = method.getName();
		if (name.equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}
		if (name.equals("hashCode")) {
			return System.identityHashCode(proxyObject);
		}
		if (name.equals("equals")) {
			return proxyObject == args[0];
		}
		throw new RuntimeException(method.getName() + " is not a member of java.lang.Object");
	}
}