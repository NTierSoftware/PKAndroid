package org.peacekeeper.rest;

import com.google.gson.Gson;

import org.peacekeeper.account.Contract;
import org.peacekeeper.util.Deployment;
import org.slf4j.*;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class WebServiceGet{ 
protected static Contract mContract;
private static final Logger mLog	= LoggerFactory.getLogger( WebServiceGet.class );

public WebServiceGet(Contract contract){ mContract = contract;  }//constructor

public String get(Contract.URLGet urlget) {
//		public String get(URL url) {
	URL url = mContract.contractURL(urlget);
	HttpsURLConnection urlConnection;
	InputStream in, ins;
	String result;
	try {
		//trustEveryone(true);
		urlConnection = (HttpsURLConnection) url.openConnection();
		ins = urlConnection.getInputStream();
		in = new BufferedInputStream(ins );
		result = inputStreamToString(in);
		urlConnection.disconnect();
		in.close();
		ins.close();
	}
	catch (IOException e) {
		e.printStackTrace();
		return ("FAILED:\t" + url );
	}// TODO Auto-generated catch block

	switch (urlget) {
	case Test  :
		Deployment deploy = new Gson().fromJson(result, Deployment.class);

		mLog.debug(deploy.toString());
		break;

	default: break;
	}
return result;
}//get


protected static String inputStreamToString(InputStream is) {
	StringBuilder total = new StringBuilder();

	try {
		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(
				new InputStreamReader(is));
		String line;
		// Read response until the end
		while ((line = rd.readLine()) != null) {
			total.append(line);
		}
		rd.close();
	} catch (IOException e) {
		mLog.error(e.getLocalizedMessage(), e);
	}

	return total.toString();
}//inputStreamToString
}//class WebServiceGet


/*
protected void trustEveryone(boolean trust) {//TODO FOR TESTING/DEV PURPOSE ONLY!!! HIGHLY INSECURE!!!
	// see: http://stackoverflow.com/questions/1217141/self-signed-ssl-acceptance-android/1607997#1607997
	if (!trust) return;

	try {
		javax.net.ssl.HttpsURLConnection
				.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
					@Override
					public boolean verify(String hostname,
					                      javax.net.ssl.SSLSession session) {
						return true;
					}
				});
		javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
		context.init(null,
				            new javax.net.ssl.X509TrustManager[] { new javax.net.ssl.X509TrustManager() {
					            @Override
					            public void checkClientTrusted(
							                                          java.security.cert.X509Certificate[] chain, String authType)
							            throws java.security.cert.CertificateException {
					            }

					            @Override
					            public void checkServerTrusted(
							                                          java.security.cert.X509Certificate[] chain, String authType)
							            throws java.security.cert.CertificateException {
					            }

					            @Override
					            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						            return new java.security.cert.X509Certificate[0];
					            }
				            } }, new java.security.SecureRandom());
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context
				                                                            .getSocketFactory());
	} catch (Exception e) { // should never happen
		e.printStackTrace();
	}
}// trustEveryone
*/
