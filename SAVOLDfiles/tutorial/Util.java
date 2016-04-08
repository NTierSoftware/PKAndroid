package org.peacekeeper.tutorial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.util.Log;

public class Util {
	public final static String LOG_TAG = "X509_Tutorial";
	
	static public void turnOfStrictMode() {
		android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.
				ThreadPolicy.Builder().permitAll().build();
				android.os.StrictMode.setThreadPolicy(policy);
	}
	
	static public boolean isNetworkAvailable(android.content.Context ctx) {
		android.net.ConnectivityManager cm = (android.net.ConnectivityManager) ctx
				.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
				
		android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		// if no network is available networkInfo will be null
		// otherwise check if we are connected
		if (networkInfo != null && networkInfo.isConnected()) 
			return true;
		else
			return false;
	}
	
	public static String printResponse(java.net.HttpURLConnection connection) throws java.io.IOException {
		String line;
		
		int status = connection.getResponseCode();

        switch (status) {
            case 200:
            case 201:
            	StringBuilder sb = new StringBuilder();
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                try {
                	/**
                	 * Read inputstream and convert to string
                	 */
	                while ((line = br.readLine()) != null) {
	                    sb.append(line+"\n");
	                }
                } finally {
                	br.close();  // Closes BufferedReader & Inputstream
                }
                String result = sb.toString();
                android.util.Log.d(LOG_TAG + ".util", result);
                return result;
            default:
       			return "Failure - Status: " + status;
        }
	}
}
