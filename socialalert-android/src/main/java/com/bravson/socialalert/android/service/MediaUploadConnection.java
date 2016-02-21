package com.bravson.socialalert.android.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.SupposeBackground;
import org.androidannotations.annotations.res.StringRes;

import android.support.annotation.UiThread;

@EBean(scope = Scope.Singleton)
public class MediaUploadConnection extends ServerConnection {
	
	@StringRes
	String baseUploadUrl;

    private static final int BUFFER_SIZE = 1024 * 1024;

	@SupposeBackground
	public String upload(File file, String contentType, ProgressListener progressListener) throws Exception {

		// create URLConnection
		HttpURLConnection conn = createHttpPost(new URL(baseUploadUrl));
		
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Content-Length", String.valueOf(file.length()));
		
        conn.connect();
		try {
			writeFile(conn, file, progressListener);
			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_CREATED) {
				return conn.getHeaderField("Location");
			}
			return null;
		} finally {
			conn.disconnect();
		}
	}
	
	private void writeFile(HttpURLConnection conn, File file, ProgressListener progressListener) throws IOException {
		OutputStream os = conn.getOutputStream();
		try {
			copy(file, os, progressListener);
		} finally {
			os.close();
		}
		
	}
	
	private static long copy(File file, OutputStream output, ProgressListener progressListener) throws IOException {
		int fileLength = (int) file.length();
		progressListener.onProgress(fileLength, 0);
		InputStream input = new FileInputStream(file);
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
	        long count = 0;
	        int n = 0;
	        while ((n = input.read(buffer)) > 0) {
	            output.write(buffer, 0, n);
	            count += n;
	            progressListener.onProgress(fileLength, (int) count);
	        }
	        return count;
		} finally {
			input.close();
		}
    }
}
