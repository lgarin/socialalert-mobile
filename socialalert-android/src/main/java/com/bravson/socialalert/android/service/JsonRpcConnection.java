package com.bravson.socialalert.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.SupposeBackground;
import org.androidannotations.annotations.res.IntegerRes;
import org.androidannotations.annotations.res.StringRes;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.googlecode.jsonrpc4j.JsonRpcClient;

@EBean(scope = Scope.Singleton)
public class JsonRpcConnection {
	@StringRes
	String baseServerUrl;
	
	@IntegerRes
	int connectionTimeoutMillis;
	
	@IntegerRes
	int readTimeoutMillis;
	
	private Proxy connectionProxy = Proxy.NO_PROXY;
	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;
	private String cookie;
	
	private JsonRpcClient client;
	
	@AfterInject
	void init() {
		client = new JsonRpcClient();
		client.getObjectMapper().registerModule(new JodaModule());
		client.setExceptionResolver(new JsonClientExceptionResolver());
	}

	private HttpURLConnection prepareConnection(URL serviceUrl) throws IOException {

		// create URLConnection
		HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection(connectionProxy);
		con.setConnectTimeout(connectionTimeoutMillis);
		con.setReadTimeout(readTimeoutMillis);
		con.setAllowUserInteraction(false);
		con.setDefaultUseCaches(false);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setInstanceFollowRedirects(true);
		con.setRequestMethod("POST");

		// do stuff for ssl
		if (HttpsURLConnection.class.isInstance(con)) {
			HttpsURLConnection https = HttpsURLConnection.class.cast(con);
			if (hostNameVerifier != null) {
				https.setHostnameVerifier(hostNameVerifier);
			}
			if (sslContext != null) {
				https.setSSLSocketFactory(sslContext.getSocketFactory());
			}
		}

		// init HTTP properites
		if (cookie != null) {
			con.setRequestProperty("cookie", cookie);
		}
		con.setRequestProperty("Content-Type", "application/json-rpc");

		// return it
		return con;
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

		// create URLConnection
		HttpURLConnection con = prepareConnection(new URL(baseServerUrl + "/" + servicePath));
		con.connect();

		// invoke it
		OutputStream ops = con.getOutputStream();
		try {
			client.invoke(methodName, argument, ops);
		} finally {
			ops.close();
		}

		// store session id
		cookie = extractCookie(con);

		// read and return value
		InputStream ips = con.getInputStream();
		try {
			return client.readResponse(returnType, ips);
		} finally {
			ips.close();
		}
	}
}
