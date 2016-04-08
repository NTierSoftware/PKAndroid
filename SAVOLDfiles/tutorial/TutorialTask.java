package org.peacekeeper.tutorial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Environment;
import android.webkit.WebView;
import android.widget.Toast;
import be.kuleuven.cs.msec.x509_tutorial_android.web.KeyChainManager;
import be.kuleuven.cs.msec.x509_tutorial_android.web.PRNGFixes;
import be.kuleuven.cs.msec.x509_tutorial_android.web.Util;


@SuppressWarnings("unused")
public class TutorialTask extends android.os.AsyncTask<String, Void, String>{

	static {
		/**
		 * BUGFIX: TrulyRandom when Calling "new SecureRandom()"
		 * http://android-developers.blogspot.be/2013/08/some-securerandom-thoughts.html
		 */
		PRNGFixes.apply();
	}

	private android.app.ProgressDialog progressDialog;
	private String error="";
	private android.webkit.WebView webview;
	private android.app.Activity activity;
	private String urlString;
	
	
	public TutorialTask(android.webkit.WebView webview, android.app.Activity activity) {
		this.webview = webview;
		this.activity = activity;
	}
	
	/*
	 * Before starting the Task Thread:
	 *  - clear the WebView (set to about:blank)
	 *  - show a progress dialog 
	 */
	@Override
	protected void onPreExecute() {
		// Reset the Webview to a Blank window.
		webview.clearView();
		// Show Progress Dialog
		progressDialog= android.app.ProgressDialog.show(activity, "Connect","Connecting to Server", true, true, new android.content.DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(android.content.DialogInterface dialog) {
				org.peacekeeper.tutorial.TutorialTask.this.cancel(true);
			}
		});
		super.onPreExecute();
	}
	/*
	 * Run the Networking on a separate Thread
	 */
	@Override
	protected String doInBackground(String... params) {
		error = null;
		urlString = params[0];
		try {
			/**
			 * BEGIN -- Fetch Web Page
			 */
			java.net.URL url = new java.net.URL(this.urlString);
			java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
			
			if (connection instanceof javax.net.ssl.HttpsURLConnection){
				/**
				 * Option 1:  Add Server Certificates manually to the Default Android Store
				 * 			  By default the Android CA Store is used.
				 */
				
				/**
				 * Option 2:  When using either Custom Server or Client Certificates:
				 * 		=> we need to create a Custom SSL SocketFactory
				 */	
				/**
				 *  Set Custom SSL Socket Factory to the current Connection (Locally):
				 *   	when only this connection should use custom SSL Settings,
				 *  	apply the custom SSL Settings directly on the connection:
				 */	
				((javax.net.ssl.HttpsURLConnection)connection).setSSLSocketFactory(getCustomSSLSocketFactory());
				/**
				 * Set Custom SSL Socket Factory as --Default-- (Globally)
				 * 	Note: This applies to 'all' connections made by the App 
				 * 		  This is required only once, e.g., at startup of the application
				 */
				//HttpsURLConnection.setDefaultSSLSocketFactory(getCustomSSLSocketFactory());
			}
			
			connection.connect();
			try {
				return Util.printResponse(connection);
			} finally {
				connection.disconnect();
			}
			/**
			 * END -- Fetch Web Page
			 */
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			return null;
		}
	}
	
	/*
	 * Back on the Main Thread:
	 *  hide the dialog box
	 */
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		// hide progressDialog
		progressDialog.dismiss();
		if (error!=null) 
			// Show Error
			android.widget.Toast.makeText(activity, error, android.widget.Toast.LENGTH_LONG).show();
		else  
			// Set Response to Webview
			webview.loadDataWithBaseURL(urlString, result, "text/html", "UTF-8", null);
	}

	
	private javax.net.ssl.SSLSocketFactory getCustomSSLSocketFactory() throws java.security.KeyStoreException, java.security.KeyManagementException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException, java.io.IOException, java.security.UnrecoverableKeyException{
		javax.net.ssl.KeyManager[] keyManagers = null; 	 // --Defaults to No Client Authentication Certificates Provided
		javax.net.ssl.TrustManager[] trustManagers = null;   // --Defaults to the built-in AndroidCAStore
		/**  ---------------- Custom Server Certificates  ---------------- **/
		/**
		 * Since we are using a Custom PKI we need to add the Certificates as being trusted by the application:
		 * 		using  A) a BKS file 
		 * 		   or  B) a PEM file
		 */
		
		/**
		 *  A) Read Trusted Certificates from BKS file 
		 *
		 */
		
		//KeyStore trustStore = KeyStore.getInstance("BKS");
		//InputStream trustStoreStream = activity.openFileInput("SSL_Server_chain.bks");
		//trustStore.load(trustStoreStream, null);
		
		
		/**
		 * B) Read Trusted Certificates from PEM file
		 * Note: the PEM files should not contain any text (use: openssl ca -notext ...) 
		 */
		/*
		trustStore = KeyStore.getInstance("BKS");
		trustStore.load(null,null);
		final BufferedInputStream bis = new BufferedInputStream(
						activity.openFileInput("SSL_Server_chain.crt"));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		while (bis.available()>0){
			Certificate cert = cf.generateCertificate(bis);
			SslCertificate sslCert = new SslCertificate((X509Certificate)cert);
			trustStore.setCertificateEntry(sslCert.getIssuedTo().getCName(), cert);
		}
		*/
		/**
		 * Add TrustStore To the TrustManager:
		 */
		
		//final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		//tmf.init(trustStore);
		//trustManagers = tmf.getTrustManagers();
		
		/**  ---------------- Client Certificates  ---------------- **/ 	
		/**
		 * Load Client Certificate
		 */
		/**
		 * When using the built-in KeyChain:
		 */
		
		
		//keyManagers = new KeyManager[] {new KeyChainManager(activity)};
		
		/**
		 * When using a custom KeyStore from a file:
		 */
		
		//KeyStore clientStore = KeyStore.getInstance("PKCS12");
		//final InputStream keyStoreLocation = activity.openFileInput("SSL_Client_B.p12"); 
		//clientStore.load(keyStoreLocation, "user".toCharArray());
		
		//final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		//kmf.init(clientStore, "user".toCharArray());
		
		//keyManagers = kmf.getKeyManagers();
		
		
		/**  ---------------- Insert into SSLContext ---------------- **/
		
		final javax.net.ssl.SSLContext sslCtx = javax.net.ssl.SSLContext.getInstance("TLS");
		sslCtx.init(keyManagers, trustManagers, new java.security.SecureRandom());
		return sslCtx.getSocketFactory();
	}
}


