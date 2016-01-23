package com.bravson.socialalert.android.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.res.IntegerRes;

@EBean
public abstract class ServerConnection {

	@IntegerRes
	int connectionTimeoutMillis;
	
	@IntegerRes
	int readTimeoutMillis;
	
	private Proxy connectionProxy = Proxy.NO_PROXY;
	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;
	
	protected HttpURLConnection createHttpPost(URL serviceUrl) throws IOException {

		// create URLConnection
		HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection(connectionProxy);
		con.setConnectTimeout(connectionTimeoutMillis);
		con.setReadTimeout(readTimeoutMillis);
		con.setAllowUserInteraction(false);
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
		
		// return it
		return con;
	}
}
