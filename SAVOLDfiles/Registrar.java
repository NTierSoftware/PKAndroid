package org.peacekeeper.crypto;

import android.os.AsyncTask;

import org.json.*;
import org.peacekeeper.account.Contract;
import org.peacekeeper.rest.WebServicePost;
import org.peacekeeper.util.AsyncResponse;
import org.slf4j.*;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;


//public class Registrar extends AsyncTask<URL, Void, String>{
public class Registrar extends AsyncTask<Void, Void, String>{
private AsyncResponse delegate=null;
//private static Contract mContract = new Contract();
static private final Logger mLog = LoggerFactory.getLogger( org.peacekeeper.crypto.Registrar.class );

//Assigning call back interface through constructor
public Registrar(AsyncResponse asyncResponse) {
	SecurityGuard.initSecurity();
	delegate = asyncResponse;
	getRegistration();
}//constructor


//@Override protected String doInBackground(URL...urls ){
@Override protected String doInBackground(Void... params) {
	mLog.debug("doInBackground()");

	WebServicePost post = new WebServicePost(new Contract());
    //String result = post.post(Contract.URLPost.registrations, getRegistration());  //downloadUrl(urls[0]);

//return result;
return "";
}//doInBackground


private static JSONArray getRegistration() {
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug(SecurityGuard.toPEM(CSR));

	JSONArray registration = new JSONArray();
	JSONObject rowObject = new JSONObject();
	try {
		String CSRstr = Base64.toBase64String( CSR.getEncoded() );
		rowObject.put("CSR", CSRstr); }
	catch (java.io.IOException| JSONException X) { X.printStackTrace(); }

	registration.put(rowObject);
	mLog.debug(registration.toString());
return registration;
}//getRegistration


@Override protected void onPostExecute(String result){
	mLog.debug("onPostExecute");
	delegate.processFinish(result);
}//onPostExecute
}//class Registrar



/** Network connection timeout, in milliseconds. */
// private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000,  // 15 seconds

/** Network read timeout, in milliseconds. */
//NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

/** Given a string representation of a URL, sets up a connection and gets an input stream. */
/*
    private static String downloadUrl( java.net.URL url) throws java.io.IOException {
 		java.io.InputStream in = null, ins = null;
 		String result;

         javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
         conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS */
/* milliseconds *//*
);
         conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS */
/* milliseconds *//*
);
         conn.setRequestMethod("GET");
         conn.setDoInput(true);
         // Starts the query
         conn.connect();

 		//trustEveryone(true);
 		ins = conn.getInputStream();
 		in = new java.io.BufferedInputStream(ins );
 		result = inputStreamToString(in);
 		conn.disconnect();
 		in.close();
 		ins.close();
 		return result;
     }//downloadUrl

	protected static String inputStreamToString(java.io.InputStream is) {
		StringBuilder total = new StringBuilder();

		try {
			// Wrap a BufferedReader around the InputStream
			java.io.BufferedReader rd = new java.io.BufferedReader(
					new java.io.InputStreamReader(is));
			String line;
			// Read response until the end
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
			rd.close();
		}
		catch (java.io.IOException e) { Log.e("inputStreamToString", e.getLocalizedMessage()); }

		return total.toString();
	}//inputStreamToString
*/
