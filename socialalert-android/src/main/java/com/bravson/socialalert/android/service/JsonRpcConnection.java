package com.bravson.socialalert.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.SupposeBackground;
import org.androidannotations.annotations.res.StringRes;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.googlecode.jsonrpc4j.JsonRpcClient;

@EBean(scope = Scope.Singleton)
public class JsonRpcConnection extends ServerConnection {
	@StringRes
	String baseServerUrl;
	
	private String cookie;	
	private JsonRpcClient client;
	
	@AfterInject
	void init() {
		client = new JsonRpcClient();
		client.getObjectMapper().registerModule(new JodaModule());
		client.setExceptionResolver(new JsonClientExceptionResolver());
	}

	private static String extractCookie(HttpURLConnection con) {
		String cookie = con.getHeaderField("Set-Cookie");
		if (cookie != null && cookie.contains("JSESSIONID")) {
			int start = cookie.indexOf("JSESSIONID");
			int end = cookie.indexOf(';', start);
			if (end < 0) {
				end = cookie.length();
			}
			return cookie.substring(start, end);
		}
		return null;
	}

	@SupposeBackground
	public Object invoke(String methodName, Object argument, Type returnType, String servicePath) throws Throwable {

		HttpURLConnection con = createHttpPost(new URL(baseServerUrl + "/" + servicePath));
		initRequestProperties(con);
		
		con.connect();
		try {
			invokeMethod(methodName, argument, con);
			storeCookie(con);
			return readResponse(returnType, con);
		} finally {
			con.disconnect();
		}
	}

	void initRequestProperties(HttpURLConnection con) {
		if (cookie != null) {
			con.setRequestProperty("cookie", cookie);
		}
		con.setRequestProperty("Content-Type", "application/json-rpc");
	}

	Object readResponse(Type returnType, HttpURLConnection con) throws IOException, Throwable {
		InputStream ips = con.getInputStream();
		try {
			return client.readResponse(returnType, ips);
		} finally {
			ips.close();
		}
	}

	void storeCookie(HttpURLConnection con) {
		String newCookie = extractCookie(con);
		if (newCookie != null) {
			cookie = newCookie;
		}
	}

	void invokeMethod(String methodName, Object argument, HttpURLConnection con) throws IOException {
		OutputStream ops = con.getOutputStream();
		try {
			client.invoke(methodName, argument, ops);
		} finally {
			ops.close();
		}
	}
}
